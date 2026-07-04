CREATE DATABASE IF NOT EXISTS finance_reimbursement
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE finance_reimbursement;

CREATE TABLE IF NOT EXISTS department (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(80) NOT NULL,
  code VARCHAR(40) NOT NULL UNIQUE,
  manager_name VARCHAR(80),
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `user` (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(60) NOT NULL UNIQUE,
  password VARCHAR(120) NOT NULL,
  real_name VARCHAR(80) NOT NULL,
  role VARCHAR(40) NOT NULL,
  department_id BIGINT,
  phone VARCHAR(40),
  email VARCHAR(100),
  enabled BIT NOT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  CONSTRAINT fk_user_department
    FOREIGN KEY (department_id) REFERENCES department(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS budget (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  department_id BIGINT NOT NULL,
  budget_year INT NOT NULL,
  total_amount DECIMAL(12, 2) NOT NULL,
  used_amount DECIMAL(12, 2) NOT NULL,
  remaining_amount DECIMAL(12, 2) NOT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  CONSTRAINT fk_budget_department
    FOREIGN KEY (department_id) REFERENCES department(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS reimbursement (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  approval_number VARCHAR(32) UNIQUE,
  title VARCHAR(120) NOT NULL,
  expense_type VARCHAR(60) NOT NULL,
  amount DECIMAL(12, 2) NOT NULL,
  expense_date DATE NOT NULL,
  description VARCHAR(500),
  applicant_phone VARCHAR(40),
  budget_number VARCHAR(60),
  reimbursement_reason VARCHAR(500),
  payment_date DATE,
  payee_name VARCHAR(160),
  bank_account VARCHAR(80),
  bank_name VARCHAR(160),
  payment_total DECIMAL(12, 2),
  payment_voucher_number VARCHAR(80),
  related_purchase_number VARCHAR(40),
  high_value_explanation VARCHAR(1000),
  status VARCHAR(40) NOT NULL,
  applicant_id BIGINT,
  department_id BIGINT,
  submitted_at DATETIME,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  CONSTRAINT fk_reimbursement_applicant
    FOREIGN KEY (applicant_id) REFERENCES `user`(id),
  CONSTRAINT fk_reimbursement_department
    FOREIGN KEY (department_id) REFERENCES department(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS approval_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  reimbursement_id BIGINT NOT NULL,
  approver_id BIGINT,
  approval_node VARCHAR(60) NOT NULL,
  action VARCHAR(20) NOT NULL,
  comment VARCHAR(500),
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  CONSTRAINT fk_approval_reimbursement
    FOREIGN KEY (reimbursement_id) REFERENCES reimbursement(id),
  CONSTRAINT fk_approval_approver
    FOREIGN KEY (approver_id) REFERENCES `user`(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS attachment (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  reimbursement_id BIGINT NOT NULL,
  file_name VARCHAR(200) NOT NULL,
  file_url VARCHAR(500) NOT NULL,
  file_type VARCHAR(80),
  file_size BIGINT,
  attachment_type VARCHAR(40),
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  CONSTRAINT fk_attachment_reimbursement
    FOREIGN KEY (reimbursement_id) REFERENCES reimbursement(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS invoice_ocr_result (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  reimbursement_id BIGINT NOT NULL,
  attachment_id BIGINT,
  invoice_code VARCHAR(80),
  invoice_number VARCHAR(80),
  invoice_date DATE,
  amount DECIMAL(12, 2),
  tax_amount DECIMAL(12, 2),
  amount_matched BIT,
  amount_difference DECIMAL(12, 2),
  verification_message VARCHAR(200),
  seller_name VARCHAR(200),
  buyer_name VARCHAR(200),
  ocr_status VARCHAR(40) NOT NULL,
  raw_ocr_json TEXT,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  CONSTRAINT uk_invoice_ocr_reimbursement UNIQUE (reimbursement_id),
  CONSTRAINT fk_invoice_ocr_reimbursement
    FOREIGN KEY (reimbursement_id) REFERENCES reimbursement(id),
  CONSTRAINT fk_invoice_ocr_attachment
    FOREIGN KEY (attachment_id) REFERENCES attachment(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS operation_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT,
  username VARCHAR(60),
  real_name VARCHAR(80),
  role VARCHAR(40),
  action VARCHAR(80) NOT NULL,
  module VARCHAR(80) NOT NULL,
  target_id BIGINT,
  target_name VARCHAR(160),
  detail VARCHAR(1000),
  ip_address VARCHAR(80),
  created_at DATETIME NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS purchase_application (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  application_number VARCHAR(30) NOT NULL UNIQUE,
  applicant_id BIGINT NOT NULL,
  department_id BIGINT,
  applicant_phone VARCHAR(40),
  amount DECIMAL(14, 2) NOT NULL,
  budget_number VARCHAR(80),
  purchase_method VARCHAR(80) NOT NULL,
  tax_exempt BIT NOT NULL DEFAULT 0,
  use_location VARCHAR(200),
  purchase_reason VARCHAR(1000) NOT NULL,
  asset_acceptance_number VARCHAR(80),
  status VARCHAR(40) NOT NULL,
  submitted_at DATETIME,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  CONSTRAINT fk_purchase_applicant FOREIGN KEY (applicant_id) REFERENCES `user`(id),
  CONSTRAINT fk_purchase_department FOREIGN KEY (department_id) REFERENCES department(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS purchase_item (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  purchase_application_id BIGINT NOT NULL,
  item_name VARCHAR(200) NOT NULL,
  specification VARCHAR(200),
  manufacturer VARCHAR(200),
  unit_price DECIMAL(14, 2) NOT NULL,
  quantity INT NOT NULL,
  total_price DECIMAL(14, 2) NOT NULL,
  CONSTRAINT fk_purchase_item_application
    FOREIGN KEY (purchase_application_id) REFERENCES purchase_application(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS purchase_attachment (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  purchase_application_id BIGINT NOT NULL,
  file_name VARCHAR(200) NOT NULL,
  file_url VARCHAR(500) NOT NULL,
  file_type VARCHAR(100),
  file_size BIGINT,
  attachment_type VARCHAR(40) NOT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  CONSTRAINT fk_purchase_attachment_application
    FOREIGN KEY (purchase_application_id) REFERENCES purchase_application(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS purchase_approval_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  purchase_application_id BIGINT NOT NULL,
  approver_id BIGINT,
  approval_node VARCHAR(60) NOT NULL,
  action VARCHAR(20) NOT NULL,
  comment VARCHAR(500),
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  CONSTRAINT fk_purchase_approval_application
    FOREIGN KEY (purchase_application_id) REFERENCES purchase_application(id) ON DELETE CASCADE,
  CONSTRAINT fk_purchase_approval_approver FOREIGN KEY (approver_id) REFERENCES `user`(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS asset_acceptance (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  acceptance_number VARCHAR(30) NOT NULL UNIQUE,
  purchase_application_id BIGINT NOT NULL UNIQUE,
  accepted_by BIGINT,
  received_at DATETIME NOT NULL,
  storage_location VARCHAR(200) NOT NULL,
  remark VARCHAR(500),
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  CONSTRAINT fk_asset_acceptance_purchase
    FOREIGN KEY (purchase_application_id) REFERENCES purchase_application(id),
  CONSTRAINT fk_asset_acceptance_user FOREIGN KEY (accepted_by) REFERENCES `user`(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS asset (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  asset_number VARCHAR(30) NOT NULL UNIQUE,
  acceptance_id BIGINT NOT NULL,
  purchase_item_id BIGINT NOT NULL,
  item_name VARCHAR(200) NOT NULL,
  specification VARCHAR(200),
  manufacturer VARCHAR(200),
  quantity INT NOT NULL,
  total_price DECIMAL(14, 2) NOT NULL,
  received_at DATETIME NOT NULL,
  location VARCHAR(200) NOT NULL,
  status VARCHAR(40) NOT NULL,
  custodian_id BIGINT,
  claimed_by BIGINT,
  claimed_at DATETIME,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  CONSTRAINT fk_asset_acceptance FOREIGN KEY (acceptance_id) REFERENCES asset_acceptance(id),
  CONSTRAINT fk_asset_purchase_item FOREIGN KEY (purchase_item_id) REFERENCES purchase_item(id),
  CONSTRAINT fk_asset_custodian FOREIGN KEY (custodian_id) REFERENCES `user`(id),
  CONSTRAINT fk_asset_claimed_by FOREIGN KEY (claimed_by) REFERENCES `user`(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS asset_history (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  asset_id BIGINT NOT NULL,
  receipt_number VARCHAR(30),
  action VARCHAR(40) NOT NULL,
  operator_id BIGINT NOT NULL,
  custodian_id BIGINT,
  location VARCHAR(200),
  asset_status VARCHAR(40) NOT NULL,
  remark VARCHAR(500),
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  CONSTRAINT fk_asset_history_asset FOREIGN KEY (asset_id) REFERENCES asset(id),
  CONSTRAINT fk_asset_history_operator FOREIGN KEY (operator_id) REFERENCES `user`(id),
  CONSTRAINT fk_asset_history_custodian FOREIGN KEY (custodian_id) REFERENCES `user`(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS labor_application (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  application_number VARCHAR(30) NOT NULL UNIQUE,
  category VARCHAR(40) NOT NULL,
  title VARCHAR(200) NOT NULL,
  description VARCHAR(1000),
  budget_number VARCHAR(80),
  total_amount DECIMAL(14, 2) NOT NULL,
  amount_in_words VARCHAR(160) NOT NULL,
  status VARCHAR(40) NOT NULL,
  applicant_id BIGINT NOT NULL,
  department_id BIGINT,
  submitted_at DATETIME,
  payment_date DATE,
  payment_amount DECIMAL(14, 2),
  payment_voucher_number VARCHAR(80),
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  CONSTRAINT fk_labor_applicant FOREIGN KEY (applicant_id) REFERENCES `user`(id),
  CONSTRAINT fk_labor_department FOREIGN KEY (department_id) REFERENCES department(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS labor_recipient (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  labor_application_id BIGINT NOT NULL,
  name VARCHAR(80) NOT NULL,
  phone VARCHAR(40),
  id_card VARCHAR(40) NOT NULL,
  organization VARCHAR(160),
  position VARCHAR(100),
  service_content VARCHAR(500) NOT NULL,
  net_amount DECIMAL(14, 2) NOT NULL,
  bank_account VARCHAR(80) NOT NULL,
  bank_name VARCHAR(160) NOT NULL,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  CONSTRAINT fk_labor_recipient_application
    FOREIGN KEY (labor_application_id) REFERENCES labor_application(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS labor_attachment (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  labor_application_id BIGINT NOT NULL,
  attachment_type VARCHAR(40) NOT NULL,
  file_name VARCHAR(255) NOT NULL,
  file_url VARCHAR(500) NOT NULL,
  file_type VARCHAR(120),
  file_size BIGINT,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  CONSTRAINT fk_labor_attachment_application
    FOREIGN KEY (labor_application_id) REFERENCES labor_application(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS labor_approval_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  labor_application_id BIGINT NOT NULL,
  approver_id BIGINT,
  approval_node VARCHAR(60) NOT NULL,
  action VARCHAR(20) NOT NULL,
  comment VARCHAR(500),
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  CONSTRAINT fk_labor_approval_application
    FOREIGN KEY (labor_application_id) REFERENCES labor_application(id) ON DELETE CASCADE,
  CONSTRAINT fk_labor_approval_user FOREIGN KEY (approver_id) REFERENCES `user`(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS advance_application (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  application_number VARCHAR(30) NOT NULL UNIQUE,
  type VARCHAR(30) NOT NULL,
  reason VARCHAR(1000) NOT NULL,
  amount DECIMAL(14, 2) NOT NULL,
  payment_method VARCHAR(60) NOT NULL,
  payee_name VARCHAR(160) NOT NULL,
  bank_account VARCHAR(80) NOT NULL,
  bank_name VARCHAR(160) NOT NULL,
  expected_repayment_date DATE,
  partner_name VARCHAR(200),
  expected_settlement_date DATE,
  status VARCHAR(40) NOT NULL,
  settlement_status VARCHAR(40),
  offset_amount DECIMAL(14, 2) NOT NULL DEFAULT 0,
  applicant_id BIGINT NOT NULL,
  department_id BIGINT,
  submitted_at DATETIME,
  payment_date DATE,
  payment_amount DECIMAL(14, 2),
  payment_voucher_number VARCHAR(80),
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  CONSTRAINT fk_advance_applicant FOREIGN KEY (applicant_id) REFERENCES `user`(id),
  CONSTRAINT fk_advance_department FOREIGN KEY (department_id) REFERENCES department(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS advance_approval_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  advance_application_id BIGINT NOT NULL,
  approver_id BIGINT,
  approval_node VARCHAR(60) NOT NULL,
  action VARCHAR(20) NOT NULL,
  comment VARCHAR(500),
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  CONSTRAINT fk_advance_approval_application
    FOREIGN KEY (advance_application_id) REFERENCES advance_application(id) ON DELETE CASCADE,
  CONSTRAINT fk_advance_approval_user FOREIGN KEY (approver_id) REFERENCES `user`(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS advance_attachment (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  advance_application_id BIGINT NOT NULL,
  attachment_type VARCHAR(40) NOT NULL,
  file_name VARCHAR(255) NOT NULL,
  file_url VARCHAR(500) NOT NULL,
  file_type VARCHAR(120),
  file_size BIGINT,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  CONSTRAINT fk_advance_attachment_application
    FOREIGN KEY (advance_application_id) REFERENCES advance_application(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS advance_offset_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  advance_application_id BIGINT NOT NULL,
  operator_id BIGINT NOT NULL,
  amount DECIMAL(14, 2) NOT NULL,
  comment VARCHAR(500),
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  CONSTRAINT fk_advance_offset_application
    FOREIGN KEY (advance_application_id) REFERENCES advance_application(id) ON DELETE CASCADE,
  CONSTRAINT fk_advance_offset_user FOREIGN KEY (operator_id) REFERENCES `user`(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS income_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  income_number VARCHAR(30) NOT NULL UNIQUE,
  receipt_date DATE NOT NULL,
  voucher_number VARCHAR(80),
  payer_name VARCHAR(200) NOT NULL,
  income_category VARCHAR(80) NOT NULL,
  amount DECIMAL(14, 2) NOT NULL,
  funding_source VARCHAR(160),
  arrival_account VARCHAR(160),
  invoice_status VARCHAR(80),
  remark VARCHAR(1000),
  department_id BIGINT,
  created_by BIGINT,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  CONSTRAINT fk_income_department FOREIGN KEY (department_id) REFERENCES department(id),
  CONSTRAINT fk_income_created_by FOREIGN KEY (created_by) REFERENCES `user`(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS income_attachment (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  income_record_id BIGINT NOT NULL,
  file_name VARCHAR(200) NOT NULL,
  file_url VARCHAR(500) NOT NULL,
  file_type VARCHAR(100),
  file_size BIGINT,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  CONSTRAINT fk_income_attachment_record
    FOREIGN KEY (income_record_id) REFERENCES income_record(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS asset_claim_application (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  asset_id BIGINT NOT NULL,
  applicant_id BIGINT NOT NULL,
  department_id BIGINT,
  use_location VARCHAR(200) NOT NULL,
  reason VARCHAR(500) NOT NULL,
  status VARCHAR(30) NOT NULL,
  reviewed_by BIGINT,
  review_comment VARCHAR(500),
  reviewed_at DATETIME,
  created_at DATETIME NOT NULL,
  updated_at DATETIME NOT NULL,
  CONSTRAINT fk_asset_claim_asset FOREIGN KEY (asset_id) REFERENCES asset(id),
  CONSTRAINT fk_asset_claim_applicant FOREIGN KEY (applicant_id) REFERENCES `user`(id),
  CONSTRAINT fk_asset_claim_department FOREIGN KEY (department_id) REFERENCES department(id),
  CONSTRAINT fk_asset_claim_reviewer FOREIGN KEY (reviewed_by) REFERENCES `user`(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
