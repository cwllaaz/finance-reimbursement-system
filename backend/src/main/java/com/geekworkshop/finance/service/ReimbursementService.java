package com.geekworkshop.finance.service;

import com.geekworkshop.finance.dto.ApprovalRecordResponse;
import com.geekworkshop.finance.dto.ApprovalRequest;
import com.geekworkshop.finance.dto.AttachmentResponse;
import com.geekworkshop.finance.dto.BudgetResponse;
import com.geekworkshop.finance.dto.DashboardStatsResponse;
import com.geekworkshop.finance.dto.InvoiceOcrRequest;
import com.geekworkshop.finance.dto.InvoiceOcrResponse;
import com.geekworkshop.finance.dto.OcrResponse;
import com.geekworkshop.finance.dto.PaymentRequest;
import com.geekworkshop.finance.dto.ReimbursementDetailResponse;
import com.geekworkshop.finance.dto.ReimbursementRequest;
import com.geekworkshop.finance.dto.ReimbursementResponse;
import com.geekworkshop.finance.dto.ReimbursementTimelineNodeResponse;
import com.geekworkshop.finance.entity.ApprovalAction;
import com.geekworkshop.finance.entity.ApprovalRecord;
import com.geekworkshop.finance.entity.AppUser;
import com.geekworkshop.finance.entity.Attachment;
import com.geekworkshop.finance.entity.AttachmentType;
import com.geekworkshop.finance.entity.Budget;
import com.geekworkshop.finance.entity.Department;
import com.geekworkshop.finance.entity.InvoiceOcrResult;
import com.geekworkshop.finance.entity.InvoiceOcrStatus;
import com.geekworkshop.finance.entity.Reimbursement;
import com.geekworkshop.finance.entity.ReimbursementStatus;
import com.geekworkshop.finance.entity.UserRole;
import com.geekworkshop.finance.exception.BusinessException;
import com.geekworkshop.finance.exception.ForbiddenException;
import com.geekworkshop.finance.repository.ApprovalRecordRepository;
import com.geekworkshop.finance.repository.AppUserRepository;
import com.geekworkshop.finance.repository.AttachmentRepository;
import com.geekworkshop.finance.repository.BudgetRepository;
import com.geekworkshop.finance.repository.DepartmentRepository;
import com.geekworkshop.finance.repository.InvoiceOcrResultRepository;
import com.geekworkshop.finance.repository.ReimbursementRepository;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ReimbursementService {

    private static final BigDecimal HIGH_VALUE_THRESHOLD = new BigDecimal("50000.00");

    private final ReimbursementRepository reimbursementRepository;
    private final AppUserRepository appUserRepository;
    private final DepartmentRepository departmentRepository;
    private final ApprovalRecordRepository approvalRecordRepository;
    private final BudgetRepository budgetRepository;
    private final AttachmentRepository attachmentRepository;
    private final InvoiceOcrResultRepository invoiceOcrResultRepository;
    private final BaiduOcrService baiduOcrService;
    private final OperationLogService operationLogService;

    @Value("${app.upload-dir:uploads}")
    private String uploadDir;

    public ReimbursementService(
            ReimbursementRepository reimbursementRepository,
            AppUserRepository appUserRepository,
            DepartmentRepository departmentRepository,
            ApprovalRecordRepository approvalRecordRepository,
            BudgetRepository budgetRepository,
            AttachmentRepository attachmentRepository,
            InvoiceOcrResultRepository invoiceOcrResultRepository,
            BaiduOcrService baiduOcrService,
            OperationLogService operationLogService
    ) {
        this.reimbursementRepository = reimbursementRepository;
        this.appUserRepository = appUserRepository;
        this.departmentRepository = departmentRepository;
        this.approvalRecordRepository = approvalRecordRepository;
        this.budgetRepository = budgetRepository;
        this.attachmentRepository = attachmentRepository;
        this.invoiceOcrResultRepository = invoiceOcrResultRepository;
        this.baiduOcrService = baiduOcrService;
        this.operationLogService = operationLogService;
    }

    @Transactional(readOnly = true)
    public List<ReimbursementResponse> list(
            AppUser currentUser,
            String keyword,
            ReimbursementStatus status,
            List<ReimbursementStatus> statuses,
            LocalDate dateFrom,
            LocalDate dateTo
    ) {
        if (!canAccessReimbursements(currentUser)) {
            throw new ForbiddenException("no permission to access reimbursements");
        }
        if (dateFrom != null && dateTo != null && dateFrom.isAfter(dateTo)) {
            throw new BusinessException("开始日期不能晚于结束日期");
        }
        String normalizedKeyword = StringUtils.hasText(keyword) ? keyword.trim() : null;
        return reimbursementRepository.search(normalizedKeyword, status)
                .stream()
                .filter(item -> canView(currentUser, item))
                .filter(item -> statuses == null || statuses.isEmpty() || statuses.contains(item.getStatus()))
                .filter(item -> dateFrom == null
                        || (item.getExpenseDate() != null && !item.getExpenseDate().isBefore(dateFrom)))
                .filter(item -> dateTo == null
                        || (item.getExpenseDate() != null && !item.getExpenseDate().isAfter(dateTo)))
                .map(ReimbursementResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ReimbursementResponse> pending(AppUser currentUser, String keyword) {
        if (currentUser.getRole() != UserRole.DEPARTMENT_MANAGER
                && currentUser.getRole() != UserRole.FINANCE
                && currentUser.getRole() != UserRole.EXECUTIVE
                && currentUser.getRole() != UserRole.ADMIN) {
            throw new ForbiddenException("no permission to access approval tasks");
        }
        String normalizedKeyword = StringUtils.hasText(keyword) ? keyword.trim() : null;
        return reimbursementRepository.search(normalizedKeyword, null)
                .stream()
                .filter(item -> isPendingForUser(currentUser, item))
                .map(ReimbursementResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ReimbursementResponse> paymentTasks(AppUser currentUser) {
        if (currentUser.getRole() != UserRole.CASHIER
                && currentUser.getRole() != UserRole.FINANCE
                && currentUser.getRole() != UserRole.ADMIN) {
            throw new ForbiddenException("no permission to access payment tasks");
        }

        return reimbursementRepository.search(null, null)
                .stream()
                .filter(item -> item.getStatus() == ReimbursementStatus.EXECUTIVE_APPROVED)
                .map(ReimbursementResponse::fromEntity)
                .toList();
    }

    @Transactional
    public byte[] exportExcel(AppUser currentUser) {
        if (currentUser.getRole() != UserRole.FINANCE && currentUser.getRole() != UserRole.ADMIN) {
            throw new BusinessException("only finance and admin can export reimbursements");
        }

        operationLogService.record(currentUser, "报表导出", "导出 Excel", null, "报销申请数据", "导出所有报销单数据");
        List<Reimbursement> reimbursements = reimbursementRepository.findAllForExport();
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            XSSFSheet sheet = workbook.createSheet("报销申请数据");
            CreationHelper creationHelper = workbook.getCreationHelper();

            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(headerFont);

            CellStyle moneyStyle = workbook.createCellStyle();
            moneyStyle.setDataFormat(creationHelper.createDataFormat().getFormat("#,##0.00"));

            CellStyle dateStyle = workbook.createCellStyle();
            dateStyle.setDataFormat(creationHelper.createDataFormat().getFormat("yyyy-mm-dd"));

            CellStyle dateTimeStyle = workbook.createCellStyle();
            dateTimeStyle.setDataFormat(creationHelper.createDataFormat().getFormat("yyyy-mm-dd hh:mm:ss"));

            String[] headers = {
                    "报销单ID", "报销标题", "申请人", "所属部门", "费用类型",
                    "报销金额", "发生日期", "报销状态", "提交时间", "创建时间", "说明"
            };
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            for (int i = 0; i < reimbursements.size(); i++) {
                Reimbursement reimbursement = reimbursements.get(i);
                Row row = sheet.createRow(i + 1);
                writeNumber(row, 0, reimbursement.getId());
                writeText(row, 1, reimbursement.getTitle());
                writeText(row, 2, reimbursement.getApplicant() == null ? "" : reimbursement.getApplicant().getRealName());
                writeText(row, 3, reimbursement.getDepartment() == null ? "" : reimbursement.getDepartment().getName());
                writeText(row, 4, reimbursement.getExpenseType());
                writeMoney(row, 5, reimbursement.getAmount(), moneyStyle);
                writeDate(row, 6, reimbursement.getExpenseDate(), dateStyle);
                writeText(row, 7, statusLabel(reimbursement.getStatus()));
                writeDateTime(row, 8, reimbursement.getSubmittedAt(), dateTimeStyle);
                writeDateTime(row, 9, reimbursement.getCreatedAt(), dateTimeStyle);
                writeText(row, 10, reimbursement.getDescription());
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
                sheet.setColumnWidth(i, Math.min(sheet.getColumnWidth(i) + 800, 12000));
            }
            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException exception) {
            throw new BusinessException("failed to export reimbursement excel");
        }
    }

    @Transactional(readOnly = true)
    public ReimbursementResponse getById(AppUser currentUser, Long id) {
        Reimbursement reimbursement = findDetail(id);
        assertCanView(currentUser, reimbursement);
        return ReimbursementResponse.fromEntity(reimbursement);
    }

    @Transactional
    public ReimbursementResponse create(AppUser currentUser, ReimbursementRequest request) {
        Reimbursement reimbursement = new Reimbursement();
        reimbursement.setApprovalNumber(generateApprovalNumber());
        fillEntity(reimbursement, request, currentUser);
        reimbursement.setStatus(ReimbursementStatus.DRAFT);
        reimbursement.setSubmittedAt(null);
        Reimbursement saved = reimbursementRepository.save(reimbursement);
        operationLogService.record(currentUser, "报销管理", "新增报销单", saved.getId(), saved.getTitle(), reimbursementDetail(saved));
        return ReimbursementResponse.fromEntity(saved);
    }

    @Transactional
    public ReimbursementResponse update(AppUser currentUser, Long id, ReimbursementRequest request) {
        Reimbursement reimbursement = findEntity(id);
        assertCanManage(currentUser, reimbursement);
        fillEntity(reimbursement, request, currentUser);
        Reimbursement saved = reimbursementRepository.save(reimbursement);
        operationLogService.record(currentUser, "报销管理", "编辑报销单", saved.getId(), saved.getTitle(), reimbursementDetail(saved));
        return ReimbursementResponse.fromEntity(saved);
    }

    @Transactional
    public void delete(AppUser currentUser, Long id) {
        Reimbursement reimbursement = findEntity(id);
        assertCanManage(currentUser, reimbursement);
        String targetName = reimbursement.getTitle();
        String detail = reimbursementDetail(reimbursement);
        reimbursementRepository.delete(reimbursement);
        operationLogService.record(currentUser, "报销管理", "删除报销单", id, targetName, detail);
    }

    @Transactional
    public ReimbursementResponse submit(AppUser currentUser, Long id) {
        Reimbursement reimbursement = findDetail(id);

        if (currentUser.getRole() != UserRole.EMPLOYEE && currentUser.getRole() != UserRole.ADMIN) {
            throw new BusinessException("only employees can submit reimbursements");
        }
        if (reimbursement.getApplicant() == null || !reimbursement.getApplicant().getId().equals(currentUser.getId())) {
            throw new BusinessException("only the applicant can submit this reimbursement");
        }
        if (reimbursement.getStatus() != ReimbursementStatus.DRAFT) {
            throw new BusinessException("only draft reimbursements can be submitted");
        }
        validateSubmissionMaterials(reimbursement);

        reimbursement.setStatus(ReimbursementStatus.SUBMITTED);
        reimbursement.setSubmittedAt(LocalDateTime.now());
        Reimbursement saved = reimbursementRepository.save(reimbursement);
        operationLogService.record(currentUser, "报销管理", "提交报销单", saved.getId(), saved.getTitle(), "提交进入部门审批");
        return ReimbursementResponse.fromEntity(saved);
    }

    @Transactional
    public ReimbursementResponse approve(AppUser currentUser, Long id, ApprovalRequest request) {
        Reimbursement reimbursement = findDetail(id);

        requireRejectionReason(request);

        if (currentUser.getRole() == UserRole.FINANCE) {
            return handleFinanceApproval(currentUser, reimbursement, request);
        }
        if (currentUser.getRole() == UserRole.DEPARTMENT_MANAGER) {
            return handleDepartmentApproval(currentUser, reimbursement, request);
        }
        if (currentUser.getRole() == UserRole.EXECUTIVE) {
            return handleExecutiveApproval(currentUser, reimbursement, request);
        }
        if (currentUser.getRole() == UserRole.ADMIN) {
            return switch (reimbursement.getStatus()) {
                case SUBMITTED -> handleFinanceApproval(currentUser, reimbursement, request);
                case FINANCE_INITIAL_APPROVED -> handleDepartmentApproval(currentUser, reimbursement, request);
                case DEPARTMENT_APPROVED -> handleExecutiveApproval(currentUser, reimbursement, request);
                case PAID -> handleFinanceApproval(currentUser, reimbursement, request);
                default -> throw new BusinessException("current reimbursement has no pending approval");
            };
        }

        throw new BusinessException("no permission to approve reimbursements");
    }

    @Transactional
    public ReimbursementResponse confirmPayment(AppUser currentUser, Long id, PaymentRequest request) {
        Reimbursement reimbursement = findDetail(id);
        if (currentUser.getRole() != UserRole.CASHIER && currentUser.getRole() != UserRole.ADMIN) {
            throw new ForbiddenException("only cashier can confirm payment");
        }
        if (reimbursement.getStatus() != ReimbursementStatus.EXECUTIVE_APPROVED) {
            throw new BusinessException("payment is only allowed after executive approval");
        }
        if (!attachmentRepository.existsByReimbursementIdAndAttachmentType(id, AttachmentType.BANK_RECEIPT)) {
            throw new BusinessException("bank receipt attachment is required before confirming payment");
        }

        reimbursement.setPaymentDate(request.getPaymentDate());
        reimbursement.setPaymentTotal(request.getPaymentAmount());
        reimbursement.setPaymentVoucherNumber(request.getVoucherNumber().trim());
        reimbursement.setStatus(ReimbursementStatus.PAID);
        saveApprovalRecord(reimbursement, currentUser, "CASHIER_PAYMENT", ApprovalAction.APPROVE, request.getComment());
        Reimbursement saved = reimbursementRepository.save(reimbursement);
        operationLogService.record(
                currentUser, "付款管理", "出纳付款", saved.getId(), saved.getTitle(),
                "付款日期：" + request.getPaymentDate() + "，金额：" + request.getPaymentAmount()
                        + "，凭证号：" + request.getVoucherNumber().trim()
        );
        return ReimbursementResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<ApprovalRecordResponse> approvalRecords(AppUser currentUser, Long id) {
        Reimbursement reimbursement = findDetail(id);
        assertCanView(currentUser, reimbursement);

        return approvalRecordRepository.findDetailByReimbursementIdOrderByCreatedAtAsc(id)
                .stream()
                .map(ApprovalRecordResponse::fromEntity)
                .toList();
    }

    @Transactional
    public ReimbursementDetailResponse detail(AppUser currentUser, Long id) {
        Reimbursement reimbursement = findDetail(id);
        assertCanView(currentUser, reimbursement);

        List<ApprovalRecordResponse> records = approvalRecordRepository
                .findDetailByReimbursementIdOrderByCreatedAtAsc(id)
                .stream()
                .map(ApprovalRecordResponse::fromEntity)
                .toList();
        List<AttachmentResponse> attachments = attachmentRepository
                .findByReimbursementIdOrderByCreatedAtDesc(id)
                .stream()
                .map(AttachmentResponse::fromEntity)
                .toList();
        InvoiceOcrResponse invoiceOcr = invoiceOcrResultRepository.findByReimbursementId(id)
                .map(result -> invoiceOcrResponseWithVerification(result, reimbursement))
                .orElse(null);

        return new ReimbursementDetailResponse(
                ReimbursementResponse.fromEntity(reimbursement),
                records,
                attachments,
                invoiceOcr
        );
    }

    @Transactional(readOnly = true)
    public List<ReimbursementTimelineNodeResponse> timeline(AppUser currentUser, Long id) {
        Reimbursement reimbursement = findDetail(id);
        assertCanView(currentUser, reimbursement);

        List<ApprovalRecord> records = approvalRecordRepository.findDetailByReimbursementIdOrderByCreatedAtAsc(id);
        ApprovalRecord financeInitial = latestRecord(records, "FINANCE_INITIAL");
        ApprovalRecord department = latestRecord(records, "DEPARTMENT");
        ApprovalRecord executive = latestRecord(records, "EXECUTIVE");
        ApprovalRecord payment = latestRecord(records, "CASHIER_PAYMENT");
        ApprovalRecord financeRecheck = latestRecord(records, "FINANCE_RECHECK");

        return List.of(
                creationNode(reimbursement),
                submitNode(reimbursement),
                processNode("财务初审", "FINANCE_INITIAL", ReimbursementStatus.SUBMITTED, financeInitial, reimbursement),
                processNode("部门负责人审批", "DEPARTMENT", ReimbursementStatus.FINANCE_INITIAL_APPROVED, department, reimbursement),
                processNode("执行院长审批", "EXECUTIVE", ReimbursementStatus.DEPARTMENT_APPROVED, executive, reimbursement),
                processNode("出纳付款并上传银行回执", "CASHIER_PAYMENT", ReimbursementStatus.EXECUTIVE_APPROVED, payment, reimbursement),
                processNode("财务复核", "FINANCE_RECHECK", ReimbursementStatus.PAID, financeRecheck, reimbursement),
                researchInstituteFinalNode(reimbursement, financeRecheck)
        );
    }

    @Transactional
    public AttachmentResponse uploadAttachment(
            AppUser currentUser,
            Long id,
            AttachmentType attachmentType,
            MultipartFile file
    ) {
        Reimbursement reimbursement = findDetail(id);
        boolean cashierReceiptUpload = currentUser.getRole() == UserRole.CASHIER
                && attachmentType == AttachmentType.BANK_RECEIPT
                && reimbursement.getStatus() == ReimbursementStatus.EXECUTIVE_APPROVED;
        if (!cashierReceiptUpload) {
            assertCanManage(currentUser, reimbursement);
        }

        String originalName = SecureFileSupport.validate(file);

        try {
            AttachmentType resolvedType = attachmentType == null ? AttachmentType.OTHER : attachmentType;
            String typeDirectory = resolvedType.name().toLowerCase();
            Path attachmentUploadDir = Paths.get(uploadDir, typeDirectory).toAbsolutePath().normalize();
            Files.createDirectories(attachmentUploadDir);

            String safeName = originalName.replaceAll("[^a-zA-Z0-9._\\-\\u4e00-\\u9fa5]", "_");
            String storedName = UUID.randomUUID() + "-" + safeName;
            Path target = attachmentUploadDir.resolve(storedName);
            file.transferTo(target);

            Attachment attachment = new Attachment();
            attachment.setReimbursement(reimbursement);
            attachment.setFileName(originalName);
            attachment.setFileUrl("/uploads/" + typeDirectory + "/" + storedName);
            attachment.setFileType(file.getContentType());
            attachment.setFileSize(file.getSize());
            attachment.setAttachmentType(resolvedType);
            Attachment saved = attachmentRepository.save(attachment);
            operationLogService.record(
                    currentUser,
                    "附件管理",
                    resolvedType == AttachmentType.INVOICE ? "上传发票附件" : "上传附件",
                    reimbursement.getId(),
                    reimbursement.getTitle(),
                    "附件类型：" + resolvedType.name() + "，上传文件：" + originalName
            );
            return AttachmentResponse.fromEntity(saved);
        } catch (IOException exception) {
            throw new BusinessException("failed to save uploaded file");
        }
    }

    @Transactional(readOnly = true)
    public OcrResponse simulateOcr(AppUser currentUser, Long id) {
        Reimbursement reimbursement = findDetail(id);
        assertCanView(currentUser, reimbursement);

        int seed = Math.abs((int) (reimbursement.getId() * 37 + reimbursement.getAmount().intValue()));
        String invoiceNo = "SIM" + Year.now().getValue() + String.format("%06d", seed % 1000000);
        BigDecimal amount = reimbursement.getAmount();
        LocalDate invoiceDate = reimbursement.getExpenseDate();
        String vendor = switch (reimbursement.getExpenseType()) {
            case "交通费" -> "城市交通服务有限公司";
            case "住宿费" -> "温州商务酒店";
            case "餐饮费" -> "园区餐饮服务中心";
            default -> "模拟发票供应商";
        };
        return new OcrResponse(invoiceNo, amount, invoiceDate, vendor, "模拟 OCR 识别成功");
    }

    @Transactional
    public InvoiceOcrResponse getInvoiceOcr(AppUser currentUser, Long id) {
        Reimbursement reimbursement = findDetail(id);
        assertCanView(currentUser, reimbursement);
        return invoiceOcrResultRepository.findByReimbursementId(id)
                .map(result -> invoiceOcrResponseWithVerification(result, reimbursement))
                .orElse(null);
    }

    @Transactional
    public InvoiceOcrResponse recognizeInvoice(AppUser currentUser, Long id) {
        Reimbursement reimbursement = findDetail(id);
        assertCanManage(currentUser, reimbursement);

        Attachment attachment = attachmentRepository
                .findByReimbursementIdAndAttachmentTypeOrderByCreatedAtDesc(id, AttachmentType.INVOICE)
                .stream()
                .findFirst()
                .or(() -> attachmentRepository.findByReimbursementIdOrderByCreatedAtDesc(id)
                        .stream()
                        .filter(item -> item.getAttachmentType() == null)
                        .findFirst())
                .orElseThrow(() -> new BusinessException("please upload invoice attachment first"));

        InvoiceOcrResult result = invoiceOcrResultRepository.findByReimbursementId(id)
                .orElseGet(() -> {
                    InvoiceOcrResult created = new InvoiceOcrResult();
                    created.setReimbursement(reimbursement);
                    return created;
                });
        result.setAttachment(attachment);

        InvoiceOcrRequest recognized;
        String rawJson;
        if (baiduOcrService.isConfigured() && isImageAttachment(attachment)) {
            BaiduOcrService.RecognizedInvoice baiduResult = baiduOcrService.recognizeVatInvoice(resolveAttachmentPath(attachment));
            recognized = baiduResult.invoice();
            rawJson = baiduResult.rawJson();
        } else {
            recognized = buildSimulatedInvoice(reimbursement);
            rawJson = "{\"source\":\"simulation\",\"reason\":\"baidu ocr key not configured or file is not image\"}";
        }

        fillInvoiceResult(result, recognized);
        verifyInvoiceAmount(result, reimbursement);
        result.setRawOcrJson(rawJson);
        result.setOcrStatus(InvoiceOcrStatus.RECOGNIZED);
        InvoiceOcrResult saved = invoiceOcrResultRepository.save(result);
        operationLogService.record(
                currentUser,
                "OCR识别",
                "OCR 识别",
                reimbursement.getId(),
                reimbursement.getTitle(),
                "识别发票号码：" + nullSafe(saved.getInvoiceNumber())
        );
        return InvoiceOcrResponse.fromEntity(saved);
    }

    @Transactional
    public InvoiceOcrResponse saveInvoiceOcr(AppUser currentUser, Long id, InvoiceOcrRequest request) {
        Reimbursement reimbursement = findDetail(id);
        assertCanManage(currentUser, reimbursement);

        InvoiceOcrResult result = invoiceOcrResultRepository.findByReimbursementId(id)
                .orElseGet(() -> {
                    InvoiceOcrResult created = new InvoiceOcrResult();
                    created.setReimbursement(reimbursement);
                    return created;
                });
        fillInvoiceResult(result, request);
        verifyInvoiceAmount(result, reimbursement);
        if (result.getOcrStatus() == null || result.getOcrStatus() == InvoiceOcrStatus.UNRECOGNIZED) {
            result.setOcrStatus(InvoiceOcrStatus.RECOGNIZED);
        }
        InvoiceOcrResult saved = invoiceOcrResultRepository.save(result);
        operationLogService.record(
                currentUser,
                "OCR识别",
                "OCR 保存",
                reimbursement.getId(),
                reimbursement.getTitle(),
                "保存发票金额：" + nullSafe(saved.getAmount()) + "，" + nullSafe(saved.getVerificationMessage())
        );
        return InvoiceOcrResponse.fromEntity(saved);
    }

    @Transactional
    public InvoiceOcrResponse confirmInvoiceOcr(AppUser currentUser, Long id) {
        Reimbursement reimbursement = findDetail(id);
        assertCanManage(currentUser, reimbursement);

        InvoiceOcrResult result = invoiceOcrResultRepository.findByReimbursementId(id)
                .orElseThrow(() -> new BusinessException("invoice ocr result not found"));
        verifyInvoiceAmount(result, reimbursement);
        result.setOcrStatus(InvoiceOcrStatus.CONFIRMED);
        InvoiceOcrResult saved = invoiceOcrResultRepository.save(result);
        operationLogService.record(
                currentUser,
                "OCR识别",
                "OCR 确认",
                reimbursement.getId(),
                reimbursement.getTitle(),
                "确认发票金额：" + nullSafe(saved.getAmount())
        );
        return InvoiceOcrResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public DashboardStatsResponse dashboardStats(AppUser currentUser) {
        if (currentUser.getRole() != UserRole.DEPARTMENT_MANAGER
                && currentUser.getRole() != UserRole.FINANCE
                && currentUser.getRole() != UserRole.EXECUTIVE
                && currentUser.getRole() != UserRole.ADMIN) {
            throw new ForbiddenException("no permission to access financial dashboard");
        }

        List<Reimbursement> visible = reimbursementRepository.search(null, null)
                .stream()
                .filter(item -> canView(currentUser, item))
                .toList();

        YearMonth currentMonth = YearMonth.now();
        BigDecimal monthAmount = visible.stream()
                .filter(item -> item.getExpenseDate() != null && YearMonth.from(item.getExpenseDate()).equals(currentMonth))
                .map(Reimbursement::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long pendingCount = visible.stream()
                .filter(item -> item.getStatus() == ReimbursementStatus.SUBMITTED
                        || item.getStatus() == ReimbursementStatus.FINANCE_INITIAL_APPROVED
                        || item.getStatus() == ReimbursementStatus.DEPARTMENT_APPROVED
                        || item.getStatus() == ReimbursementStatus.PAID)
                .count();

        Map<String, Long> statusCounts = visible.stream()
                .collect(Collectors.groupingBy(item -> item.getStatus().name(), LinkedHashMap::new, Collectors.counting()));

        List<BudgetResponse> budgetResponses = budgetRepository.findAllWithDepartment()
                .stream()
                .filter(item -> canViewBudget(currentUser, item))
                .map(BudgetResponse::fromEntity)
                .toList();

        return new DashboardStatsResponse(monthAmount, pendingCount, (long) visible.size(), statusCounts, budgetResponses);
    }

    private Reimbursement findEntity(Long id) {
        return reimbursementRepository.findById(id)
                .orElseThrow(() -> new BusinessException("reimbursement not found"));
    }

    private Reimbursement findDetail(Long id) {
        return reimbursementRepository.findDetailById(id)
                .orElseThrow(() -> new BusinessException("reimbursement not found"));
    }

    private void fillEntity(Reimbursement reimbursement, ReimbursementRequest request, AppUser currentUser) {
        reimbursement.setTitle(request.getTitle().trim());
        reimbursement.setExpenseType(request.getExpenseType().trim());
        reimbursement.setAmount(request.getAmount());
        reimbursement.setExpenseDate(request.getExpenseDate());
        reimbursement.setDescription(StringUtils.hasText(request.getDescription()) ? request.getDescription().trim() : null);
        reimbursement.setApplicantPhone(StringUtils.hasText(request.getApplicantPhone())
                ? request.getApplicantPhone().trim()
                : currentUser.getPhone());
        reimbursement.setBudgetNumber(trimToNull(request.getBudgetNumber()));
        reimbursement.setReimbursementReason(trimToNull(request.getReimbursementReason()));
        reimbursement.setPaymentDate(request.getPaymentDate());
        reimbursement.setPayeeName(trimToNull(request.getPayeeName()));
        reimbursement.setBankAccount(trimToNull(request.getBankAccount()));
        reimbursement.setBankName(trimToNull(request.getBankName()));
        reimbursement.setPaymentTotal(request.getPaymentTotal() == null ? request.getAmount() : request.getPaymentTotal());
        reimbursement.setRelatedPurchaseNumber(trimToNull(request.getRelatedPurchaseNumber()));
        reimbursement.setHighValueExplanation(trimToNull(request.getHighValueExplanation()));
        reimbursement.setApplicant(resolveApplicant(request, currentUser));
        reimbursement.setDepartment(resolveDepartment(request, currentUser));

        if (reimbursement.getStatus() == null) {
            reimbursement.setStatus(ReimbursementStatus.DRAFT);
        }
    }

    private AppUser resolveApplicant(ReimbursementRequest request, AppUser currentUser) {
        if (currentUser.getRole() != UserRole.ADMIN || request.getApplicantId() == null) {
            return currentUser;
        }

        return appUserRepository.findById(request.getApplicantId())
                .orElseThrow(() -> new BusinessException("applicant not found"));
    }

    private synchronized String generateApprovalNumber() {
        String prefix = "BX" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        int nextSequence = reimbursementRepository
                .findTopByApprovalNumberStartingWithOrderByApprovalNumberDesc(prefix)
                .map(Reimbursement::getApprovalNumber)
                .map(number -> number.substring(prefix.length()))
                .filter(sequence -> sequence.matches("\\d{3}"))
                .map(Integer::parseInt)
                .map(sequence -> sequence + 1)
                .orElse(1);
        if (nextSequence > 999) {
            throw new BusinessException("daily reimbursement number limit exceeded");
        }
        return prefix + String.format("%03d", nextSequence);
    }

    private void validateSubmissionMaterials(Reimbursement reimbursement) {
        Long reimbursementId = reimbursement.getId();
        if (!attachmentRepository.existsByReimbursementIdAndAttachmentType(
                reimbursementId,
                AttachmentType.INVOICE
        )) {
            throw new BusinessException("提交报销单前必须至少上传 1 份发票");
        }
        if (!attachmentRepository.existsByReimbursementIdAndAttachmentTypeIn(
                reimbursementId,
                List.of(
                        AttachmentType.CONTRACT,
                        AttachmentType.MEETING_MINUTES,
                        AttachmentType.BANK_RECEIPT,
                        AttachmentType.OTHER
                )
        )) {
            throw new BusinessException("提交报销单前必须至少上传 1 份其他凭证（合同、会议记录、付款凭证或其他证明材料）");
        }

        if (reimbursement.getAmount() == null
                || reimbursement.getAmount().compareTo(HIGH_VALUE_THRESHOLD) <= 0) {
            return;
        }
        if (!StringUtils.hasText(reimbursement.getHighValueExplanation())) {
            throw new BusinessException("large reimbursement explanation is required for amounts over 50000");
        }
        if (!attachmentRepository.existsByReimbursementIdAndAttachmentType(
                reimbursement.getId(),
                AttachmentType.MEETING_MINUTES
        )) {
            throw new BusinessException("meeting review material is required for amounts over 50000");
        }
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private Department resolveDepartment(ReimbursementRequest request, AppUser currentUser) {
        if (currentUser.getRole() != UserRole.ADMIN || request.getDepartmentId() == null) {
            return currentUser.getDepartment();
        }

        return departmentRepository.findById(request.getDepartmentId())
                .orElseThrow(() -> new BusinessException("department not found"));
    }

    private boolean canView(AppUser currentUser, Reimbursement reimbursement) {
        if (currentUser.getRole() == UserRole.ADMIN
                || currentUser.getRole() == UserRole.FINANCE
                || currentUser.getRole() == UserRole.EXECUTIVE) {
            return true;
        }

        if (currentUser.getRole() == UserRole.EMPLOYEE) {
            return reimbursement.getApplicant() != null
                    && reimbursement.getApplicant().getId().equals(currentUser.getId());
        }

        if (currentUser.getRole() == UserRole.DEPARTMENT_MANAGER) {
            return sameDepartment(currentUser, reimbursement);
        }

        if (currentUser.getRole() == UserRole.CASHIER) {
            return reimbursement.getStatus() == ReimbursementStatus.EXECUTIVE_APPROVED
                    || reimbursement.getStatus() == ReimbursementStatus.PAID
                    || reimbursement.getStatus() == ReimbursementStatus.COMPLETED
                    || reimbursement.getStatus() == ReimbursementStatus.APPROVED;
        }

        return false;
    }

    private boolean canAccessReimbursements(AppUser currentUser) {
        return currentUser.getRole() == UserRole.EMPLOYEE
                || currentUser.getRole() == UserRole.DEPARTMENT_MANAGER
                || currentUser.getRole() == UserRole.FINANCE
                || currentUser.getRole() == UserRole.EXECUTIVE
                || currentUser.getRole() == UserRole.CASHIER
                || currentUser.getRole() == UserRole.ADMIN;
    }

    private boolean isPendingForUser(AppUser currentUser, Reimbursement reimbursement) {
        if (currentUser.getRole() == UserRole.ADMIN) {
            return reimbursement.getStatus() == ReimbursementStatus.SUBMITTED
                    || reimbursement.getStatus() == ReimbursementStatus.FINANCE_INITIAL_APPROVED
                    || reimbursement.getStatus() == ReimbursementStatus.DEPARTMENT_APPROVED
                    || reimbursement.getStatus() == ReimbursementStatus.PAID;
        }

        if (currentUser.getRole() == UserRole.DEPARTMENT_MANAGER) {
            return reimbursement.getStatus() == ReimbursementStatus.FINANCE_INITIAL_APPROVED
                    && sameDepartment(currentUser, reimbursement);
        }

        if (currentUser.getRole() == UserRole.FINANCE) {
            return reimbursement.getStatus() == ReimbursementStatus.SUBMITTED
                    || reimbursement.getStatus() == ReimbursementStatus.PAID;
        }

        if (currentUser.getRole() == UserRole.EXECUTIVE) {
            return reimbursement.getStatus() == ReimbursementStatus.DEPARTMENT_APPROVED;
        }

        return false;
    }

    private void assertCanView(AppUser currentUser, Reimbursement reimbursement) {
        if (!canView(currentUser, reimbursement)) {
            throw new ForbiddenException("no permission to view this reimbursement");
        }
    }

    private void assertCanManage(AppUser currentUser, Reimbursement reimbursement) {
        if (currentUser.getRole() == UserRole.ADMIN || currentUser.getRole() == UserRole.FINANCE) {
            return;
        }

        if (currentUser.getRole() == UserRole.EMPLOYEE
                && reimbursement.getApplicant() != null
                && reimbursement.getApplicant().getId().equals(currentUser.getId())) {
            return;
        }

        throw new BusinessException("no permission to manage this reimbursement");
    }

    private boolean sameDepartment(AppUser currentUser, Reimbursement reimbursement) {
        return currentUser.getDepartment() != null
                && reimbursement.getDepartment() != null
                && currentUser.getDepartment().getId().equals(reimbursement.getDepartment().getId());
    }

    private boolean canViewBudget(AppUser currentUser, Budget budget) {
        if (currentUser.getRole() == UserRole.ADMIN
                || currentUser.getRole() == UserRole.FINANCE
                || currentUser.getRole() == UserRole.EXECUTIVE) {
            return true;
        }
        return currentUser.getRole() == UserRole.DEPARTMENT_MANAGER
                && currentUser.getDepartment() != null
                && budget.getDepartment() != null
                && currentUser.getDepartment().getId().equals(budget.getDepartment().getId());
    }

    private void writeText(Row row, int column, String value) {
        row.createCell(column).setCellValue(value == null ? "" : value);
    }

    private void writeNumber(Row row, int column, Long value) {
        if (value != null) {
            row.createCell(column).setCellValue(value);
        }
    }

    private void writeMoney(Row row, int column, BigDecimal value, CellStyle style) {
        Cell cell = row.createCell(column);
        if (value != null) {
            cell.setCellValue(value.doubleValue());
        }
        cell.setCellStyle(style);
    }

    private void writeDate(Row row, int column, LocalDate value, CellStyle style) {
        Cell cell = row.createCell(column);
        if (value != null) {
            cell.setCellValue(value);
        }
        cell.setCellStyle(style);
    }

    private void writeDateTime(Row row, int column, LocalDateTime value, CellStyle style) {
        Cell cell = row.createCell(column);
        if (value != null) {
            cell.setCellValue(value);
        }
        cell.setCellStyle(style);
    }

    private String statusLabel(ReimbursementStatus status) {
        return switch (status) {
            case DRAFT -> "草稿";
            case SUBMITTED -> "财务初审中";
            case FINANCE_INITIAL_APPROVED -> "部门负责人审批中";
            case DEPARTMENT_APPROVED -> "执行院长审批中";
            case EXECUTIVE_APPROVED -> "待出纳付款";
            case PAID -> "财务复核中";
            case COMPLETED -> "已完成";
            case FINANCE_APPROVED -> "财务已审批（历史）";
            case APPROVED -> "已通过（历史）";
            case REJECTED -> "已驳回";
        };
    }

    private void fillInvoiceResult(InvoiceOcrResult result, InvoiceOcrRequest request) {
        result.setInvoiceCode(normalize(request.getInvoiceCode()));
        result.setInvoiceNumber(normalize(request.getInvoiceNumber()));
        result.setInvoiceDate(request.getInvoiceDate());
        result.setAmount(request.getAmount());
        result.setTaxAmount(request.getTaxAmount());
        result.setSellerName(normalize(request.getSellerName()));
        result.setBuyerName(normalize(request.getBuyerName()));
    }

    private void verifyInvoiceAmount(InvoiceOcrResult result, Reimbursement reimbursement) {
        BigDecimal invoiceAmount = result.getAmount();
        BigDecimal reimbursementAmount = reimbursement.getAmount();

        if (invoiceAmount == null) {
            result.setAmountMatched(false);
            result.setAmountDifference(null);
            result.setVerificationMessage("未识别到发票金额，请人工核对");
            return;
        }

        BigDecimal difference = invoiceAmount.subtract(reimbursementAmount).abs();
        result.setAmountDifference(difference);
        if (difference.compareTo(BigDecimal.ZERO) == 0) {
            result.setAmountMatched(true);
            result.setVerificationMessage("发票金额与报销金额一致");
            return;
        }

        result.setAmountMatched(false);
        result.setVerificationMessage("发票金额与报销金额不一致，请人工核对");
    }

    private InvoiceOcrResponse invoiceOcrResponseWithVerification(
            InvoiceOcrResult result,
            Reimbursement reimbursement
    ) {
        verifyInvoiceAmount(result, reimbursement);
        return InvoiceOcrResponse.fromEntity(result);
    }

    private InvoiceOcrRequest buildSimulatedInvoice(Reimbursement reimbursement) {
        InvoiceOcrRequest request = new InvoiceOcrRequest();
        int seed = Math.abs((int) (reimbursement.getId() * 37 + reimbursement.getAmount().intValue()));
        request.setInvoiceCode("SIM" + Year.now().getValue());
        request.setInvoiceNumber(String.format("%08d", seed % 100000000));
        request.setInvoiceDate(reimbursement.getExpenseDate());
        request.setAmount(reimbursement.getAmount());
        request.setTaxAmount(reimbursement.getAmount()
                .multiply(new BigDecimal("0.06"))
                .setScale(2, java.math.RoundingMode.HALF_UP));
        request.setSellerName(switch (reimbursement.getExpenseType()) {
            case "交通费" -> "城市交通服务有限公司";
            case "住宿费" -> "温州商务酒店";
            case "餐饮费" -> "园区餐饮服务中心";
            default -> "模拟发票供应商";
        });
        request.setBuyerName("温州市图灵人工智能高等研究院");
        return request;
    }

    private boolean isImageAttachment(Attachment attachment) {
        return attachment.getFileType() != null && attachment.getFileType().startsWith("image/");
    }

    private Path resolveAttachmentPath(Attachment attachment) {
        String fileUrl = attachment.getFileUrl();
        String relativePath = fileUrl.startsWith("/uploads/")
                ? fileUrl.substring("/uploads/".length())
                : fileUrl.substring(fileUrl.lastIndexOf('/') + 1);
        Path uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
        Path resolved = uploadRoot.resolve(relativePath).normalize();
        if (!resolved.startsWith(uploadRoot)) {
            throw new BusinessException("invalid attachment path");
        }
        if (!Files.exists(resolved) && !relativePath.contains("/")) {
            return uploadRoot.resolve("invoices").resolve(relativePath).normalize();
        }
        return resolved;
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private ReimbursementResponse handleDepartmentApproval(
            AppUser currentUser,
            Reimbursement reimbursement,
            ApprovalRequest request
    ) {
        if (reimbursement.getStatus() != ReimbursementStatus.FINANCE_INITIAL_APPROVED) {
            throw new BusinessException("department approval requires finance initial review");
        }
        if (currentUser.getRole() != UserRole.ADMIN && !sameDepartment(currentUser, reimbursement)) {
            throw new BusinessException("department manager can only approve own department reimbursements");
        }

        if (request.getAction() == ApprovalAction.APPROVE) {
            reimbursement.setStatus(ReimbursementStatus.DEPARTMENT_APPROVED);
        } else {
            reimbursement.setStatus(ReimbursementStatus.DRAFT);
            reimbursement.setSubmittedAt(null);
        }

        saveApprovalRecord(reimbursement, currentUser, "DEPARTMENT", request);
        Reimbursement saved = reimbursementRepository.save(reimbursement);
        operationLogService.record(
                currentUser,
                "审批管理",
                "部门审批",
                saved.getId(),
                saved.getTitle(),
                approvalDetail(request)
        );
        return ReimbursementResponse.fromEntity(saved);
    }

    private ReimbursementResponse handleFinanceApproval(
            AppUser currentUser,
            Reimbursement reimbursement,
            ApprovalRequest request
    ) {
        boolean initialReview = reimbursement.getStatus() == ReimbursementStatus.SUBMITTED;
        boolean finalReview = reimbursement.getStatus() == ReimbursementStatus.PAID;
        if (!initialReview && !finalReview) {
            throw new BusinessException("finance can only perform initial review or final review");
        }

        String node = initialReview ? "FINANCE_INITIAL" : "FINANCE_RECHECK";
        String actionName = initialReview ? "财务初审" : "财务复核";
        if (request.getAction() == ApprovalAction.APPROVE) {
            if (initialReview) {
                validateSubmissionMaterials(reimbursement);
                reimbursement.setStatus(ReimbursementStatus.FINANCE_INITIAL_APPROVED);
            } else {
                deductBudget(reimbursement);
                reimbursement.setStatus(ReimbursementStatus.COMPLETED);
            }
        } else {
            reimbursement.setStatus(ReimbursementStatus.DRAFT);
            reimbursement.setSubmittedAt(null);
        }

        saveApprovalRecord(reimbursement, currentUser, node, request);
        Reimbursement saved = reimbursementRepository.save(reimbursement);
        operationLogService.record(
                currentUser,
                "审批管理",
                actionName,
                saved.getId(),
                saved.getTitle(),
                approvalDetail(request)
        );
        return ReimbursementResponse.fromEntity(saved);
    }

    private ReimbursementResponse handleExecutiveApproval(
            AppUser currentUser,
            Reimbursement reimbursement,
            ApprovalRequest request
    ) {
        if (reimbursement.getStatus() != ReimbursementStatus.DEPARTMENT_APPROVED) {
            throw new BusinessException("executive approval requires department approval");
        }
        reimbursement.setStatus(request.getAction() == ApprovalAction.APPROVE
                ? ReimbursementStatus.EXECUTIVE_APPROVED
                : ReimbursementStatus.DRAFT);
        if (request.getAction() == ApprovalAction.REJECT) {
            reimbursement.setSubmittedAt(null);
        }
        saveApprovalRecord(reimbursement, currentUser, "EXECUTIVE", request);
        Reimbursement saved = reimbursementRepository.save(reimbursement);
        operationLogService.record(
                currentUser, "审批管理", "执行院长审批", saved.getId(), saved.getTitle(), approvalDetail(request)
        );
        return ReimbursementResponse.fromEntity(saved);
    }

    private void deductBudget(Reimbursement reimbursement) {
        if (reimbursement.getDepartment() == null) {
            throw new BusinessException("reimbursement department is missing");
        }

        Budget budget = budgetRepository
                .findByDepartmentIdAndBudgetYear(reimbursement.getDepartment().getId(), Year.now().getValue())
                .orElseThrow(() -> new BusinessException("department budget is not configured"));

        BigDecimal amount = reimbursement.getAmount();
        if (budget.getRemainingAmount().compareTo(amount) < 0) {
            throw new BusinessException("budget is not enough for this reimbursement");
        }

        budget.setUsedAmount(budget.getUsedAmount().add(amount));
        budget.setRemainingAmount(budget.getRemainingAmount().subtract(amount));
        budgetRepository.save(budget);
    }

    private void saveApprovalRecord(
            Reimbursement reimbursement,
            AppUser approver,
            String node,
            ApprovalRequest request
    ) {
        ApprovalRecord record = new ApprovalRecord();
        record.setReimbursement(reimbursement);
        record.setApprover(approver);
        record.setApprovalNode(node);
        record.setAction(request.getAction());
        record.setComment(StringUtils.hasText(request.getComment()) ? request.getComment().trim() : null);
        approvalRecordRepository.save(record);
    }

    private void saveApprovalRecord(
            Reimbursement reimbursement,
            AppUser approver,
            String node,
            ApprovalAction action,
            String comment
    ) {
        ApprovalRecord record = new ApprovalRecord();
        record.setReimbursement(reimbursement);
        record.setApprover(approver);
        record.setApprovalNode(node);
        record.setAction(action);
        record.setComment(trimToNull(comment));
        approvalRecordRepository.save(record);
    }

    private void requireRejectionReason(ApprovalRequest request) {
        if (request.getAction() == ApprovalAction.REJECT && !StringUtils.hasText(request.getComment())) {
            throw new BusinessException("rejection reason is required");
        }
    }

    private ApprovalRecord latestRecord(List<ApprovalRecord> records, String node) {
        return records.stream()
                .filter(record -> Objects.equals(record.getApprovalNode(), node))
                .reduce((first, second) -> second)
                .orElse(null);
    }

    private ReimbursementTimelineNodeResponse creationNode(Reimbursement reimbursement) {
        AppUser applicant = reimbursement.getApplicant();
        return timelineNode(
                "创建报销单",
                "COMPLETED",
                applicant,
                "创建报销申请：" + reimbursement.getTitle(),
                reimbursement.getCreatedAt(),
                "CREATE"
        );
    }

    private ReimbursementTimelineNodeResponse submitNode(Reimbursement reimbursement) {
        boolean submitted = reimbursement.getSubmittedAt() != null
                || reimbursement.getStatus() != ReimbursementStatus.DRAFT;
        return timelineNode(
                "提交报销单",
                submitted ? "COMPLETED" : "IN_PROGRESS",
                submitted ? reimbursement.getApplicant() : null,
                submitted ? "提交进入部门负责人审批" : "等待申请人提交",
                reimbursement.getSubmittedAt(),
                "SUBMIT"
        );
    }

    private ReimbursementTimelineNodeResponse departmentApprovalNode(
            Reimbursement reimbursement,
            ApprovalRecord record
    ) {
        if (record != null) {
            return timelineNode(
                    "部门负责人审批",
                    record.getAction() == ApprovalAction.APPROVE ? "COMPLETED" : "REJECTED",
                    record.getApprover(),
                    approvalComment(record),
                    record.getCreatedAt(),
                    "DEPARTMENT_APPROVAL"
            );
        }

        String status = reimbursement.getStatus() == ReimbursementStatus.SUBMITTED ? "IN_PROGRESS" : "NOT_STARTED";
        return timelineNode("部门负责人审批", status, null, "等待部门负责人审批", null, "DEPARTMENT_APPROVAL");
    }

    private ReimbursementTimelineNodeResponse financeApprovalNode(
            Reimbursement reimbursement,
            ApprovalRecord record
    ) {
        if (record != null) {
            return timelineNode(
                    "财务审批",
                    record.getAction() == ApprovalAction.APPROVE ? "COMPLETED" : "REJECTED",
                    record.getApprover(),
                    approvalComment(record),
                    record.getCreatedAt(),
                    "FINANCE_APPROVAL"
            );
        }

        String status = reimbursement.getStatus() == ReimbursementStatus.DEPARTMENT_APPROVED ? "IN_PROGRESS" : "NOT_STARTED";
        return timelineNode("财务审批", status, null, "等待财务审批", null, "FINANCE_APPROVAL");
    }

    private ReimbursementTimelineNodeResponse finalResultNode(
            Reimbursement reimbursement,
            ApprovalRecord departmentRecord,
            ApprovalRecord financeRecord
    ) {
        if (reimbursement.getStatus() == ReimbursementStatus.APPROVED) {
            return timelineNode(
                    "最终结果",
                    "COMPLETED",
                    financeRecord == null ? null : financeRecord.getApprover(),
                    "报销单已通过，预算已扣减",
                    financeRecord == null ? reimbursement.getUpdatedAt() : financeRecord.getCreatedAt(),
                    "FINAL_RESULT"
            );
        }

        if (reimbursement.getStatus() == ReimbursementStatus.REJECTED) {
            ApprovalRecord rejectRecord = financeRecord != null && financeRecord.getAction() == ApprovalAction.REJECT
                    ? financeRecord
                    : departmentRecord;
            return timelineNode(
                    "最终结果",
                    "REJECTED",
                    rejectRecord == null ? null : rejectRecord.getApprover(),
                    rejectRecord == null ? "报销单已驳回" : approvalComment(rejectRecord),
                    rejectRecord == null ? reimbursement.getUpdatedAt() : rejectRecord.getCreatedAt(),
                    "FINAL_RESULT"
            );
        }

        String status = reimbursement.getStatus() == ReimbursementStatus.DRAFT ? "NOT_STARTED" : "IN_PROGRESS";
        return timelineNode("最终结果", status, null, "流程尚未结束", null, "FINAL_RESULT");
    }

    private ReimbursementTimelineNodeResponse processNode(
            String title,
            String node,
            ReimbursementStatus pendingStatus,
            ApprovalRecord record,
            Reimbursement reimbursement
    ) {
        if (record != null) {
            return timelineNode(
                    title,
                    record.getAction() == ApprovalAction.APPROVE ? "COMPLETED" : "REJECTED",
                    record.getApprover(),
                    approvalComment(record),
                    record.getCreatedAt(),
                    node
            );
        }
        int currentRank = workflowRank(reimbursement.getStatus());
        int pendingRank = workflowRank(pendingStatus);
        if (currentRank > pendingRank) {
            return timelineNode(title, "COMPLETED", null, "历史报销单已通过该环节", null, node);
        }
        if (currentRank == pendingRank) {
            return timelineNode(title, "IN_PROGRESS", null, "等待" + title, null, node);
        }
        return timelineNode(title, "NOT_STARTED", null, "尚未进入该环节", null, node);
    }

    private ReimbursementTimelineNodeResponse researchInstituteFinalNode(
            Reimbursement reimbursement,
            ApprovalRecord financeRecheck
    ) {
        if (reimbursement.getStatus() == ReimbursementStatus.COMPLETED
                || reimbursement.getStatus() == ReimbursementStatus.APPROVED) {
            return timelineNode(
                    "流程完成",
                    "COMPLETED",
                    financeRecheck == null ? null : financeRecheck.getApprover(),
                    "财务复核通过，部门预算已扣减，报销流程完成",
                    financeRecheck == null ? reimbursement.getUpdatedAt() : financeRecheck.getCreatedAt(),
                    "FINAL_RESULT"
            );
        }
        if (reimbursement.getStatus() == ReimbursementStatus.REJECTED) {
            return timelineNode("流程完成", "REJECTED", null, "历史报销单已驳回", reimbursement.getUpdatedAt(), "FINAL_RESULT");
        }
        return timelineNode("流程完成", "NOT_STARTED", null, "财务复核通过后完成", null, "FINAL_RESULT");
    }

    private int workflowRank(ReimbursementStatus status) {
        return switch (status) {
            case DRAFT, REJECTED -> 0;
            case SUBMITTED -> 1;
            case FINANCE_INITIAL_APPROVED -> 2;
            case DEPARTMENT_APPROVED -> 3;
            case EXECUTIVE_APPROVED -> 4;
            case PAID, FINANCE_APPROVED -> 5;
            case COMPLETED, APPROVED -> 6;
        };
    }

    private ReimbursementTimelineNodeResponse timelineNode(
            String title,
            String status,
            AppUser operator,
            String comment,
            LocalDateTime time,
            String nodeType
    ) {
        return new ReimbursementTimelineNodeResponse(
                title,
                status,
                operator == null ? null : operator.getRealName(),
                operator == null ? null : operator.getRole(),
                comment,
                time,
                nodeType
        );
    }

    private String approvalComment(ApprovalRecord record) {
        String action = record.getAction() == ApprovalAction.APPROVE ? "审批通过" : "审批驳回";
        if (!StringUtils.hasText(record.getComment())) {
            return action;
        }
        return action + "：" + record.getComment();
    }

    private String reimbursementDetail(Reimbursement reimbursement) {
        return "金额：" + reimbursement.getAmount()
                + "，费用类型：" + reimbursement.getExpenseType()
                + "，状态：" + statusLabel(reimbursement.getStatus());
    }

    private String approvalDetail(ApprovalRequest request) {
        String action = request.getAction() == ApprovalAction.APPROVE ? "通过" : "驳回";
        String comment = StringUtils.hasText(request.getComment()) ? "，意见：" + request.getComment().trim() : "";
        return "审批结果：" + action + comment;
    }

    private String nullSafe(Object value) {
        return value == null ? "-" : value.toString();
    }
}
