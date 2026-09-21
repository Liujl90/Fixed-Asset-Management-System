CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(64) NOT NULL,
    password VARCHAR(128) NOT NULL,
    real_name VARCHAR(64) NOT NULL,
    phone VARCHAR(32),
    email VARCHAR(128),
    employee_id BIGINT,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_sys_user_username UNIQUE (username)
);

CREATE TABLE IF NOT EXISTS sys_role (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(64) NOT NULL,
    code VARCHAR(64) NOT NULL,
    description VARCHAR(255),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_sys_role_code UNIQUE (code)
);

CREATE TABLE IF NOT EXISTS sys_user_role (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    CONSTRAINT uk_sys_user_role UNIQUE (user_id, role_id)
);

CREATE TABLE IF NOT EXISTS sys_permission (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(64) NOT NULL,
    code VARCHAR(96) NOT NULL,
    module VARCHAR(64) NOT NULL,
    CONSTRAINT uk_sys_permission_code UNIQUE (code)
);

CREATE TABLE IF NOT EXISTS sys_role_permission (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    role_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    CONSTRAINT uk_sys_role_permission UNIQUE (role_id, permission_id)
);

CREATE TABLE IF NOT EXISTS sys_operation_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    username VARCHAR(64),
    module VARCHAR(64) NOT NULL,
    action VARCHAR(64) NOT NULL,
    method VARCHAR(16),
    path VARCHAR(255),
    params VARCHAR(2000),
    success BOOLEAN NOT NULL DEFAULT TRUE,
    error_message VARCHAR(500),
    duration_ms BIGINT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS department (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    code VARCHAR(50) NOT NULL,
    manager_id BIGINT,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_department_code UNIQUE (code)
);

CREATE TABLE IF NOT EXISTS employee (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    employee_no VARCHAR(50) NOT NULL,
    name VARCHAR(64) NOT NULL,
    department_id BIGINT NOT NULL,
    phone VARCHAR(32),
    email VARCHAR(128),
    join_date DATE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_employee_no UNIQUE (employee_no)
);

CREATE TABLE IF NOT EXISTS asset_category (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    code VARCHAR(50) NOT NULL,
    parent_id BIGINT,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_asset_category_code UNIQUE (code)
);

CREATE TABLE IF NOT EXISTS asset (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    asset_no VARCHAR(64) NOT NULL,
    name VARCHAR(128) NOT NULL,
    category_id BIGINT NOT NULL,
    brand_model VARCHAR(128),
    purchase_date DATE,
    original_value DECIMAL(14, 2) NOT NULL DEFAULT 0,
    useful_life INT NOT NULL DEFAULT 0,
    department_id BIGINT,
    owner_id BIGINT,
    status VARCHAR(32) NOT NULL,
    remark VARCHAR(500),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_asset_no UNIQUE (asset_no)
);

CREATE INDEX IF NOT EXISTS idx_asset_name ON asset (name);
CREATE INDEX IF NOT EXISTS idx_asset_category ON asset (category_id);
CREATE INDEX IF NOT EXISTS idx_asset_department ON asset (department_id);
CREATE INDEX IF NOT EXISTS idx_asset_status ON asset (status);

CREATE TABLE IF NOT EXISTS asset_change_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    asset_id BIGINT NOT NULL,
    type VARCHAR(64) NOT NULL,
    description VARCHAR(500) NOT NULL,
    operator VARCHAR(64),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_asset_change_asset ON asset_change_record (asset_id, created_at);

CREATE TABLE IF NOT EXISTS loan_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    asset_id BIGINT NOT NULL,
    applicant_id BIGINT NOT NULL,
    department_id BIGINT NOT NULL,
    owner_id BIGINT,
    requested_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    approved_at DATETIME,
    loan_date DATETIME,
    return_requested_at DATETIME,
    return_date DATETIME,
    status VARCHAR(32) NOT NULL,
    remark VARCHAR(500)
);

CREATE INDEX IF NOT EXISTS idx_loan_asset_status ON loan_record (asset_id, status);
CREATE INDEX IF NOT EXISTS idx_loan_applicant ON loan_record (applicant_id, requested_at);

CREATE TABLE IF NOT EXISTS transfer_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    asset_id BIGINT NOT NULL,
    from_department_id BIGINT NOT NULL,
    to_department_id BIGINT NOT NULL,
    from_owner_id BIGINT,
    to_owner_id BIGINT NOT NULL,
    reason VARCHAR(500) NOT NULL,
    transferred_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(32) NOT NULL DEFAULT 'COMPLETED'
);

CREATE INDEX IF NOT EXISTS idx_transfer_asset ON transfer_record (asset_id, transferred_at);
