package com.geekworkshop.finance.service;

import com.geekworkshop.finance.dto.*;
import com.geekworkshop.finance.entity.*;
import com.geekworkshop.finance.exception.BusinessException;
import com.geekworkshop.finance.exception.ForbiddenException;
import com.geekworkshop.finance.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class PurchaseApplicationService {
    private static final BigDecimal LARGE_PURCHASE_THRESHOLD = new BigDecimal("50000");

    private final PurchaseApplicationRepository applicationRepository;
    private final PurchaseAttachmentRepository attachmentRepository;
    private final PurchaseApprovalRecordRepository approvalRepository;
    private final OperationLogService operationLogService;

    @Value("${app.upload-dir:uploads}")
    private String uploadDir;

    public PurchaseApplicationService(
            PurchaseApplicationRepository applicationRepository,
            PurchaseAttachmentRepository attachmentRepository,
            PurchaseApprovalRecordRepository approvalRepository,
            OperationLogService operationLogService
    ) {
        this.applicationRepository = applicationRepository;
        this.attachmentRepository = attachmentRepository;
        this.approvalRepository = approvalRepository;
        this.operationLogService = operationLogService;
    }

    @Transactional(readOnly = true)
    public List<PurchaseApplicationResponse> list(AppUser user, String keyword, PurchaseStatus status) {
        requireModuleAccess(user);
        String normalized = StringUtils.hasText(keyword) ? keyword.trim().toLowerCase() : null;
        return applicationRepository.findAllDetails().stream()
                .filter(application -> canView(user, application))
                .filter(application -> status == null || application.getStatus() == status)
                .filter(application -> normalized == null
                        || application.getApplicationNumber().toLowerCase().contains(normalized)
                        || application.getPurchaseReason().toLowerCase().contains(normalized)
                        || application.getApplicant().getRealName().toLowerCase().contains(normalized))
                .map(application -> toResponse(application, false))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PurchaseApplicationResponse> pending(AppUser user) {
        requireApprover(user);
        return applicationRepository.findAllDetails().stream()
                .filter(application -> canView(user, application))
                .filter(application -> isPendingFor(user, application))
                .map(application -> toResponse(application, false))
                .toList();
    }

    @Transactional(readOnly = true)
    public PurchaseApplicationResponse detail(AppUser user, Long id) {
        PurchaseApplication application = requireApplication(id);
        requireView(user, application);
        return toResponse(application, true);
    }

    @Transactional
    public PurchaseApplicationResponse create(AppUser user, PurchaseApplicationRequest request) {
        if (!EnumSet.of(UserRole.EMPLOYEE, UserRole.OFFICE, UserRole.ADMIN).contains(user.getRole())) {
            throw new ForbiddenException("当前角色不能创建申购单");
        }
        PurchaseApplication application = new PurchaseApplication();
        application.setApplicationNumber(nextNumber());
        application.setApplicant(user);
        application.setDepartment(user.getDepartment());
        application.setStatus(PurchaseStatus.DRAFT);
        applyRequest(application, request);
        PurchaseApplication saved = applicationRepository.save(application);
        operationLogService.record(user, "申购管理", "新增申购单", saved.getId(),
                saved.getApplicationNumber(), "创建申购申请，金额：" + saved.getAmount());
        return toResponse(saved, true);
    }

    @Transactional
    public PurchaseApplicationResponse update(AppUser user, Long id, PurchaseApplicationRequest request) {
        PurchaseApplication application = requireApplication(id);
        requireOwnerOrAdmin(user, application);
        if (application.getStatus() != PurchaseStatus.DRAFT) {
            throw new BusinessException("只有草稿状态的申购单可以修改");
        }
        applyRequest(application, request);
        operationLogService.record(user, "申购管理", "编辑申购单", id,
                application.getApplicationNumber(), "修改申购申请及采购明细");
        return toResponse(applicationRepository.save(application), true);
    }

    @Transactional
    public void delete(AppUser user, Long id) {
        PurchaseApplication application = requireApplication(id);
        requireOwnerOrAdmin(user, application);
        if (application.getStatus() != PurchaseStatus.DRAFT) {
            throw new BusinessException("只有草稿状态的申购单可以删除");
        }
        operationLogService.record(user, "申购管理", "删除申购单", id,
                application.getApplicationNumber(), "删除申购申请");
        approvalRepository.deleteByPurchaseApplicationId(id);
        attachmentRepository.deleteByPurchaseApplicationId(id);
        applicationRepository.delete(application);
    }

    @Transactional
    public PurchaseApplicationResponse submit(AppUser user, Long id) {
        PurchaseApplication application = requireApplication(id);
        requireOwnerOrAdmin(user, application);
        if (application.getStatus() != PurchaseStatus.DRAFT) {
            throw new BusinessException("只有草稿状态的申购单可以提交");
        }
        validateBeforeApproval(application);
        application.setStatus(PurchaseStatus.SUBMITTED);
        application.setSubmittedAt(LocalDateTime.now());
        operationLogService.record(user, "申购管理", "提交申购单", id,
                application.getApplicationNumber(), "提交财务审核");
        return toResponse(applicationRepository.save(application), true);
    }

    @Transactional
    public PurchaseApplicationResponse approve(AppUser user, Long id, ApprovalRequest request) {
        PurchaseApplication application = requireApplication(id);
        requireView(user, application);
        String node;
        PurchaseStatus nextStatus;
        if (application.getStatus() == PurchaseStatus.SUBMITTED
                && EnumSet.of(UserRole.FINANCE, UserRole.ADMIN).contains(user.getRole())) {
            node = "FINANCE";
            nextStatus = PurchaseStatus.FINANCE_APPROVED;
        } else if (application.getStatus() == PurchaseStatus.FINANCE_APPROVED
                && EnumSet.of(UserRole.DEPARTMENT_MANAGER, UserRole.ADMIN).contains(user.getRole())) {
            if (user.getRole() != UserRole.ADMIN && !sameDepartment(user, application)) {
                throw new ForbiddenException("只能审批本部门申购单");
            }
            node = "DEPARTMENT";
            nextStatus = PurchaseStatus.DEPARTMENT_APPROVED;
        } else if (application.getStatus() == PurchaseStatus.DEPARTMENT_APPROVED
                && EnumSet.of(UserRole.EXECUTIVE, UserRole.ADMIN).contains(user.getRole())) {
            node = "EXECUTIVE";
            nextStatus = PurchaseStatus.COMPLETED;
        } else {
            throw new ForbiddenException("当前角色不能处理该审批节点");
        }

        if (request.getAction() == ApprovalAction.REJECT) {
            if (!StringUtils.hasText(request.getComment())) {
                throw new BusinessException("驳回时必须填写原因");
            }
            application.setStatus(PurchaseStatus.DRAFT);
            application.setSubmittedAt(null);
        } else {
            validateBeforeApproval(application);
            application.setStatus(nextStatus);
        }
        saveApproval(application, user, node, request);
        String action = request.getAction() == ApprovalAction.APPROVE ? "审批通过" : "审批驳回";
        operationLogService.record(user, "申购审批", action, id, application.getApplicationNumber(),
                node + "：" + (StringUtils.hasText(request.getComment()) ? request.getComment() : "无意见"));
        return toResponse(applicationRepository.save(application), true);
    }

    @Transactional
    public PurchaseAttachmentResponse upload(
            AppUser user, Long id, PurchaseAttachmentType attachmentType, MultipartFile file
    ) {
        PurchaseApplication application = requireApplication(id);
        requireOwnerOrAdmin(user, application);
        if (application.getStatus() != PurchaseStatus.DRAFT) {
            throw new BusinessException("仅草稿状态可以上传申购附件");
        }
        String safeName = SecureFileSupport.validate(file);
        String storedName = UUID.randomUUID() + "-" + safeName.replaceAll("[^a-zA-Z0-9._\\-\\u4e00-\\u9fa5]", "_");
        try {
            Path directory = Paths.get(uploadDir, "purchases").toAbsolutePath().normalize();
            Files.createDirectories(directory);
            Files.copy(file.getInputStream(), directory.resolve(storedName), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            throw new BusinessException("附件保存失败");
        }
        PurchaseAttachment attachment = new PurchaseAttachment();
        attachment.setPurchaseApplication(application);
        attachment.setAttachmentType(attachmentType);
        attachment.setFileName(safeName);
        attachment.setFileType(file.getContentType());
        attachment.setFileSize(file.getSize());
        attachment.setFileUrl("/uploads/purchases/" + storedName);
        PurchaseAttachment saved = attachmentRepository.save(attachment);
        operationLogService.record(user, "申购附件", "上传附件", id, application.getApplicationNumber(),
                attachmentType + "：" + safeName);
        return PurchaseAttachmentResponse.fromEntity(saved);
    }

    private void applyRequest(PurchaseApplication application, PurchaseApplicationRequest request) {
        application.setApplicantPhone(request.getApplicantPhone());
        application.setBudgetNumber(request.getBudgetNumber());
        application.setPurchaseMethod(request.getPurchaseMethod().trim());
        application.setTaxExempt(Boolean.TRUE.equals(request.getTaxExempt()));
        application.setUseLocation(request.getUseLocation());
        application.setPurchaseReason(request.getPurchaseReason().trim());
        application.setAssetAcceptanceNumber(request.getAssetAcceptanceNumber());
        application.getItems().clear();
        BigDecimal amount = BigDecimal.ZERO;
        for (PurchaseItemRequest itemRequest : request.getItems()) {
            PurchaseItem item = new PurchaseItem();
            item.setPurchaseApplication(application);
            item.setItemName(itemRequest.getItemName().trim());
            item.setSpecification(itemRequest.getSpecification());
            item.setManufacturer(itemRequest.getManufacturer());
            item.setUnitPrice(itemRequest.getUnitPrice());
            item.setQuantity(itemRequest.getQuantity());
            item.setTotalPrice(itemRequest.getUnitPrice().multiply(BigDecimal.valueOf(itemRequest.getQuantity())));
            application.getItems().add(item);
            amount = amount.add(item.getTotalPrice());
        }
        application.setAmount(amount);
    }

    private void validateBeforeApproval(PurchaseApplication application) {
        if (application.getItems().isEmpty() || application.getAmount() == null
                || application.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("申购单至少需要一条有效采购明细");
        }
        if (application.getAmount().compareTo(LARGE_PURCHASE_THRESHOLD) > 0
                && !attachmentRepository.existsByPurchaseApplicationIdAndAttachmentType(
                        application.getId(), PurchaseAttachmentType.MEETING_MINUTES)) {
            throw new BusinessException("超过 5 万元的申购必须上传院务委员会审议材料");
        }
    }

    private void saveApproval(PurchaseApplication application, AppUser user, String node, ApprovalRequest request) {
        PurchaseApprovalRecord record = new PurchaseApprovalRecord();
        record.setPurchaseApplication(application);
        record.setApprover(user);
        record.setApprovalNode(node);
        record.setAction(request.getAction());
        record.setComment(request.getComment());
        approvalRepository.save(record);
    }

    private synchronized String nextNumber() {
        String prefix = "CG" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        int sequence = applicationRepository.findTopByApplicationNumberStartingWithOrderByApplicationNumberDesc(prefix)
                .map(PurchaseApplication::getApplicationNumber)
                .map(number -> Integer.parseInt(number.substring(prefix.length())) + 1)
                .orElse(1);
        return prefix + String.format("%03d", sequence);
    }

    private PurchaseApplication requireApplication(Long id) {
        return applicationRepository.findDetailById(id)
                .orElseThrow(() -> new BusinessException("申购单不存在"));
    }

    private PurchaseApplicationResponse toResponse(PurchaseApplication application, boolean full) {
        List<PurchaseAttachmentResponse> attachments = full
                ? attachmentRepository.findByPurchaseApplicationIdOrderByCreatedAtAsc(application.getId())
                    .stream().map(PurchaseAttachmentResponse::fromEntity).toList()
                : List.of();
        List<PurchaseApprovalResponse> approvals = full
                ? approvalRepository.findByPurchaseApplicationIdOrderByCreatedAtAsc(application.getId())
                    .stream().map(PurchaseApprovalResponse::fromEntity).toList()
                : List.of();
        return PurchaseApplicationResponse.fromEntity(application, attachments, approvals);
    }

    private void requireModuleAccess(AppUser user) {
        if (user.getRole() == UserRole.CASHIER) {
            throw new ForbiddenException("当前角色不能访问申购管理");
        }
    }

    private void requireApprover(AppUser user) {
        if (!EnumSet.of(UserRole.FINANCE, UserRole.DEPARTMENT_MANAGER, UserRole.EXECUTIVE, UserRole.ADMIN)
                .contains(user.getRole())) {
            throw new ForbiddenException("当前角色不能审批申购单");
        }
    }

    private boolean canView(AppUser user, PurchaseApplication application) {
        return switch (user.getRole()) {
            case ADMIN, FINANCE, EXECUTIVE -> true;
            case DEPARTMENT_MANAGER -> sameDepartment(user, application);
            case EMPLOYEE, OFFICE -> application.getApplicant().getId().equals(user.getId());
            default -> false;
        };
    }

    private boolean isPendingFor(AppUser user, PurchaseApplication application) {
        return switch (user.getRole()) {
            case FINANCE -> application.getStatus() == PurchaseStatus.SUBMITTED;
            case DEPARTMENT_MANAGER -> application.getStatus() == PurchaseStatus.FINANCE_APPROVED;
            case EXECUTIVE -> application.getStatus() == PurchaseStatus.DEPARTMENT_APPROVED;
            case ADMIN -> EnumSet.of(PurchaseStatus.SUBMITTED, PurchaseStatus.FINANCE_APPROVED,
                    PurchaseStatus.DEPARTMENT_APPROVED).contains(application.getStatus());
            default -> false;
        };
    }

    private boolean sameDepartment(AppUser user, PurchaseApplication application) {
        return user.getDepartment() != null && application.getDepartment() != null
                && user.getDepartment().getId().equals(application.getDepartment().getId());
    }

    private void requireView(AppUser user, PurchaseApplication application) {
        if (!canView(user, application)) {
            throw new ForbiddenException("无权查看该申购单");
        }
    }

    private void requireOwnerOrAdmin(AppUser user, PurchaseApplication application) {
        if (user.getRole() != UserRole.ADMIN && !application.getApplicant().getId().equals(user.getId())) {
            throw new ForbiddenException("只能操作自己的申购单");
        }
    }
}
