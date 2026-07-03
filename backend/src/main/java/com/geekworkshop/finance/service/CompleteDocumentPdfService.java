package com.geekworkshop.finance.service;

import com.geekworkshop.finance.dto.*;
import com.geekworkshop.finance.entity.*;
import com.geekworkshop.finance.exception.BusinessException;
import com.geekworkshop.finance.exception.ForbiddenException;
import com.geekworkshop.finance.util.ChineseAmountFormatter;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class CompleteDocumentPdfService {
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final ReimbursementService reimbursementService;
    private final PurchaseApplicationService purchaseService;
    private final LaborApplicationService laborService;
    private final AdvanceApplicationService advanceService;
    private final OperationLogService operationLogService;

    public CompleteDocumentPdfService(
            ReimbursementService reimbursementService,
            PurchaseApplicationService purchaseService,
            LaborApplicationService laborService,
            AdvanceApplicationService advanceService,
            OperationLogService operationLogService
    ) {
        this.reimbursementService = reimbursementService;
        this.purchaseService = purchaseService;
        this.laborService = laborService;
        this.advanceService = advanceService;
        this.operationLogService = operationLogService;
    }

    @Transactional
    public PdfFile generate(AppUser user, DocumentModule module, Long id) {
        PdfModel model = switch (module) {
            case REIMBURSEMENT -> reimbursement(user, id);
            case PURCHASE -> purchase(user, id);
            case LABOR -> labor(user, id);
            case ADVANCE -> advance(user, id);
        };
        authorize(user, model);
        byte[] bytes = render(model);
        operationLogService.record(user, "单据归档", "下载完整单据", id, model.number(),
                "业务类型：" + model.businessType() + "，PDF归档下载");
        return new PdfFile(bytes, model.number() + "-完整单据.pdf");
    }

    private PdfModel reimbursement(AppUser user, Long id) {
        ReimbursementDetailResponse detail = reimbursementService.detail(user, id);
        ReimbursementResponse value = detail.getReimbursement();
        List<ReimbursementTimelineNodeResponse> timeline = reimbursementService.timeline(user, id);
        List<List<String>> approvals = timeline.stream()
                .filter(node -> node.getOperatorName() != null)
                .map(node -> List.of(
                        text(node.getTitle()), text(node.getOperatorName()), text(node.getOperatorRole()),
                        text(node.getComment()), text(node.getTime())
                )).toList();
        List<List<String>> attachments = detail.getAttachments().stream()
                .map(item -> List.of(text(item.getFileName()), text(item.getAttachmentType()))).toList();
        Map<String, String> fields = linked(
                "标题", value.getTitle(), "费用类型", value.getExpenseType(),
                "申请人", value.getApplicantName(), "部门", value.getDepartmentName(),
                "联系电话", value.getApplicantPhone(), "收款人", value.getPayeeName(),
                "银行账号", value.getBankAccount(), "开户行", value.getBankName(),
                "报销金额", money(value.getAmount()), "金额大写", ChineseAmountFormatter.format(value.getAmount()),
                "发生日期", value.getExpenseDate(), "预算编号", value.getBudgetNumber(),
                "报销事由", value.getReimbursementReason(), "补充说明", value.getDescription(),
                "付款日期", value.getPaymentDate(), "付款金额", value.getPaymentTotal(),
                "付款凭证号", value.getPaymentVoucherNumber()
        );
        return new PdfModel("报销单", value.getApprovalNumber(), value.getApplicantId(), value.getDepartmentId(),
                value.getDepartmentName(), fields, List.of(), approvals, attachments);
    }

    private PdfModel purchase(AppUser user, Long id) {
        PurchaseApplicationResponse value = purchaseService.detail(user, id);
        Map<String, String> fields = linked(
                "申请人", value.applicantName(), "部门", value.departmentName(),
                "联系电话", value.applicantPhone(), "预算编号", value.budgetNumber(),
                "采购方式", value.purchaseMethod(), "总金额", money(value.amount()),
                "金额大写", ChineseAmountFormatter.format(value.amount()), "使用地点", value.useLocation(),
                "购置理由", value.purchaseReason(), "验收单号", value.assetAcceptanceNumber()
        );
        List<List<String>> details = value.items().stream().map(item -> List.of(
                text(item.itemName()), text(item.specification()), text(item.manufacturer()),
                money(item.unitPrice()), text(item.quantity()), money(item.totalPrice())
        )).toList();
        List<List<String>> approvals = value.approvalRecords().stream().map(item -> List.of(
                text(item.approvalNode()), text(item.approverName()), text(item.approverRole()),
                text(item.comment()), text(item.createdAt())
        )).toList();
        List<List<String>> attachments = value.attachments().stream()
                .map(item -> List.of(text(item.fileName()), text(item.attachmentType()))).toList();
        return new PdfModel("申购单", value.applicationNumber(), value.applicantId(), value.departmentId(),
                value.departmentName(), fields, details, approvals, attachments);
    }

    private PdfModel labor(AppUser user, Long id) {
        LaborApplicationResponse value = laborService.detail(user, id);
        Map<String, String> fields = linked(
                "发放事项", value.title(), "类别", value.category(),
                "申请人", value.applicantName(), "部门", value.departmentName(),
                "预算编号", value.budgetNumber(), "总金额", money(value.totalAmount()),
                "金额大写", value.amountInWords(), "说明", value.description(),
                "付款日期", value.paymentDate(), "付款金额", value.paymentAmount(),
                "付款凭证号", value.paymentVoucherNumber()
        );
        List<List<String>> details = value.recipients().stream().map(item -> List.of(
                text(item.name()), text(item.phone()), text(item.organization()), text(item.position()),
                text(item.serviceContent()), money(item.netAmount()), text(item.bankAccount()), text(item.bankName())
        )).toList();
        List<List<String>> approvals = timelineRows(value.timeline());
        List<List<String>> attachments = value.attachments().stream()
                .map(item -> List.of(text(item.fileName()), text(item.attachmentType()))).toList();
        return new PdfModel("劳务/酬金发放单", value.applicationNumber(), value.applicantId(), value.departmentId(),
                value.departmentName(), fields, details, approvals, attachments);
    }

    private PdfModel advance(AppUser user, Long id) {
        AdvanceApplicationResponse value = advanceService.detail(user, id);
        Map<String, String> fields = linked(
                "业务类型", value.type(), "申请人", value.applicantName(),
                "部门", value.departmentName(), "申请金额", money(value.amount()),
                "金额大写", ChineseAmountFormatter.format(value.amount()), "支付方式", value.paymentMethod(),
                "收款人", value.payeeName(), "银行账号", value.bankAccount(),
                "开户行", value.bankName(), "申请理由", value.reason(),
                "合作方", value.partnerName(), "预计结算/还款", value.expectedSettlementDate() != null
                        ? value.expectedSettlementDate() : value.expectedRepaymentDate(),
                "付款日期", value.paymentDate(), "付款金额", value.paymentAmount(),
                "付款凭证号", value.paymentVoucherNumber(), "已冲账", value.offsetAmount(),
                "剩余待冲", value.remainingAmount()
        );
        List<List<String>> details = value.offsetRecords().stream().map(item -> List.of(
                text(item.createdAt()), text(item.operatorName()), money(item.amount()), text(item.comment())
        )).toList();
        List<List<String>> approvals = timelineRows(value.timeline());
        List<List<String>> attachments = value.attachments().stream()
                .map(item -> List.of(text(item.fileName()), text(item.attachmentType()))).toList();
        return new PdfModel("暂借款/预付款单", value.applicationNumber(), value.applicantId(), null,
                value.departmentName(), fields, details, approvals, attachments);
    }

    private List<List<String>> timelineRows(List<? extends Object> timeline) {
        return timeline.stream().map(item -> {
            if (item instanceof LaborTimelineResponse node) return List.of(
                    text(node.title()), text(node.operatorName()), text(node.operatorRole()),
                    text(node.comment()), text(node.time()));
            AdvanceTimelineResponse node = (AdvanceTimelineResponse) item;
            return List.of(text(node.title()), text(node.operatorName()), text(node.operatorRole()),
                    text(node.comment()), text(node.time()));
        }).toList();
    }

    private void authorize(AppUser user, PdfModel model) {
        if (EnumSet.of(UserRole.FINANCE, UserRole.EXECUTIVE, UserRole.COMMITTEE, UserRole.ADMIN)
                .contains(user.getRole())) return;
        if (user.getRole() == UserRole.DEPARTMENT_MANAGER && user.getDepartment() != null
                && (Objects.equals(user.getDepartment().getId(), model.departmentId())
                || Objects.equals(user.getDepartment().getName(), model.departmentName()))) return;
        if (user.getRole() == UserRole.EMPLOYEE && Objects.equals(user.getId(), model.applicantId())) return;
        throw new ForbiddenException("无权下载该完整单据");
    }

    private byte[] render(PdfModel model) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 42, 42, 58, 48);
            PdfWriter writer = PdfWriter.getInstance(document, output);
            BaseFont base = BaseFont.createFont("STSong-Light", "UniGB-UCS2-H", BaseFont.NOT_EMBEDDED);
            writer.setPageEvent(new FooterEvent(base));
            document.addTitle("内部凭证财务系统 - " + model.businessType() + " - " + model.number());
            document.open();
            Font title = new Font(base, 18, Font.BOLD, new Color(20, 45, 75));
            Font heading = new Font(base, 12, Font.BOLD, new Color(20, 45, 75));
            Font normal = new Font(base, 9, Font.NORMAL, Color.DARK_GRAY);
            Paragraph system = new Paragraph("内部凭证财务系统", title);
            system.setAlignment(Element.ALIGN_CENTER);
            document.add(system);
            Paragraph type = new Paragraph(model.businessType() + "  " + model.number(), new Font(base, 13, Font.BOLD));
            type.setAlignment(Element.ALIGN_CENTER);
            type.setSpacingAfter(14);
            document.add(type);
            document.add(section("基本信息", heading));
            document.add(keyValueTable(model.fields(), normal));
            if (!model.details().isEmpty()) {
                document.add(section("明细数据", heading));
                document.add(dataTable(model.details(), normal));
            }
            document.add(section("审批流程及记录", heading));
            document.add(model.approvals().isEmpty()
                    ? new Paragraph("暂无审批记录", normal)
                    : dataTable(model.approvals(), normal));
            document.add(section("附件清单", heading));
            document.add(model.attachments().isEmpty()
                    ? new Paragraph("暂无附件", normal)
                    : dataTable(model.attachments(), normal));
            document.close();
            return output.toByteArray();
        } catch (Exception exception) {
            throw new BusinessException("完整单据 PDF 生成失败：" + exception.getMessage());
        }
    }

    private Paragraph section(String text, Font font) {
        Paragraph paragraph = new Paragraph(text, font);
        paragraph.setSpacingBefore(12);
        paragraph.setSpacingAfter(6);
        return paragraph;
    }

    private PdfPTable keyValueTable(Map<String, String> values, Font font) {
        PdfPTable table = new PdfPTable(new float[]{1.1f, 2.2f, 1.1f, 2.2f});
        table.setWidthPercentage(100);
        List<Map.Entry<String, String>> entries = new ArrayList<>(values.entrySet());
        for (int i = 0; i < entries.size(); i += 2) {
            addCell(table, entries.get(i).getKey(), font, true);
            addCell(table, entries.get(i).getValue(), font, false);
            if (i + 1 < entries.size()) {
                addCell(table, entries.get(i + 1).getKey(), font, true);
                addCell(table, entries.get(i + 1).getValue(), font, false);
            } else {
                addCell(table, "", font, true);
                addCell(table, "", font, false);
            }
        }
        return table;
    }

    private PdfPTable dataTable(List<List<String>> rows, Font font) {
        int columns = rows.stream().mapToInt(List::size).max().orElse(1);
        PdfPTable table = new PdfPTable(columns);
        table.setWidthPercentage(100);
        for (List<String> row : rows) {
            for (int i = 0; i < columns; i++) addCell(table, i < row.size() ? row.get(i) : "", font, false);
        }
        return table;
    }

    private void addCell(PdfPTable table, String value, Font font, boolean label) {
        PdfPCell cell = new PdfPCell(new Phrase(text(value), font));
        cell.setPadding(6);
        cell.setBorderColor(new Color(210, 218, 228));
        if (label) cell.setBackgroundColor(new Color(241, 245, 249));
        table.addCell(cell);
    }

    private Map<String, String> linked(Object... values) {
        Map<String, String> result = new LinkedHashMap<>();
        for (int i = 0; i < values.length; i += 2) result.put(text(values[i]), text(values[i + 1]));
        return result;
    }

    private String money(BigDecimal value) { return value == null ? "-" : "¥ " + value.setScale(2); }
    private String text(Object value) { return value == null ? "-" : String.valueOf(value); }

    private record PdfModel(
            String businessType, String number, Long applicantId, Long departmentId, String departmentName,
            Map<String, String> fields, List<List<String>> details,
            List<List<String>> approvals, List<List<String>> attachments
    ) {}
    public record PdfFile(byte[] bytes, String fileName) {}

    private static class FooterEvent extends PdfPageEventHelper {
        private final BaseFont font;
        private FooterEvent(BaseFont font) { this.font = font; }
        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            String footer = "生成时间：" + LocalDateTime.now().format(DATE_TIME)
                    + "    第 " + writer.getPageNumber() + " 页";
            ColumnText.showTextAligned(writer.getDirectContent(), Element.ALIGN_CENTER,
                    new Phrase(footer, new Font(font, 8, Font.NORMAL, Color.GRAY)),
                    (document.left() + document.right()) / 2, 24, 0);
        }
    }
}
