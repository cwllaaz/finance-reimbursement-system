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
  title VARCHAR(120) NOT NULL,
  expense_type VARCHAR(60) NOT NULL,
  amount DECIMAL(12, 2) NOT NULL,
  expense_date DATE NOT NULL,
  description VARCHAR(500),
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
