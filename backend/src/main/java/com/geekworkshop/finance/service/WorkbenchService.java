package com.geekworkshop.finance.service;

import com.geekworkshop.finance.dto.WorkbenchItemResponse;
import com.geekworkshop.finance.entity.*;
import com.geekworkshop.finance.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

@Service
public class WorkbenchService {
    private final ReimbursementRepository reimbursementRepository;
    private final PurchaseApplicationRepository purchaseRepository;
    private final LaborApplicationRepository laborRepository;
    private final AdvanceApplicationRepository advanceRepository;
    private final ApprovalRecordRepository reimbursementApprovalRepository;
    private final PurchaseApprovalRecordRepository purchaseApprovalRepository;
    private final LaborApprovalRecordRepository laborApprovalRepository;
    private final AdvanceApprovalRecordRepository advanceApprovalRepository;

    public WorkbenchService(
            ReimbursementRepository reimbursementRepository,
            PurchaseApplicationRepository purchaseRepository,
            LaborApplicationRepository laborRepository,
            AdvanceApplicationRepository advanceRepository,
            ApprovalRecordRepository reimbursementApprovalRepository,
            PurchaseApprovalRecordRepository purchaseApprovalRepository,
            LaborApprovalRecordRepository laborApprovalRepository,
            AdvanceApprovalRecordRepository advanceApprovalRepository
    ) {
        this.reimbursementRepository = reimbursementRepository;
        this.purchaseRepository = purchaseRepository;
        this.laborRepository = laborRepository;
        this.advanceRepository = advanceRepository;
        this.reimbursementApprovalRepository = reimbursementApprovalRepository;
        this.purchaseApprovalRepository = purchaseApprovalRepository;
        this.laborApprovalRepository = laborApprovalRepository;
        this.advanceApprovalRepository = advanceApprovalRepository;
    }

    @Transactional(readOnly = true)
    public List<WorkbenchItemResponse> list(
            AppUser user, WorkbenchScope scope, String businessType, String status, String keyword
    ) {
        Stream<WorkbenchItemResponse> stream = switch (scope) {
            case MY_APPLICATIONS -> myApplications(user);
            case MY_TODOS -> myTodos(user);
            case DONE -> done(user);
        };
        String normalizedType = normalize(businessType);
        String normalizedStatus = normalize(status);
        String normalizedKeyword = normalize(keyword);
        return stream
                .filter(item -> normalizedType == null || item.businessType().equalsIgnoreCase(normalizedType))
                .filter(item -> normalizedStatus == null || item.status().equalsIgnoreCase(normalizedStatus))
                .filter(item -> normalizedKeyword == null || contains(item, normalizedKeyword))
                .sorted(Comparator.comparing(
                        WorkbenchItemResponse::time,
                        Comparator.nullsLast(Comparator.reverseOrder())
                ))
                .toList();
    }

    private Stream<WorkbenchItemResponse> myApplications(AppUser user) {
        Long userId = user.getId();
        if (user.getRole() == UserRole.OFFICE) {
            return purchaseRepository.findAllDetails().stream()
                    .filter(item -> applicantId(item.getApplicant()).equals(userId))
                    .map(this::fromPurchase);
        }
        return Stream.of(
                reimbursementRepository.findAllForExport().stream()
                        .filter(item -> applicantId(item.getApplicant()).equals(userId))
                        .map(this::fromReimbursement),
                purchaseRepository.findAllDetails().stream()
                        .filter(item -> applicantId(item.getApplicant()).equals(userId))
                        .map(this::fromPurchase),
                laborRepository.findAllDetails().stream()
                        .filter(item -> applicantId(item.getApplicant()).equals(userId))
                        .map(this::fromLabor),
                advanceRepository.findAllDetails().stream()
                        .filter(item -> applicantId(item.getApplicant()).equals(userId))
                        .map(this::fromAdvance)
        ).flatMap(Function.identity());
    }

    private Stream<WorkbenchItemResponse> myTodos(AppUser user) {
        return Stream.of(
                reimbursementRepository.findAllForExport().stream()
                        .filter(item -> pendingFor(user, item))
                        .map(this::fromReimbursement)
                        .map(item -> withTodoActions(user, item)),
                purchaseRepository.findAllDetails().stream()
                        .filter(item -> pendingFor(user, item))
                        .map(this::fromPurchase)
                        .map(item -> withTodoActions(user, item)),
                laborRepository.findAllDetails().stream()
                        .filter(item -> pendingFor(user, item))
                        .map(this::fromLabor)
                        .map(item -> withTodoActions(user, item)),
                advanceRepository.findAllDetails().stream()
                        .filter(item -> pendingFor(user, item))
                        .map(this::fromAdvance)
                        .map(item -> withTodoActions(user, item))
        ).flatMap(Function.identity());
    }

    private Stream<WorkbenchItemResponse> done(AppUser user) {
        Map<String, WorkbenchItemResponse> items = new LinkedHashMap<>();
        reimbursementApprovalRepository.findDetailsByApproverId(user.getId()).stream()
                .filter(record -> handledNode(record.getApprovalNode()))
                .forEach(record -> mergeLatest(items, fromReimbursement(record.getReimbursement()), record.getCreatedAt()));
        purchaseApprovalRepository.findDetailsByApproverId(user.getId()).stream()
                .filter(record -> handledNode(record.getApprovalNode()))
                .forEach(record -> mergeLatest(items, fromPurchase(record.getPurchaseApplication()), record.getCreatedAt()));
        laborApprovalRepository.findDetailsByApproverId(user.getId()).stream()
                .filter(record -> handledNode(record.getApprovalNode()))
                .forEach(record -> mergeLatest(items, fromLabor(record.getLaborApplication()), record.getCreatedAt()));
        advanceApprovalRepository.findDetailsByApproverId(user.getId()).stream()
                .filter(record -> handledNode(record.getApprovalNode()))
                .forEach(record -> mergeLatest(items, fromAdvance(record.getAdvanceApplication()), record.getCreatedAt()));
        return items.values().stream();
    }

    private void mergeLatest(
            Map<String, WorkbenchItemResponse> items, WorkbenchItemResponse item, LocalDateTime handledAt
    ) {
        WorkbenchItemResponse handled = new WorkbenchItemResponse(
                item.businessType(), item.businessId(), item.number(), item.title(),
                item.applicantId(), item.applicantName(), item.departmentId(), item.departmentName(),
                item.amount(), item.status(), handledAt, item.currentNode(), handledAt,
                List.of(WorkbenchAction.VIEW)
        );
        items.merge(item.businessType() + "-" + item.businessId(), handled,
                (left, right) -> left.time() != null && left.time().isAfter(right.time()) ? left : right);
    }

    private boolean handledNode(String node) {
        return StringUtils.hasText(node) && !"SUBMIT".equalsIgnoreCase(node) && !"CREATE".equalsIgnoreCase(node);
    }

    private boolean pendingFor(AppUser user, Reimbursement item) {
        return switch (user.getRole()) {
            case FINANCE -> item.getStatus() == ReimbursementStatus.SUBMITTED
                    || item.getStatus() == ReimbursementStatus.PAID;
            case DEPARTMENT_MANAGER -> item.getStatus() == ReimbursementStatus.FINANCE_INITIAL_APPROVED
                    && sameDepartment(user, item.getDepartment());
            case EXECUTIVE -> item.getStatus() == ReimbursementStatus.DEPARTMENT_APPROVED;
            case CASHIER -> item.getStatus() == ReimbursementStatus.EXECUTIVE_APPROVED;
            case ADMIN -> EnumSet.of(
                    ReimbursementStatus.SUBMITTED, ReimbursementStatus.FINANCE_INITIAL_APPROVED,
                    ReimbursementStatus.DEPARTMENT_APPROVED, ReimbursementStatus.EXECUTIVE_APPROVED,
                    ReimbursementStatus.PAID
            ).contains(item.getStatus());
            default -> false;
        };
    }

    private boolean pendingFor(AppUser user, PurchaseApplication item) {
        return switch (user.getRole()) {
            case FINANCE -> item.getStatus() == PurchaseStatus.SUBMITTED;
            case DEPARTMENT_MANAGER -> item.getStatus() == PurchaseStatus.FINANCE_APPROVED
                    && sameDepartment(user, item.getDepartment());
            case EXECUTIVE -> item.getStatus() == PurchaseStatus.DEPARTMENT_APPROVED;
            case ADMIN -> EnumSet.of(
                    PurchaseStatus.SUBMITTED, PurchaseStatus.FINANCE_APPROVED, PurchaseStatus.DEPARTMENT_APPROVED
            ).contains(item.getStatus());
            default -> false;
        };
    }

    private boolean pendingFor(AppUser user, LaborApplication item) {
        return switch (user.getRole()) {
            case FINANCE -> item.getStatus() == LaborStatus.SUBMITTED || item.getStatus() == LaborStatus.PAID;
            case DEPARTMENT_MANAGER -> item.getStatus() == LaborStatus.FINANCE_INITIAL_APPROVED
                    && sameDepartment(user, item.getDepartment());
            case EXECUTIVE -> item.getStatus() == LaborStatus.DEPARTMENT_APPROVED;
            case CASHIER -> item.getStatus() == LaborStatus.EXECUTIVE_APPROVED;
            case ADMIN -> EnumSet.of(
                    LaborStatus.SUBMITTED, LaborStatus.FINANCE_INITIAL_APPROVED,
                    LaborStatus.DEPARTMENT_APPROVED, LaborStatus.EXECUTIVE_APPROVED, LaborStatus.PAID
            ).contains(item.getStatus());
            default -> false;
        };
    }

    private boolean pendingFor(AppUser user, AdvanceApplication item) {
        return switch (user.getRole()) {
            case DEPARTMENT_MANAGER -> item.getStatus() == AdvanceStatus.SUBMITTED
                    && sameDepartment(user, item.getDepartment());
            case FINANCE -> item.getStatus() == AdvanceStatus.DEPARTMENT_APPROVED
                    || item.getStatus() == AdvanceStatus.PAID
                    || needsOffset(item);
            case EXECUTIVE -> item.getStatus() == AdvanceStatus.FINANCE_APPROVED;
            case CASHIER -> item.getStatus() == AdvanceStatus.EXECUTIVE_APPROVED;
            case ADMIN -> EnumSet.of(
                    AdvanceStatus.SUBMITTED, AdvanceStatus.DEPARTMENT_APPROVED,
                    AdvanceStatus.FINANCE_APPROVED, AdvanceStatus.EXECUTIVE_APPROVED, AdvanceStatus.PAID
            ).contains(item.getStatus()) || needsOffset(item);
            default -> false;
        };
    }

    private WorkbenchItemResponse fromReimbursement(Reimbursement item) {
        return response("REIMBURSEMENT", item.getId(), item.getApprovalNumber(), item.getTitle(),
                item.getApplicant(), item.getDepartment(), item.getAmount(), item.getStatus().name(), item.getCreatedAt(),
                reimbursementNode(item), reimbursementWaitingSince(item));
    }

    private WorkbenchItemResponse fromPurchase(PurchaseApplication item) {
        return response("PURCHASE", item.getId(), item.getApplicationNumber(), item.getPurchaseReason(),
                item.getApplicant(), item.getDepartment(), item.getAmount(), item.getStatus().name(), item.getCreatedAt(),
                purchaseNode(item), purchaseWaitingSince(item));
    }

    private WorkbenchItemResponse fromLabor(LaborApplication item) {
        return response("LABOR", item.getId(), item.getApplicationNumber(), item.getTitle(),
                item.getApplicant(), item.getDepartment(), item.getTotalAmount(), item.getStatus().name(), item.getCreatedAt(),
                laborNode(item), laborWaitingSince(item));
    }

    private WorkbenchItemResponse fromAdvance(AdvanceApplication item) {
        return response("ADVANCE", item.getId(), item.getApplicationNumber(), item.getReason(),
                item.getApplicant(), item.getDepartment(), item.getAmount(), item.getStatus().name(), item.getCreatedAt(),
                advanceNode(item), advanceWaitingSince(item));
    }

    private WorkbenchItemResponse response(
            String type, Long id, String number, String title, AppUser applicant, Department department,
            java.math.BigDecimal amount, String status, LocalDateTime time,
            String currentNode, LocalDateTime waitingSince
    ) {
        return new WorkbenchItemResponse(
                type, id, number, title, applicantId(applicant),
                applicant == null ? null : applicant.getRealName(),
                department == null ? null : department.getId(),
                department == null ? null : department.getName(),
                amount, status, time, currentNode, waitingSince, List.of(WorkbenchAction.VIEW)
        );
    }

    private WorkbenchItemResponse withTodoActions(AppUser user, WorkbenchItemResponse item) {
        List<WorkbenchAction> actions = new ArrayList<>();
        switch (item.currentNode()) {
            case "CASHIER_PAYMENT" -> {
                actions.add(WorkbenchAction.UPLOAD_RECEIPT);
                actions.add(WorkbenchAction.PAY);
            }
            case "FINANCE_RECHECK" -> {
                actions.add(WorkbenchAction.FINANCE_RECHECK);
                actions.add(WorkbenchAction.REJECT);
                if ("ADVANCE".equals(item.businessType())) actions.add(WorkbenchAction.OFFSET);
            }
            case "OFFSET" -> actions.add(WorkbenchAction.OFFSET);
            default -> {
                actions.add(WorkbenchAction.APPROVE);
                actions.add(WorkbenchAction.REJECT);
            }
        }
        actions.add(WorkbenchAction.VIEW);
        return new WorkbenchItemResponse(
                item.businessType(), item.businessId(), item.number(), item.title(),
                item.applicantId(), item.applicantName(), item.departmentId(), item.departmentName(),
                item.amount(), item.status(), item.time(), item.currentNode(), item.waitingSince(),
                List.copyOf(actions)
        );
    }

    private boolean needsOffset(AdvanceApplication item) {
        return item.getStatus() == AdvanceStatus.COMPLETED
                && EnumSet.of(
                        SettlementStatus.PENDING_OFFSET,
                        SettlementStatus.PARTIAL_OFFSET,
                        SettlementStatus.OVERDUE
                ).contains(item.getSettlementStatus());
    }

    private String reimbursementNode(Reimbursement item) {
        return switch (item.getStatus()) {
            case SUBMITTED -> "FINANCE_INITIAL";
            case FINANCE_INITIAL_APPROVED -> "DEPARTMENT";
            case DEPARTMENT_APPROVED -> "EXECUTIVE";
            case EXECUTIVE_APPROVED -> "CASHIER_PAYMENT";
            case PAID -> "FINANCE_RECHECK";
            default -> item.getStatus().name();
        };
    }

    private LocalDateTime reimbursementWaitingSince(Reimbursement item) {
        String previousNode = switch (item.getStatus()) {
            case FINANCE_INITIAL_APPROVED -> "FINANCE_INITIAL";
            case DEPARTMENT_APPROVED -> "DEPARTMENT";
            case EXECUTIVE_APPROVED -> "EXECUTIVE";
            case PAID -> "CASHIER_PAYMENT";
            default -> null;
        };
        if (previousNode == null) return fallback(item.getSubmittedAt(), item.getCreatedAt());
        return latestTime(
                reimbursementApprovalRepository.findByReimbursementIdOrderByCreatedAtAsc(item.getId()),
                ApprovalRecord::getApprovalNode, ApprovalRecord::getCreatedAt, previousNode,
                fallback(item.getSubmittedAt(), item.getCreatedAt())
        );
    }

    private String purchaseNode(PurchaseApplication item) {
        return switch (item.getStatus()) {
            case SUBMITTED -> "FINANCE";
            case FINANCE_APPROVED -> "DEPARTMENT";
            case DEPARTMENT_APPROVED -> "EXECUTIVE";
            default -> item.getStatus().name();
        };
    }

    private LocalDateTime purchaseWaitingSince(PurchaseApplication item) {
        String previousNode = switch (item.getStatus()) {
            case FINANCE_APPROVED -> "FINANCE";
            case DEPARTMENT_APPROVED -> "DEPARTMENT";
            default -> null;
        };
        if (previousNode == null) return fallback(item.getSubmittedAt(), item.getCreatedAt());
        return latestTime(
                purchaseApprovalRepository.findByPurchaseApplicationIdOrderByCreatedAtAsc(item.getId()),
                PurchaseApprovalRecord::getApprovalNode, PurchaseApprovalRecord::getCreatedAt, previousNode,
                fallback(item.getSubmittedAt(), item.getCreatedAt())
        );
    }

    private String laborNode(LaborApplication item) {
        return switch (item.getStatus()) {
            case SUBMITTED -> "FINANCE_INITIAL";
            case FINANCE_INITIAL_APPROVED -> "DEPARTMENT";
            case DEPARTMENT_APPROVED -> "EXECUTIVE";
            case EXECUTIVE_APPROVED -> "CASHIER_PAYMENT";
            case PAID -> "FINANCE_RECHECK";
            default -> item.getStatus().name();
        };
    }

    private LocalDateTime laborWaitingSince(LaborApplication item) {
        String previousNode = switch (item.getStatus()) {
            case FINANCE_INITIAL_APPROVED -> "FINANCE_INITIAL";
            case DEPARTMENT_APPROVED -> "DEPARTMENT";
            case EXECUTIVE_APPROVED -> "EXECUTIVE";
            case PAID -> "CASHIER_PAYMENT";
            default -> null;
        };
        if (previousNode == null) return fallback(item.getSubmittedAt(), item.getCreatedAt());
        return latestTime(
                laborApprovalRepository.findDetailsByApplicationId(item.getId()),
                LaborApprovalRecord::getApprovalNode, LaborApprovalRecord::getCreatedAt, previousNode,
                fallback(item.getSubmittedAt(), item.getCreatedAt())
        );
    }

    private String advanceNode(AdvanceApplication item) {
        if (needsOffset(item)) return "OFFSET";
        return switch (item.getStatus()) {
            case SUBMITTED -> "DEPARTMENT";
            case DEPARTMENT_APPROVED -> "FINANCE";
            case FINANCE_APPROVED -> "EXECUTIVE";
            case EXECUTIVE_APPROVED -> "CASHIER_PAYMENT";
            case PAID -> "FINANCE_RECHECK";
            default -> item.getStatus().name();
        };
    }

    private LocalDateTime advanceWaitingSince(AdvanceApplication item) {
        String previousNode = needsOffset(item) ? "FINANCE_RECHECK" : switch (item.getStatus()) {
            case DEPARTMENT_APPROVED -> "DEPARTMENT";
            case FINANCE_APPROVED -> "FINANCE";
            case EXECUTIVE_APPROVED -> "EXECUTIVE";
            case PAID -> "CASHIER_PAYMENT";
            default -> null;
        };
        if (previousNode == null) return fallback(item.getSubmittedAt(), item.getCreatedAt());
        return latestTime(
                advanceApprovalRepository.findDetailsByApplicationId(item.getId()),
                AdvanceApprovalRecord::getApprovalNode, AdvanceApprovalRecord::getCreatedAt, previousNode,
                fallback(item.getSubmittedAt(), item.getCreatedAt())
        );
    }

    private <T> LocalDateTime latestTime(
            List<T> records,
            Function<T, String> node,
            Function<T, LocalDateTime> createdAt,
            String expectedNode,
            LocalDateTime fallback
    ) {
        return records.stream()
                .filter(record -> expectedNode.equals(node.apply(record)))
                .map(createdAt)
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(fallback);
    }

    private LocalDateTime fallback(LocalDateTime preferred, LocalDateTime fallback) {
        return preferred == null ? fallback : preferred;
    }

    private Long applicantId(AppUser applicant) {
        return applicant == null ? -1L : applicant.getId();
    }

    private boolean sameDepartment(AppUser user, Department department) {
        return user.getDepartment() != null && department != null
                && Objects.equals(user.getDepartment().getId(), department.getId());
    }

    private boolean contains(WorkbenchItemResponse item, String keyword) {
        String value = String.join(" ",
                Objects.toString(item.number(), ""),
                Objects.toString(item.title(), ""),
                Objects.toString(item.applicantName(), ""),
                Objects.toString(item.departmentName(), "")
        ).toLowerCase(Locale.ROOT);
        return value.contains(keyword.toLowerCase(Locale.ROOT));
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
