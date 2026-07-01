package com.geekworkshop.finance.entity;

public enum ReimbursementStatus {
    DRAFT,
    SUBMITTED,
    FINANCE_INITIAL_APPROVED,
    DEPARTMENT_APPROVED,
    EXECUTIVE_APPROVED,
    PAID,
    COMPLETED,
    // Legacy statuses retained for existing database rows.
    FINANCE_APPROVED,
    APPROVED,
    REJECTED
}
