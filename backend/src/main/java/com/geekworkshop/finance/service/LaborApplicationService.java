package com.geekworkshop.finance.service;

import com.geekworkshop.finance.dto.*;
import com.geekworkshop.finance.entity.*;
import com.geekworkshop.finance.exception.BusinessException;
import com.geekworkshop.finance.exception.ForbiddenException;
import com.geekworkshop.finance.repository.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.math.*;
import java.nio.file.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class LaborApplicationService {
    private final LaborApplicationRepository applicationRepository;
    private final LaborAttachmentRepository attachmentRepository;
    private final LaborApprovalRecordRepository approvalRepository;
    private final OperationLogService operationLogService;

    @Value("${app.upload-dir:uploads}")
    private String uploadDir;

    public LaborApplicationService(
            LaborApplicationRepository applicationRepository,
            LaborAttachmentRepository attachmentRepository,
            LaborApprovalRecordRepository approvalRepository,
            OperationLogService operationLogService
    ) {
        this.applicationRepository = applicationRepository;
        this.attachmentRepository = attachmentRepository;
        this.approvalRepository = approvalRepository;
        this.operationLogService = operationLogService;
    }

    @Transactional(readOnly = true)
    public List<LaborApplicationResponse> list(AppUser user, String keyword, LaborStatus status) {
        requireModuleAccess(user);
        String normalized = StringUtils.hasText(keyword) ? keyword.trim().toLowerCase() : null;
        return applicationRepository.findAllDetails().stream()
                .filter(value -> canView(user, value))
                .filter(value -> status == null || value.getStatus() == status)
                .filter(value -> normalized == null
                        || value.getApplicationNumber().toLowerCase().contains(normalized)
                        || value.getTitle().toLowerCase().contains(normalized)
                        || value.getApplicant().getRealName().toLowerCase().contains(normalized))
                .map(value -> response(value, true, false))
                .toList();
    }

    @Transactional(readOnly = true)
    public LaborApplicationResponse detail(AppUser user, Long id) {
        requireModuleAccess(user);
        LaborApplication value = requireApplication(id);
        requireView(user, value);
        return response(value, false, true);
    }

    @Transactional
    public LaborApplicationResponse create(AppUser user, LaborApplicationRequest request) {
        requireModuleAccess(user);
        requireApplicantRole(user);
        LaborApplication value = new LaborApplication();
        value.setApplicationNumber(nextNumber());
        value.setApplicant(user);
        value.setDepartment(user.getDepartment());
        applyRequest(value, request);
        LaborApplication saved = applicationRepository.save(value);
        log(user, "新增劳务单", saved, "创建劳务/酬金发放申请，总额：" + saved.getTotalAmount());
        return response(saved, false, true);
    }

    @Transactional
    public LaborApplicationResponse update(AppUser user, Long id, LaborApplicationRequest request) {
        requireModuleAccess(user);
        LaborApplication value = requireApplication(id);
        requireOwnerOrAdmin(user, value);
        requireDraft(value);
        applyRequest(value, request);
        LaborApplication saved = applicationRepository.save(value);
        log(user, "编辑劳务单", saved, "修改劳务申请及领款人信息");
        return response(saved, false, true);
    }

    @Transactional
    public void delete(AppUser user, Long id) {
        requireModuleAccess(user);
        LaborApplication value = requireApplication(id);
        requireOwnerOrAdmin(user, value);
        requireDraft(value);
        log(user, "删除劳务单", value, "删除草稿劳务申请");
        approvalRepository.deleteByLaborApplicationId(id);
        attachmentRepository.deleteByLaborApplicationId(id);
        applicationRepository.delete(value);
    }

    @Transactional
    public LaborApplicationResponse submit(AppUser user, Long id) {
        requireModuleAccess(user);
        LaborApplication value = requireApplication(id);
        requireOwnerOrAdmin(user, value);
        requireDraft(value);
        validateRecipients(value);
        value.setStatus(LaborStatus.SUBMITTED);
        value.setSubmittedAt(LocalDateTime.now());
        saveRecord(value, user, "SUBMIT", ApprovalAction.APPROVE, "提交劳务申请");
        LaborApplication saved = applicationRepository.save(value);
        log(user, "提交劳务单", saved, "提交财务初审");
        return response(saved, false, true);
    }

    @Transactional
    public LaborApplicationResponse approve(AppUser user, Long id, ApprovalRequest request) {
        requireModuleAccess(user);
        LaborApplication value = requireApplication(id);
        requireView(user, value);
        if (request.getAction() == ApprovalAction.REJECT && !StringUtils.hasText(request.getComment())) {
            throw new BusinessException("驳回时必须填写原因");
        }
        String node;
        LaborStatus next;
        if (value.getStatus() == LaborStatus.SUBMITTED && isRole(user, UserRole.FINANCE)) {
            node = "FINANCE_INITIAL";
            next = LaborStatus.FINANCE_INITIAL_APPROVED;
        } else if (value.getStatus() == LaborStatus.FINANCE_INITIAL_APPROVED
                && isRole(user, UserRole.DEPARTMENT_MANAGER)) {
            if (user.getRole() != UserRole.ADMIN && !sameDepartment(user, value)) {
                throw new ForbiddenException("只能审批本部门劳务申请");
            }
            node = "DEPARTMENT";
            next = LaborStatus.DEPARTMENT_APPROVED;
        } else if (value.getStatus() == LaborStatus.DEPARTMENT_APPROVED
                && isRole(user, UserRole.EXECUTIVE)) {
            node = "EXECUTIVE";
            next = LaborStatus.EXECUTIVE_APPROVED;
        } else if (value.getStatus() == LaborStatus.PAID && isRole(user, UserRole.FINANCE)) {
            node = "FINANCE_RECHECK";
            next = LaborStatus.COMPLETED;
        } else {
            throw new ForbiddenException("当前角色不能处理该审批节点");
        }
        if (request.getAction() == ApprovalAction.REJECT) {
            value.setStatus(LaborStatus.DRAFT);
            value.setSubmittedAt(null);
        } else {
            value.setStatus(next);
        }
        saveRecord(value, user, node, request.getAction(), request.getComment());
        LaborApplication saved = applicationRepository.save(value);
        log(user, request.getAction() == ApprovalAction.APPROVE ? "劳务审批通过" : "劳务审批驳回",
                saved, node + "：" + Objects.toString(request.getComment(), "无意见"));
        return response(saved, false, true);
    }

    @Transactional
    public LaborApplicationResponse confirmPayment(AppUser user, Long id, PaymentRequest request) {
        requireModuleAccess(user);
        LaborApplication value = requireApplication(id);
        if (!isRole(user, UserRole.CASHIER)) throw new ForbiddenException("只有出纳可以确认付款");
        if (value.getStatus() != LaborStatus.EXECUTIVE_APPROVED) {
            throw new BusinessException("执行院长审批后才能付款");
        }
        if (!attachmentRepository.existsByLaborApplicationIdAndAttachmentType(id, LaborAttachmentType.BANK_RECEIPT)) {
            throw new BusinessException("确认付款前必须上传银行回执");
        }
        if (request.getPaymentAmount().compareTo(value.getTotalAmount()) != 0) {
            throw new BusinessException("付款金额必须与劳务单总额一致");
        }
        value.setPaymentDate(request.getPaymentDate());
        value.setPaymentAmount(request.getPaymentAmount());
        value.setPaymentVoucherNumber(request.getVoucherNumber().trim());
        value.setStatus(LaborStatus.PAID);
        saveRecord(value, user, "CASHIER_PAYMENT", ApprovalAction.APPROVE, request.getComment());
        LaborApplication saved = applicationRepository.save(value);
        log(user, "劳务付款", saved, "付款金额：" + request.getPaymentAmount() + "，凭证号：" + request.getVoucherNumber());
        return response(saved, false, true);
    }

    @Transactional
    public LaborAttachmentResponse upload(
            AppUser user, Long id, LaborAttachmentType type, MultipartFile file
    ) {
        requireModuleAccess(user);
        LaborApplication value = requireApplication(id);
        boolean cashierReceipt = isRole(user, UserRole.CASHIER)
                && type == LaborAttachmentType.BANK_RECEIPT
                && value.getStatus() == LaborStatus.EXECUTIVE_APPROVED;
        if (!cashierReceipt) {
            requireOwnerOrAdmin(user, value);
            requireDraft(value);
        }
        String safe = SecureFileSupport.validate(file);
        String stored = UUID.randomUUID() + "-" + safe.replaceAll("[^a-zA-Z0-9._\\-\\u4e00-\\u9fa5]", "_");
        try {
            Path directory = Paths.get(uploadDir, "labor").toAbsolutePath().normalize();
            Files.createDirectories(directory);
            Files.copy(file.getInputStream(), directory.resolve(stored), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            throw new BusinessException("附件保存失败");
        }
        LaborAttachment attachment = new LaborAttachment();
        attachment.setLaborApplication(value);
        attachment.setAttachmentType(type == null ? LaborAttachmentType.OTHER : type);
        attachment.setFileName(safe);
        attachment.setFileUrl("/uploads/labor/" + stored);
        attachment.setFileType(file.getContentType());
        attachment.setFileSize(file.getSize());
        LaborAttachment saved = attachmentRepository.save(attachment);
        log(user, "上传劳务附件", value, saved.getAttachmentType() + "：" + safe);
        return LaborAttachmentResponse.fromEntity(saved);
    }

    @Transactional
    public byte[] exportExcel(AppUser user) {
        requireModuleAccess(user);
        if (!isRole(user, UserRole.FINANCE)) throw new ForbiddenException("只有财务和管理员可以导出");
        List<LaborApplication> values = applicationRepository.findAllDetails();
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("劳务酬金发放");
            String[] headers = {"编号", "类别", "标题", "申请人", "部门", "领款人", "身份证", "单位",
                    "劳务内容", "实发金额", "银行卡", "开户行", "状态", "创建时间"};
            Row header = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) header.createCell(i).setCellValue(headers[i]);
            int rowIndex = 1;
            for (LaborApplication value : values) {
                for (LaborRecipient recipient : value.getRecipients()) {
                    Row row = sheet.createRow(rowIndex++);
                    Object[] cells = {value.getApplicationNumber(), value.getCategory().name(), value.getTitle(),
                            value.getApplicant().getRealName(), value.getDepartment() == null ? "" : value.getDepartment().getName(),
                            recipient.getName(), maskId(recipient.getIdCard()), recipient.getOrganization(),
                            recipient.getServiceContent(), recipient.getNetAmount(), maskBank(recipient.getBankAccount()),
                            recipient.getBankName(), value.getStatus().name(), value.getCreatedAt()};
                    for (int i = 0; i < cells.length; i++) row.createCell(i).setCellValue(Objects.toString(cells[i], ""));
                }
            }
            for (int i = 0; i < headers.length; i++) sheet.setColumnWidth(i, i == 8 ? 8000 : 4200);
            workbook.write(output);
            operationLogService.record(user, "劳务管理", "导出劳务 Excel", null, "劳务酬金发放",
                    "导出 " + values.size() + " 张劳务单，敏感字段已脱敏");
            return output.toByteArray();
        } catch (IOException exception) {
            throw new BusinessException("导出 Excel 失败");
        }
    }

    private void applyRequest(LaborApplication value, LaborApplicationRequest request) {
        value.setCategory(request.getCategory());
        value.setTitle(request.getTitle().trim());
        value.setDescription(trim(request.getDescription()));
        value.setBudgetNumber(trim(request.getBudgetNumber()));
        value.getRecipients().clear();
        BigDecimal total = BigDecimal.ZERO;
        for (LaborRecipientRequest item : request.getRecipients()) {
            LaborRecipient recipient = new LaborRecipient();
            recipient.setLaborApplication(value);
            recipient.setName(item.getName().trim());
            recipient.setPhone(trim(item.getPhone()));
            recipient.setIdCard(item.getIdCard().replaceAll("\\s", ""));
            recipient.setOrganization(trim(item.getOrganization()));
            recipient.setPosition(trim(item.getPosition()));
            recipient.setServiceContent(item.getServiceContent().trim());
            recipient.setNetAmount(item.getNetAmount().setScale(2, RoundingMode.HALF_UP));
            recipient.setBankAccount(item.getBankAccount().replaceAll("\\s", ""));
            recipient.setBankName(item.getBankName().trim());
            value.getRecipients().add(recipient);
            total = total.add(recipient.getNetAmount());
        }
        value.setTotalAmount(total);
        value.setAmountInWords(toChineseUpper(total));
    }

    private LaborApplicationResponse response(LaborApplication value, boolean mask, boolean full) {
        List<LaborAttachmentResponse> attachments = full
                ? attachmentRepository.findByLaborApplicationIdOrderByCreatedAtAsc(value.getId())
                    .stream().map(LaborAttachmentResponse::fromEntity).toList()
                : List.of();
        return new LaborApplicationResponse(
                value.getId(), value.getApplicationNumber(), value.getCategory(), value.getTitle(),
                value.getDescription(), value.getBudgetNumber(), value.getTotalAmount(), value.getAmountInWords(),
                value.getStatus(), value.getApplicant().getId(), value.getApplicant().getRealName(),
                value.getDepartment() == null ? null : value.getDepartment().getId(),
                value.getDepartment() == null ? null : value.getDepartment().getName(),
                value.getSubmittedAt(), value.getPaymentDate(), value.getPaymentAmount(),
                value.getPaymentVoucherNumber(), value.getCreatedAt(),
                value.getRecipients().stream().map(item -> LaborRecipientResponse.fromEntity(item, mask)).toList(),
                attachments, full ? timeline(value) : List.of()
        );
    }

    private List<LaborTimelineResponse> timeline(LaborApplication value) {
        List<LaborApprovalRecord> records = approvalRepository.findDetailsByApplicationId(value.getId());
        return List.of(
                new LaborTimelineResponse("创建劳务单", "COMPLETED", value.getApplicant().getRealName(),
                        value.getApplicant().getRole(), null, value.getCreatedAt(), "CREATE"),
                node("提交申请", "SUBMIT", LaborStatus.DRAFT, value, records),
                node("财务初审", "FINANCE_INITIAL", LaborStatus.SUBMITTED, value, records),
                node("部门负责人审批", "DEPARTMENT", LaborStatus.FINANCE_INITIAL_APPROVED, value, records),
                node("执行院长审批", "EXECUTIVE", LaborStatus.DEPARTMENT_APPROVED, value, records),
                node("出纳付款", "CASHIER_PAYMENT", LaborStatus.EXECUTIVE_APPROVED, value, records),
                node("财务复核", "FINANCE_RECHECK", LaborStatus.PAID, value, records),
                new LaborTimelineResponse("流程完成",
                        value.getStatus() == LaborStatus.COMPLETED ? "COMPLETED" : "NOT_STARTED",
                        null, null, null, value.getStatus() == LaborStatus.COMPLETED ? value.getUpdatedAt() : null, "FINAL")
        );
    }

    private LaborTimelineResponse node(
            String title, String type, LaborStatus pendingStatus, LaborApplication value, List<LaborApprovalRecord> records
    ) {
        LaborApprovalRecord record = records.stream().filter(item -> item.getApprovalNode().equals(type))
                .reduce((first, second) -> second).orElse(null);
        if (record != null) {
            String status = record.getAction() == ApprovalAction.REJECT ? "REJECTED" : "COMPLETED";
            return new LaborTimelineResponse(title, status,
                    record.getApprover() == null ? null : record.getApprover().getRealName(),
                    record.getApprover() == null ? null : record.getApprover().getRole(),
                    record.getComment(), record.getCreatedAt(), type);
        }
        return new LaborTimelineResponse(title,
                value.getStatus() == pendingStatus ? "IN_PROGRESS" : "NOT_STARTED",
                null, null, null, null, type);
    }

    private void saveRecord(
            LaborApplication value, AppUser user, String node, ApprovalAction action, String comment
    ) {
        LaborApprovalRecord record = new LaborApprovalRecord();
        record.setLaborApplication(value);
        record.setApprover(user);
        record.setApprovalNode(node);
        record.setAction(action);
        record.setComment(comment);
        approvalRepository.save(record);
    }

    private synchronized String nextNumber() {
        String prefix = "LW" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        int sequence = applicationRepository.findTopByApplicationNumberStartingWithOrderByApplicationNumberDesc(prefix)
                .map(LaborApplication::getApplicationNumber)
                .map(number -> Integer.parseInt(number.substring(prefix.length())) + 1)
                .orElse(1);
        if (sequence > 999) throw new BusinessException("当日劳务单编号已用完");
        return prefix + String.format("%03d", sequence);
    }

    private String toChineseUpper(BigDecimal amount) {
        if (amount == null || amount.signum() < 0 || amount.compareTo(new BigDecimal("999999999999.99")) > 0) {
            throw new BusinessException("金额超出大写转换范围");
        }
        String[] digit = {"零", "壹", "贰", "叁", "肆", "伍", "陆", "柒", "捌", "玖"};
        String[] unit = {"", "拾", "佰", "仟"};
        String[] group = {"", "万", "亿"};
        long cents = amount.setScale(2, RoundingMode.HALF_UP).movePointRight(2).longValueExact();
        long integer = cents / 100;
        int jiao = (int) (cents / 10 % 10);
        int fen = (int) (cents % 10);
        if (integer == 0) return jiao == 0 && fen == 0 ? "人民币零元整"
                : "人民币" + (jiao > 0 ? digit[jiao] + "角" : "零") + (fen > 0 ? digit[fen] + "分" : "");
        StringBuilder result = new StringBuilder();
        int groupIndex = 0;
        boolean zeroBetween = false;
        while (integer > 0) {
            int part = (int) (integer % 10000);
            if (part == 0) {
                zeroBetween = result.length() > 0;
            } else {
                StringBuilder partText = new StringBuilder();
                boolean zero = false;
                for (int i = 0; i < 4; i++) {
                    int value = part % 10;
                    if (value == 0) {
                        zero = partText.length() > 0;
                    } else {
                        if (zero) partText.insert(0, digit[0]);
                        partText.insert(0, digit[value] + unit[i]);
                        zero = false;
                    }
                    part /= 10;
                }
                if (zeroBetween) result.insert(0, digit[0]);
                result.insert(0, partText + group[groupIndex]);
                zeroBetween = false;
            }
            integer /= 10000;
            groupIndex++;
        }
        result.insert(0, "人民币").append("元");
        if (jiao == 0 && fen == 0) return result.append("整").toString();
        if (jiao > 0) result.append(digit[jiao]).append("角");
        else if (fen > 0) result.append("零");
        if (fen > 0) result.append(digit[fen]).append("分");
        return result.toString();
    }

    private LaborApplication requireApplication(Long id) {
        return applicationRepository.findDetailById(id)
                .orElseThrow(() -> new BusinessException("劳务单不存在"));
    }
    private void requireDraft(LaborApplication value) {
        if (value.getStatus() != LaborStatus.DRAFT) throw new BusinessException("只有草稿可以修改");
    }
    private void validateRecipients(LaborApplication value) {
        if (value.getRecipients().isEmpty() || value.getTotalAmount().signum() <= 0) {
            throw new BusinessException("至少需要一名有效领款人");
        }
    }
    private void requireApplicantRole(AppUser user) {
        if (user.getRole() == UserRole.CASHIER) throw new ForbiddenException("出纳不能创建劳务单");
    }
    private void requireModuleAccess(AppUser user) {
        if (user.getRole() == UserRole.OFFICE) {
            throw new ForbiddenException("办公室角色不能访问劳务酬金模块");
        }
    }
    private void requireOwnerOrAdmin(AppUser user, LaborApplication value) {
        if (user.getRole() != UserRole.ADMIN && !value.getApplicant().getId().equals(user.getId())) {
            throw new ForbiddenException("只能操作自己的劳务单");
        }
    }
    private void requireView(AppUser user, LaborApplication value) {
        if (!canView(user, value)) throw new ForbiddenException("无权查看该劳务单");
    }
    private boolean canView(AppUser user, LaborApplication value) {
        if (EnumSet.of(UserRole.ADMIN, UserRole.FINANCE, UserRole.EXECUTIVE).contains(user.getRole())) return true;
        if (user.getRole() == UserRole.DEPARTMENT_MANAGER) return sameDepartment(user, value);
        if (user.getRole() == UserRole.CASHIER) return EnumSet.of(
                LaborStatus.EXECUTIVE_APPROVED, LaborStatus.PAID, LaborStatus.COMPLETED).contains(value.getStatus());
        return value.getApplicant().getId().equals(user.getId());
    }
    private boolean sameDepartment(AppUser user, LaborApplication value) {
        return user.getDepartment() != null && value.getDepartment() != null
                && user.getDepartment().getId().equals(value.getDepartment().getId());
    }
    private boolean isRole(AppUser user, UserRole role) {
        return user.getRole() == role || user.getRole() == UserRole.ADMIN;
    }
    private String trim(String value) { return StringUtils.hasText(value) ? value.trim() : null; }
    private String maskId(String value) { return LaborRecipientResponse.fromEntity(recipientForMask(value, ""), true).idCard(); }
    private String maskBank(String value) { return LaborRecipientResponse.fromEntity(recipientForMask("", value), true).bankAccount(); }
    private LaborRecipient recipientForMask(String id, String bank) {
        LaborRecipient recipient = new LaborRecipient();
        recipient.setIdCard(id);
        recipient.setBankAccount(bank);
        return recipient;
    }
    private void log(AppUser user, String action, LaborApplication value, String detail) {
        operationLogService.record(user, "劳务管理", action, value.getId(), value.getApplicationNumber(), detail);
    }
}
