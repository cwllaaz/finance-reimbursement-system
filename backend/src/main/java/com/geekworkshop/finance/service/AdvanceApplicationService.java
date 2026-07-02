package com.geekworkshop.finance.service;

import com.geekworkshop.finance.dto.*;
import com.geekworkshop.finance.entity.*;
import com.geekworkshop.finance.exception.*;
import com.geekworkshop.finance.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.math.*;
import java.nio.file.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class AdvanceApplicationService {
    private final AdvanceApplicationRepository applicationRepository;
    private final AdvanceApprovalRecordRepository approvalRepository;
    private final AdvanceAttachmentRepository attachmentRepository;
    private final AdvanceOffsetRecordRepository offsetRepository;
    private final OperationLogService operationLogService;
    @Value("${app.upload-dir:uploads}") private String uploadDir;

    public AdvanceApplicationService(
            AdvanceApplicationRepository applicationRepository,
            AdvanceApprovalRecordRepository approvalRepository,
            AdvanceAttachmentRepository attachmentRepository,
            AdvanceOffsetRecordRepository offsetRepository,
            OperationLogService operationLogService
    ) {
        this.applicationRepository = applicationRepository;
        this.approvalRepository = approvalRepository;
        this.attachmentRepository = attachmentRepository;
        this.offsetRepository = offsetRepository;
        this.operationLogService = operationLogService;
    }

    @Transactional
    public List<AdvanceApplicationResponse> list(
            AppUser user,
            String keyword,
            AdvanceType type,
            AdvanceStatus status,
            SettlementStatus settlementStatus,
            List<SettlementStatus> settlementStatuses
    ) {
        requireModuleAccess(user);
        refreshOverdueStatuses();
        String normalized = StringUtils.hasText(keyword) ? keyword.trim().toLowerCase() : null;
        return applicationRepository.findAllDetails().stream()
                .filter(value -> canView(user, value))
                .filter(value -> type == null || value.getType() == type)
                .filter(value -> status == null || value.getStatus() == status)
                .filter(value -> settlementStatus == null || value.getSettlementStatus() == settlementStatus)
                .filter(value -> settlementStatuses == null || settlementStatuses.isEmpty()
                        || settlementStatuses.contains(value.getSettlementStatus()))
                .filter(value -> normalized == null
                        || value.getApplicationNumber().toLowerCase().contains(normalized)
                        || value.getReason().toLowerCase().contains(normalized)
                        || value.getPayeeName().toLowerCase().contains(normalized))
                .map(value -> response(value, false))
                .toList();
    }

    @Transactional
    public AdvanceApplicationResponse detail(AppUser user, Long id) {
        requireModuleAccess(user);
        AdvanceApplication value = requireApplication(id);
        refreshOverdue(value);
        requireView(user, value);
        return response(value, true);
    }

    @Transactional
    public AdvanceApplicationResponse create(AppUser user, AdvanceApplicationRequest request) {
        requireModuleAccess(user);
        if (user.getRole() == UserRole.CASHIER) throw new ForbiddenException("出纳不能创建资金申请");
        AdvanceApplication value = new AdvanceApplication();
        value.setApplicationNumber(nextNumber());
        value.setApplicant(user);
        value.setDepartment(user.getDepartment());
        apply(value, request);
        AdvanceApplication saved = applicationRepository.save(value);
        log(user, "新增资金申请", saved, "申请金额：" + saved.getAmount());
        return response(saved, true);
    }

    @Transactional
    public AdvanceApplicationResponse update(AppUser user, Long id, AdvanceApplicationRequest request) {
        requireModuleAccess(user);
        AdvanceApplication value = requireApplication(id);
        requireOwner(user, value);
        requireDraft(value);
        apply(value, request);
        AdvanceApplication saved = applicationRepository.save(value);
        log(user, "编辑资金申请", saved, "修改申请资料");
        return response(saved, true);
    }

    @Transactional
    public void delete(AppUser user, Long id) {
        requireModuleAccess(user);
        AdvanceApplication value = requireApplication(id);
        requireOwner(user, value);
        requireDraft(value);
        log(user, "删除资金申请", value, "删除草稿");
        approvalRepository.deleteByAdvanceApplicationId(id);
        attachmentRepository.deleteByAdvanceApplicationId(id);
        offsetRepository.deleteByAdvanceApplicationId(id);
        applicationRepository.delete(value);
    }

    @Transactional
    public AdvanceApplicationResponse submit(AppUser user, Long id) {
        requireModuleAccess(user);
        AdvanceApplication value = requireApplication(id);
        requireOwner(user, value);
        requireDraft(value);
        validateConditionalFields(value);
        value.setStatus(AdvanceStatus.SUBMITTED);
        value.setSubmittedAt(LocalDateTime.now());
        saveApproval(value, user, "SUBMIT", ApprovalAction.APPROVE, "提交申请");
        AdvanceApplication saved = applicationRepository.save(value);
        log(user, "提交资金申请", saved, "进入部门负责人审批");
        return response(saved, true);
    }

    @Transactional
    public AdvanceApplicationResponse approve(AppUser user, Long id, ApprovalRequest request) {
        requireModuleAccess(user);
        AdvanceApplication value = requireApplication(id);
        requireView(user, value);
        if (request.getAction() == ApprovalAction.REJECT && !StringUtils.hasText(request.getComment())) {
            throw new BusinessException("驳回时必须填写原因");
        }
        String node;
        AdvanceStatus next;
        if (value.getStatus() == AdvanceStatus.SUBMITTED && role(user, UserRole.DEPARTMENT_MANAGER)) {
            if (user.getRole() != UserRole.ADMIN && !sameDepartment(user, value)) {
                throw new ForbiddenException("只能审批本部门申请");
            }
            node = "DEPARTMENT";
            next = AdvanceStatus.DEPARTMENT_APPROVED;
        } else if (value.getStatus() == AdvanceStatus.DEPARTMENT_APPROVED && role(user, UserRole.FINANCE)) {
            node = "FINANCE";
            next = AdvanceStatus.FINANCE_APPROVED;
        } else if (value.getStatus() == AdvanceStatus.FINANCE_APPROVED && role(user, UserRole.EXECUTIVE)) {
            node = "EXECUTIVE";
            next = AdvanceStatus.EXECUTIVE_APPROVED;
        } else if (value.getStatus() == AdvanceStatus.PAID && role(user, UserRole.FINANCE)) {
            node = "FINANCE_RECHECK";
            next = AdvanceStatus.COMPLETED;
        } else {
            throw new ForbiddenException("当前角色不能处理该节点");
        }
        if (request.getAction() == ApprovalAction.REJECT) {
            value.setStatus(AdvanceStatus.DRAFT);
            value.setSubmittedAt(null);
        } else {
            value.setStatus(next);
        }
        saveApproval(value, user, node, request.getAction(), request.getComment());
        AdvanceApplication saved = applicationRepository.save(value);
        log(user, request.getAction() == ApprovalAction.APPROVE ? "资金审批通过" : "资金审批驳回",
                saved, node + "：" + Objects.toString(request.getComment(), "无意见"));
        return response(saved, true);
    }

    @Transactional
    public AdvanceApplicationResponse confirmPayment(AppUser user, Long id, PaymentRequest request) {
        requireModuleAccess(user);
        AdvanceApplication value = requireApplication(id);
        if (!role(user, UserRole.CASHIER)) throw new ForbiddenException("只有出纳可以付款");
        if (value.getStatus() != AdvanceStatus.EXECUTIVE_APPROVED) {
            throw new BusinessException("执行院长审批后才能付款");
        }
        if (!attachmentRepository.existsByAdvanceApplicationIdAndAttachmentType(id, AdvanceAttachmentType.BANK_RECEIPT)) {
            throw new BusinessException("付款前必须上传银行回执");
        }
        if (request.getPaymentAmount().compareTo(value.getAmount()) != 0) {
            throw new BusinessException("付款金额必须与申请金额一致");
        }
        value.setPaymentDate(request.getPaymentDate());
        value.setPaymentAmount(request.getPaymentAmount());
        value.setPaymentVoucherNumber(request.getVoucherNumber().trim());
        value.setStatus(AdvanceStatus.PAID);
        value.setSettlementStatus(isOverdue(value) ? SettlementStatus.OVERDUE : SettlementStatus.PENDING_OFFSET);
        saveApproval(value, user, "CASHIER_PAYMENT", ApprovalAction.APPROVE, request.getComment());
        AdvanceApplication saved = applicationRepository.save(value);
        log(user, "资金付款", saved, "金额：" + request.getPaymentAmount() + "，凭证号：" + request.getVoucherNumber());
        return response(saved, true);
    }

    @Transactional
    public AdvanceApplicationResponse offset(AppUser user, Long id, AdvanceOffsetRequest request) {
        requireModuleAccess(user);
        if (!role(user, UserRole.FINANCE)) throw new ForbiddenException("只有财务可以登记冲账");
        AdvanceApplication value = requireApplication(id);
        if (!EnumSet.of(AdvanceStatus.PAID, AdvanceStatus.COMPLETED).contains(value.getStatus())) {
            throw new BusinessException("付款后才能登记还款或冲账");
        }
        BigDecimal next = value.getOffsetAmount().add(request.getAmount()).setScale(2, RoundingMode.HALF_UP);
        if (next.compareTo(value.getAmount()) > 0) throw new BusinessException("累计冲账金额不能超过申请金额");
        AdvanceOffsetRecord record = new AdvanceOffsetRecord();
        record.setAdvanceApplication(value);
        record.setOperator(user);
        record.setAmount(request.getAmount());
        record.setComment(request.getComment());
        offsetRepository.save(record);
        value.setOffsetAmount(next);
        value.setSettlementStatus(next.compareTo(value.getAmount()) == 0
                ? SettlementStatus.OFFSET_COMPLETED : SettlementStatus.PARTIAL_OFFSET);
        AdvanceApplication saved = applicationRepository.save(value);
        log(user, "登记冲账", saved, "本次：" + request.getAmount() + "，累计：" + next);
        return response(saved, true);
    }

    @Transactional
    public AdvanceAttachmentResponse upload(
            AppUser user, Long id, AdvanceAttachmentType type, MultipartFile file
    ) {
        requireModuleAccess(user);
        AdvanceApplication value = requireApplication(id);
        boolean cashierReceipt = role(user, UserRole.CASHIER) && type == AdvanceAttachmentType.BANK_RECEIPT
                && value.getStatus() == AdvanceStatus.EXECUTIVE_APPROVED;
        boolean financeOffset = role(user, UserRole.FINANCE) && type == AdvanceAttachmentType.OFFSET_VOUCHER
                && EnumSet.of(AdvanceStatus.PAID, AdvanceStatus.COMPLETED).contains(value.getStatus());
        if (!cashierReceipt && !financeOffset) {
            requireOwner(user, value);
            requireDraft(value);
        }
        String safe = SecureFileSupport.validate(file);
        String stored = UUID.randomUUID() + "-" + safe.replaceAll("[^a-zA-Z0-9._\\-\\u4e00-\\u9fa5]", "_");
        try {
            Path directory = Paths.get(uploadDir, "advances").toAbsolutePath().normalize();
            Files.createDirectories(directory);
            Files.copy(file.getInputStream(), directory.resolve(stored), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            throw new BusinessException("附件保存失败");
        }
        AdvanceAttachment attachment = new AdvanceAttachment();
        attachment.setAdvanceApplication(value);
        attachment.setAttachmentType(type == null ? AdvanceAttachmentType.OTHER : type);
        attachment.setFileName(safe);
        attachment.setFileUrl("/uploads/advances/" + stored);
        attachment.setFileType(file.getContentType());
        attachment.setFileSize(file.getSize());
        AdvanceAttachment saved = attachmentRepository.save(attachment);
        log(user, "上传资金附件", value, saved.getAttachmentType() + "：" + safe);
        return AdvanceAttachmentResponse.from(saved);
    }

    @Transactional
    public ReminderStats reminderStats(AppUser user) {
        requireDashboardAccess(user);
        refreshOverdueStatuses();
        List<AdvanceApplication> values = applicationRepository.findAllDetails().stream()
                .filter(value -> user.getRole() != UserRole.DEPARTMENT_MANAGER || sameDepartment(user, value))
                .toList();
        long pending = values.stream().filter(value -> value.getSettlementStatus() == SettlementStatus.PENDING_OFFSET
                || value.getSettlementStatus() == SettlementStatus.PARTIAL_OFFSET).count();
        long overdue = values.stream().filter(value -> value.getSettlementStatus() == SettlementStatus.OVERDUE).count();
        return new ReminderStats(pending, overdue);
    }

    public record ReminderStats(long pendingOffsetCount, long overdueAdvanceCount) {}

    private void apply(AdvanceApplication value, AdvanceApplicationRequest request) {
        value.setType(request.getType());
        value.setReason(request.getReason().trim());
        value.setAmount(request.getAmount().setScale(2, RoundingMode.HALF_UP));
        value.setPaymentMethod(request.getPaymentMethod().trim());
        value.setPayeeName(request.getPayeeName().trim());
        value.setBankAccount(request.getBankAccount().replaceAll("\\s", ""));
        value.setBankName(request.getBankName().trim());
        value.setExpectedRepaymentDate(request.getExpectedRepaymentDate());
        value.setPartnerName(trim(request.getPartnerName()));
        value.setExpectedSettlementDate(request.getExpectedSettlementDate());
        validateConditionalFields(value);
    }

    private void validateConditionalFields(AdvanceApplication value) {
        if (value.getType() == AdvanceType.TEMPORARY_LOAN && value.getExpectedRepaymentDate() == null) {
            throw new BusinessException("暂借款必须填写预计还款/冲账日期");
        }
        if (value.getType() == AdvanceType.PREPAYMENT
                && (!StringUtils.hasText(value.getPartnerName()) || value.getExpectedSettlementDate() == null)) {
            throw new BusinessException("预付款必须填写合作方和预计结算日期");
        }
    }

    private AdvanceApplicationResponse response(AdvanceApplication value, boolean full) {
        BigDecimal remaining = value.getAmount().subtract(value.getOffsetAmount()).max(BigDecimal.ZERO);
        List<AdvanceAttachmentResponse> attachments = full
                ? attachmentRepository.findByAdvanceApplicationIdOrderByCreatedAtAsc(value.getId())
                    .stream().map(AdvanceAttachmentResponse::from).toList() : List.of();
        List<AdvanceOffsetResponse> offsets = full
                ? offsetRepository.findDetailsByApplicationId(value.getId()).stream()
                    .map(item -> new AdvanceOffsetResponse(item.getId(), item.getAmount(), item.getComment(),
                            item.getOperator().getRealName(), item.getCreatedAt())).toList() : List.of();
        return new AdvanceApplicationResponse(
                value.getId(), value.getApplicationNumber(), value.getType(), value.getReason(), value.getAmount(),
                value.getPaymentMethod(), value.getPayeeName(), full ? value.getBankAccount() : maskBank(value.getBankAccount()), value.getBankName(),
                value.getExpectedRepaymentDate(), value.getPartnerName(), value.getExpectedSettlementDate(),
                value.getStatus(), value.getSettlementStatus(), value.getOffsetAmount(), remaining,
                value.getApplicant().getId(), value.getApplicant().getRealName(),
                value.getDepartment() == null ? null : value.getDepartment().getName(),
                value.getSubmittedAt(), value.getPaymentDate(), value.getPaymentAmount(),
                value.getPaymentVoucherNumber(), value.getCreatedAt(), attachments, offsets,
                full ? timeline(value) : List.of()
        );
    }

    private List<AdvanceTimelineResponse> timeline(AdvanceApplication value) {
        List<AdvanceApprovalRecord> records = approvalRepository.findDetailsByApplicationId(value.getId());
        return List.of(
                new AdvanceTimelineResponse("创建申请", "COMPLETED", value.getApplicant().getRealName(),
                        value.getApplicant().getRole(), null, value.getCreatedAt(), "CREATE"),
                node("提交申请", "SUBMIT", AdvanceStatus.DRAFT, value, records),
                node("部门负责人审批", "DEPARTMENT", AdvanceStatus.SUBMITTED, value, records),
                node("财务审核", "FINANCE", AdvanceStatus.DEPARTMENT_APPROVED, value, records),
                node("执行院长审批", "EXECUTIVE", AdvanceStatus.FINANCE_APPROVED, value, records),
                node("出纳付款", "CASHIER_PAYMENT", AdvanceStatus.EXECUTIVE_APPROVED, value, records),
                node("财务复核", "FINANCE_RECHECK", AdvanceStatus.PAID, value, records),
                new AdvanceTimelineResponse("审批完成",
                        value.getStatus() == AdvanceStatus.COMPLETED ? "COMPLETED" : "NOT_STARTED",
                        null, null, null, value.getStatus() == AdvanceStatus.COMPLETED ? value.getUpdatedAt() : null, "FINAL")
        );
    }

    private AdvanceTimelineResponse node(
            String title, String type, AdvanceStatus pending, AdvanceApplication value, List<AdvanceApprovalRecord> records
    ) {
        AdvanceApprovalRecord record = records.stream().filter(item -> item.getApprovalNode().equals(type))
                .reduce((first, second) -> second).orElse(null);
        if (record != null) return new AdvanceTimelineResponse(title,
                record.getAction() == ApprovalAction.REJECT ? "REJECTED" : "COMPLETED",
                record.getApprover().getRealName(), record.getApprover().getRole(),
                record.getComment(), record.getCreatedAt(), type);
        return new AdvanceTimelineResponse(title, value.getStatus() == pending ? "IN_PROGRESS" : "NOT_STARTED",
                null, null, null, null, type);
    }

    private synchronized String nextNumber() {
        String prefix = "YF" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        int sequence = applicationRepository.findTopByApplicationNumberStartingWithOrderByApplicationNumberDesc(prefix)
                .map(AdvanceApplication::getApplicationNumber)
                .map(number -> Integer.parseInt(number.substring(prefix.length())) + 1).orElse(1);
        if (sequence > 999) throw new BusinessException("当日编号已用完");
        return prefix + String.format("%03d", sequence);
    }

    private void refreshOverdueStatuses() {
        applicationRepository.findAllDetails().forEach(this::refreshOverdue);
    }
    private void refreshOverdue(AdvanceApplication value) {
        if (value.getSettlementStatus() != null
                && value.getSettlementStatus() != SettlementStatus.OFFSET_COMPLETED && isOverdue(value)
                && value.getSettlementStatus() != SettlementStatus.OVERDUE) {
            value.setSettlementStatus(SettlementStatus.OVERDUE);
            applicationRepository.save(value);
        }
    }
    private boolean isOverdue(AdvanceApplication value) {
        LocalDate due = value.getType() == AdvanceType.TEMPORARY_LOAN
                ? value.getExpectedRepaymentDate() : value.getExpectedSettlementDate();
        return due != null && due.isBefore(LocalDate.now());
    }
    private void saveApproval(AdvanceApplication value, AppUser user, String node, ApprovalAction action, String comment) {
        AdvanceApprovalRecord record = new AdvanceApprovalRecord();
        record.setAdvanceApplication(value);
        record.setApprover(user);
        record.setApprovalNode(node);
        record.setAction(action);
        record.setComment(comment);
        approvalRepository.save(record);
    }
    private AdvanceApplication requireApplication(Long id) {
        return applicationRepository.findDetailById(id).orElseThrow(() -> new BusinessException("资金申请不存在"));
    }
    private void requireDraft(AdvanceApplication value) {
        if (value.getStatus() != AdvanceStatus.DRAFT) throw new BusinessException("只有草稿可以修改");
    }
    private void requireOwner(AppUser user, AdvanceApplication value) {
        if (user.getRole() != UserRole.ADMIN && !user.getId().equals(value.getApplicant().getId())) {
            throw new ForbiddenException("只能操作自己的申请");
        }
    }
    private void requireView(AppUser user, AdvanceApplication value) {
        if (!canView(user, value)) throw new ForbiddenException("无权查看该申请");
    }
    private boolean canView(AppUser user, AdvanceApplication value) {
        if (EnumSet.of(UserRole.FINANCE, UserRole.EXECUTIVE, UserRole.ADMIN).contains(user.getRole())) return true;
        if (user.getRole() == UserRole.DEPARTMENT_MANAGER) return sameDepartment(user, value);
        if (user.getRole() == UserRole.CASHIER) return EnumSet.of(
                AdvanceStatus.EXECUTIVE_APPROVED, AdvanceStatus.PAID, AdvanceStatus.COMPLETED).contains(value.getStatus());
        return user.getId().equals(value.getApplicant().getId());
    }
    private boolean sameDepartment(AppUser user, AdvanceApplication value) {
        return user.getDepartment() != null && value.getDepartment() != null
                && user.getDepartment().getId().equals(value.getDepartment().getId());
    }
    private void requireDashboardAccess(AppUser user) {
        if (!EnumSet.of(UserRole.DEPARTMENT_MANAGER, UserRole.FINANCE, UserRole.EXECUTIVE, UserRole.ADMIN)
                .contains(user.getRole())) {
            throw new ForbiddenException("无权访问财务仪表盘");
        }
    }
    private void requireModuleAccess(AppUser user) {
        if (user.getRole() == UserRole.OFFICE) {
            throw new ForbiddenException("办公室角色不能访问暂借款和预付款模块");
        }
    }
    private boolean role(AppUser user, UserRole role) { return user.getRole() == role || user.getRole() == UserRole.ADMIN; }
    private String trim(String value) { return StringUtils.hasText(value) ? value.trim() : null; }
    private String maskBank(String value) {
        if (value == null || value.length() < 8) return value;
        return value.substring(0, 4) + " **** **** " + value.substring(value.length() - 4);
    }
    private void log(AppUser user, String action, AdvanceApplication value, String detail) {
        operationLogService.record(user, "资金往来", action, value.getId(), value.getApplicationNumber(), detail);
    }
}
