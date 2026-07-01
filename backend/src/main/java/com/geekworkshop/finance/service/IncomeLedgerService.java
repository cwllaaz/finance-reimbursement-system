package com.geekworkshop.finance.service;

import com.geekworkshop.finance.dto.*;
import com.geekworkshop.finance.entity.*;
import com.geekworkshop.finance.exception.BusinessException;
import com.geekworkshop.finance.exception.ForbiddenException;
import com.geekworkshop.finance.repository.*;
import org.apache.poi.ss.usermodel.*;
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
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
public class IncomeLedgerService {
    private final IncomeRecordRepository incomeRecordRepository;
    private final IncomeAttachmentRepository incomeAttachmentRepository;
    private final DepartmentRepository departmentRepository;
    private final ReimbursementRepository reimbursementRepository;
    private final LaborApplicationRepository laborApplicationRepository;
    private final PurchaseApplicationRepository purchaseApplicationRepository;
    private final AdvanceApplicationRepository advanceApplicationRepository;
    private final OperationLogService operationLogService;

    @Value("${app.upload-dir:uploads}")
    private String uploadDir;

    public IncomeLedgerService(
            IncomeRecordRepository incomeRecordRepository,
            IncomeAttachmentRepository incomeAttachmentRepository,
            DepartmentRepository departmentRepository,
            ReimbursementRepository reimbursementRepository,
            LaborApplicationRepository laborApplicationRepository,
            PurchaseApplicationRepository purchaseApplicationRepository,
            AdvanceApplicationRepository advanceApplicationRepository,
            OperationLogService operationLogService
    ) {
        this.incomeRecordRepository = incomeRecordRepository;
        this.incomeAttachmentRepository = incomeAttachmentRepository;
        this.departmentRepository = departmentRepository;
        this.reimbursementRepository = reimbursementRepository;
        this.laborApplicationRepository = laborApplicationRepository;
        this.purchaseApplicationRepository = purchaseApplicationRepository;
        this.advanceApplicationRepository = advanceApplicationRepository;
        this.operationLogService = operationLogService;
    }

    @Transactional(readOnly = true)
    public List<IncomeRecordResponse> listIncomes(AppUser user, String keyword, LocalDate startDate, LocalDate endDate) {
        requireLedgerAccess(user);
        String normalized = StringUtils.hasText(keyword) ? keyword.trim().toLowerCase() : null;
        return incomeRecordRepository.findAllDetails().stream()
                .filter(record -> canViewDepartment(user, record.getDepartment()))
                .filter(record -> inDateRange(record.getReceiptDate(), startDate, endDate))
                .filter(record -> normalized == null
                        || contains(record.getIncomeNumber(), normalized)
                        || contains(record.getPayerName(), normalized)
                        || contains(record.getIncomeCategory(), normalized)
                        || contains(record.getVoucherNumber(), normalized))
                .map(record -> toIncomeResponse(record, false))
                .toList();
    }

    @Transactional(readOnly = true)
    public IncomeRecordResponse detail(AppUser user, Long id) {
        requireLedgerAccess(user);
        IncomeRecord record = requireIncome(id);
        if (!canViewDepartment(user, record.getDepartment())) {
            throw new ForbiddenException("no permission to view this income record");
        }
        return toIncomeResponse(record, true);
    }

    @Transactional
    public IncomeRecordResponse create(AppUser user, IncomeRecordRequest request) {
        requireIncomeManage(user);
        IncomeRecord record = new IncomeRecord();
        record.setIncomeNumber(nextIncomeNumber());
        record.setCreatedBy(user);
        applyIncomeRequest(user, record, request);
        IncomeRecord saved = incomeRecordRepository.save(record);
        operationLogService.record(user, "收入台账", "新增收入记录", saved.getId(), saved.getIncomeNumber(),
                "登记收入：" + saved.getPayerName() + "，金额：" + saved.getAmount());
        return toIncomeResponse(saved, true);
    }

    @Transactional
    public IncomeRecordResponse update(AppUser user, Long id, IncomeRecordRequest request) {
        requireIncomeManage(user);
        IncomeRecord record = requireIncome(id);
        applyIncomeRequest(user, record, request);
        IncomeRecord saved = incomeRecordRepository.save(record);
        operationLogService.record(user, "收入台账", "编辑收入记录", saved.getId(), saved.getIncomeNumber(),
                "修改收入记录：" + saved.getPayerName());
        return toIncomeResponse(saved, true);
    }

    @Transactional
    public void delete(AppUser user, Long id) {
        requireIncomeManage(user);
        IncomeRecord record = requireIncome(id);
        incomeAttachmentRepository.deleteByIncomeRecordId(id);
        incomeRecordRepository.delete(record);
        operationLogService.record(user, "收入台账", "删除收入记录", id, record.getIncomeNumber(),
                "删除收入记录：" + record.getPayerName());
    }

    @Transactional
    public IncomeAttachmentResponse uploadIncomeAttachment(AppUser user, Long id, MultipartFile file) {
        requireIncomeManage(user);
        IncomeRecord record = requireIncome(id);
        String safeName = SecureFileSupport.validate(file);
        String storedName = UUID.randomUUID() + "-" + safeName.replaceAll("[^a-zA-Z0-9._\\-\\u4e00-\\u9fa5]", "_");
        try {
            Path directory = Paths.get(uploadDir, "incomes").toAbsolutePath().normalize();
            Files.createDirectories(directory);
            Files.copy(file.getInputStream(), directory.resolve(storedName), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            throw new BusinessException("failed to save attachment");
        }

        IncomeAttachment attachment = new IncomeAttachment();
        attachment.setIncomeRecord(record);
        attachment.setFileName(safeName);
        attachment.setFileUrl("/uploads/incomes/" + storedName);
        attachment.setFileType(file.getContentType());
        attachment.setFileSize(file.getSize());
        IncomeAttachment saved = incomeAttachmentRepository.save(attachment);
        operationLogService.record(user, "收入台账", "上传收入附件", record.getId(), record.getIncomeNumber(),
                "上传发票附件：" + safeName);
        return IncomeAttachmentResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public LedgerSummaryResponse ledger(AppUser user, LocalDate startDate, LocalDate endDate, Long departmentId, String businessType) {
        List<LedgerEntryResponse> entries = buildLedger(user, startDate, endDate, departmentId, businessType);
        BigDecimal totalIncome = entries.stream().map(LedgerEntryResponse::incomeAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalExpense = entries.stream().map(LedgerEntryResponse::expenseAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new LedgerSummaryResponse(totalIncome, totalExpense, totalIncome.subtract(totalExpense), entries);
    }

    @Transactional
    public byte[] exportLedger(AppUser user, LocalDate startDate, LocalDate endDate, Long departmentId, String businessType) {
        LedgerSummaryResponse summary = ledger(user, startDate, endDate, departmentId, businessType);
        operationLogService.record(user, "财务总台账", "导出总台账", null, "单位收支总台账",
                "导出收入 " + summary.totalIncome() + "，支出 " + summary.totalExpense());

        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            XSSFSheet sheet = workbook.createSheet("单位收支总台账");
            CreationHelper helper = workbook.getCreationHelper();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(headerFont);
            CellStyle moneyStyle = workbook.createCellStyle();
            moneyStyle.setDataFormat(helper.createDataFormat().getFormat("#,##0.00"));

            String[] headers = {"日期", "方向", "业务类型", "业务编号", "部门", "经办/申请人", "摘要", "收入", "支出", "备注"};
            Row header = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIndex = 1;
            for (LedgerEntryResponse entry : summary.entries()) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(entry.businessDate() == null ? "" : entry.businessDate().toString());
                row.createCell(1).setCellValue(entry.direction());
                row.createCell(2).setCellValue(entry.businessType());
                row.createCell(3).setCellValue(entry.businessNumber());
                row.createCell(4).setCellValue(entry.departmentName() == null ? "" : entry.departmentName());
                row.createCell(5).setCellValue(entry.operatorName() == null ? "" : entry.operatorName());
                row.createCell(6).setCellValue(entry.summary() == null ? "" : entry.summary());
                setMoney(row.createCell(7), entry.incomeAmount(), moneyStyle);
                setMoney(row.createCell(8), entry.expenseAmount(), moneyStyle);
                row.createCell(9).setCellValue(entry.remark() == null ? "" : entry.remark());
            }

            Row total = sheet.createRow(rowIndex + 1);
            total.createCell(6).setCellValue("合计");
            setMoney(total.createCell(7), summary.totalIncome(), moneyStyle);
            setMoney(total.createCell(8), summary.totalExpense(), moneyStyle);
            total.createCell(9).setCellValue("结余：" + summary.balance());

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
                sheet.setColumnWidth(i, Math.min(sheet.getColumnWidth(i) + 800, 14000));
            }
            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (IOException exception) {
            throw new BusinessException("failed to export ledger excel");
        }
    }

    private List<LedgerEntryResponse> buildLedger(
            AppUser user, LocalDate startDate, LocalDate endDate, Long departmentId, String businessType
    ) {
        requireLedgerAccess(user);
        Long effectiveDepartmentId = user.getRole() == UserRole.DEPARTMENT_MANAGER
                ? Optional.ofNullable(user.getDepartment()).map(Department::getId).orElse(-1L)
                : departmentId;
        String typeFilter = StringUtils.hasText(businessType) ? businessType.trim() : null;
        List<LedgerEntryResponse> entries = new ArrayList<>();

        incomeRecordRepository.findAllDetails().stream()
                .filter(record -> matchesDepartment(record.getDepartment(), effectiveDepartmentId))
                .filter(record -> inDateRange(record.getReceiptDate(), startDate, endDate))
                .filter(record -> typeFilter == null || "收入".equals(typeFilter) || record.getIncomeCategory().equals(typeFilter))
                .map(record -> new LedgerEntryResponse(
                        record.getReceiptDate(), "收入", record.getIncomeCategory(), record.getIncomeNumber(),
                        departmentId(record.getDepartment()), departmentName(record.getDepartment()),
                        record.getCreatedBy() == null ? null : record.getCreatedBy().getRealName(),
                        record.getPayerName(), record.getAmount(), BigDecimal.ZERO, record.getRemark()))
                .forEach(entries::add);

        reimbursementRepository.findAllForExport().stream()
                .filter(item -> item.getStatus() == ReimbursementStatus.COMPLETED || item.getStatus() == ReimbursementStatus.APPROVED)
                .filter(item -> matchesDepartment(item.getDepartment(), effectiveDepartmentId))
                .filter(item -> inDateRange(coalesce(item.getPaymentDate(), item.getExpenseDate()), startDate, endDate))
                .filter(item -> typeFilter == null || "报销".equals(typeFilter))
                .map(item -> new LedgerEntryResponse(
                        coalesce(item.getPaymentDate(), item.getExpenseDate()), "支出", "报销",
                        firstText(item.getApprovalNumber(), "BX#" + item.getId()), departmentId(item.getDepartment()),
                        departmentName(item.getDepartment()), item.getApplicant() == null ? null : item.getApplicant().getRealName(),
                        item.getTitle(), BigDecimal.ZERO, firstMoney(item.getPaymentTotal(), item.getAmount()),
                        item.getExpenseType()))
                .forEach(entries::add);

        laborApplicationRepository.findAllDetails().stream()
                .filter(item -> item.getStatus() == LaborStatus.COMPLETED)
                .filter(item -> matchesDepartment(item.getDepartment(), effectiveDepartmentId))
                .filter(item -> inDateRange(coalesce(item.getPaymentDate(), item.getCreatedAt().toLocalDate()), startDate, endDate))
                .filter(item -> typeFilter == null || "劳务酬金".equals(typeFilter))
                .map(item -> new LedgerEntryResponse(
                        coalesce(item.getPaymentDate(), item.getCreatedAt().toLocalDate()), "支出", "劳务酬金",
                        item.getApplicationNumber(), departmentId(item.getDepartment()), departmentName(item.getDepartment()),
                        item.getApplicant() == null ? null : item.getApplicant().getRealName(), item.getTitle(),
                        BigDecimal.ZERO, firstMoney(item.getPaymentAmount(), item.getTotalAmount()), item.getCategory().name()))
                .forEach(entries::add);

        purchaseApplicationRepository.findAllDetails().stream()
                .filter(item -> item.getStatus() == PurchaseStatus.COMPLETED)
                .filter(item -> matchesDepartment(item.getDepartment(), effectiveDepartmentId))
                .filter(item -> inDateRange(item.getCreatedAt().toLocalDate(), startDate, endDate))
                .filter(item -> typeFilter == null || "申购".equals(typeFilter))
                .map(item -> new LedgerEntryResponse(
                        item.getCreatedAt().toLocalDate(), "支出", "申购", item.getApplicationNumber(),
                        departmentId(item.getDepartment()), departmentName(item.getDepartment()),
                        item.getApplicant() == null ? null : item.getApplicant().getRealName(),
                        item.getPurchaseReason(), BigDecimal.ZERO, item.getAmount(), item.getPurchaseMethod()))
                .forEach(entries::add);

        advanceApplicationRepository.findAllDetails().stream()
                .filter(item -> item.getStatus() == AdvanceStatus.PAID || item.getStatus() == AdvanceStatus.COMPLETED)
                .filter(item -> matchesDepartment(item.getDepartment(), effectiveDepartmentId))
                .filter(item -> inDateRange(coalesce(item.getPaymentDate(), item.getCreatedAt().toLocalDate()), startDate, endDate))
                .filter(item -> typeFilter == null || "暂借款/预付款".equals(typeFilter))
                .map(item -> new LedgerEntryResponse(
                        coalesce(item.getPaymentDate(), item.getCreatedAt().toLocalDate()), "支出", "暂借款/预付款",
                        item.getApplicationNumber(), departmentId(item.getDepartment()), departmentName(item.getDepartment()),
                        item.getApplicant() == null ? null : item.getApplicant().getRealName(), item.getReason(),
                        BigDecimal.ZERO, firstMoney(item.getPaymentAmount(), item.getAmount()), item.getType().name()))
                .forEach(entries::add);

        entries.sort(Comparator.comparing(LedgerEntryResponse::businessDate, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(LedgerEntryResponse::businessNumber, Comparator.nullsLast(Comparator.naturalOrder())));
        return entries;
    }

    private IncomeRecord requireIncome(Long id) {
        return incomeRecordRepository.findDetailById(id)
                .orElseThrow(() -> new BusinessException("income record not found"));
    }

    private IncomeRecordResponse toIncomeResponse(IncomeRecord record, boolean full) {
        List<IncomeAttachmentResponse> attachments = full
                ? incomeAttachmentRepository.findByIncomeRecordIdOrderByCreatedAtAsc(record.getId())
                    .stream().map(IncomeAttachmentResponse::fromEntity).toList()
                : List.of();
        return IncomeRecordResponse.fromEntity(record, attachments);
    }

    private void applyIncomeRequest(AppUser user, IncomeRecord record, IncomeRecordRequest request) {
        record.setReceiptDate(request.getReceiptDate());
        record.setVoucherNumber(trimToNull(request.getVoucherNumber()));
        record.setPayerName(request.getPayerName().trim());
        record.setIncomeCategory(request.getIncomeCategory().trim());
        record.setAmount(request.getAmount());
        record.setFundingSource(trimToNull(request.getFundingSource()));
        record.setArrivalAccount(trimToNull(request.getArrivalAccount()));
        record.setInvoiceStatus(trimToNull(request.getInvoiceStatus()));
        record.setRemark(trimToNull(request.getRemark()));
        record.setDepartment(resolveDepartment(user, request.getDepartmentId()));
    }

    private Department resolveDepartment(AppUser user, Long departmentId) {
        if (departmentId == null) {
            return user.getDepartment();
        }
        return departmentRepository.findById(departmentId)
                .orElseThrow(() -> new BusinessException("department not found"));
    }

    private synchronized String nextIncomeNumber() {
        String prefix = "SR" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        int sequence = incomeRecordRepository.findTopByIncomeNumberStartingWithOrderByIncomeNumberDesc(prefix)
                .map(IncomeRecord::getIncomeNumber)
                .map(number -> Integer.parseInt(number.substring(prefix.length())) + 1)
                .orElse(1);
        return prefix + String.format("%03d", sequence);
    }

    private void requireLedgerAccess(AppUser user) {
        if (user.getRole() == UserRole.EMPLOYEE || user.getRole() == UserRole.OFFICE || user.getRole() == UserRole.CASHIER) {
            throw new ForbiddenException("no permission to access financial ledger");
        }
    }

    private void requireIncomeManage(AppUser user) {
        if (user.getRole() != UserRole.FINANCE && user.getRole() != UserRole.ADMIN) {
            throw new ForbiddenException("only finance and admin can manage income records");
        }
    }

    private boolean canViewDepartment(AppUser user, Department department) {
        if (EnumSet.of(UserRole.FINANCE, UserRole.EXECUTIVE, UserRole.ADMIN).contains(user.getRole())) return true;
        return user.getRole() == UserRole.DEPARTMENT_MANAGER && sameDepartment(user.getDepartment(), department);
    }

    private boolean matchesDepartment(Department department, Long departmentId) {
        return departmentId == null || Objects.equals(departmentId(department), departmentId);
    }

    private boolean sameDepartment(Department left, Department right) {
        return left != null && right != null && Objects.equals(left.getId(), right.getId());
    }

    private boolean inDateRange(LocalDate date, LocalDate startDate, LocalDate endDate) {
        if (date == null) return false;
        return (startDate == null || !date.isBefore(startDate)) && (endDate == null || !date.isAfter(endDate));
    }

    private boolean contains(String value, String keyword) {
        return value != null && value.toLowerCase().contains(keyword);
    }

    private Long departmentId(Department department) {
        return department == null ? null : department.getId();
    }

    private String departmentName(Department department) {
        return department == null ? null : department.getName();
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private LocalDate coalesce(LocalDate primary, LocalDate fallback) {
        return primary == null ? fallback : primary;
    }

    private BigDecimal firstMoney(BigDecimal primary, BigDecimal fallback) {
        return primary == null ? fallback : primary;
    }

    private String firstText(String primary, String fallback) {
        return StringUtils.hasText(primary) ? primary : fallback;
    }

    private void setMoney(Cell cell, BigDecimal value, CellStyle style) {
        cell.setCellValue(value == null ? 0D : value.doubleValue());
        cell.setCellStyle(style);
    }
}
