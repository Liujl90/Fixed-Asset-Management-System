CREATE TABLE IF NOT EXISTS supplier (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    code VARCHAR(64) NOT NULL,
    contact_name VARCHAR(64),
    phone VARCHAR(32),
    email VARCHAR(128),
    address VARCHAR(255),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_supplier_code UNIQUE (code)
);

CREATE TABLE IF NOT EXISTS purchase_order (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_no VARCHAR(64) NOT NULL,
    supplier_id BIGINT NOT NULL,
    applicant_id BIGINT NOT NULL,
    order_date DATE NOT NULL,
    expected_date DATE,
    total_amount DECIMAL(14, 2) NOT NULL DEFAULT 0,
    status VARCHAR(32) NOT NULL,
    remark VARCHAR(500),
    approved_by BIGINT,
    approved_at DATETIME,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_purchase_order_no UNIQUE (order_no)
);

CREATE INDEX IF NOT EXISTS idx_purchase_supplier ON purchase_order (supplier_id);
CREATE INDEX IF NOT EXISTS idx_purchase_status ON purchase_order (status, order_date);

CREATE TABLE IF NOT EXISTS purchase_order_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    purchase_order_id BIGINT NOT NULL,
    asset_name VARCHAR(128) NOT NULL,
    category_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    unit_price DECIMAL(14, 2) NOT NULL,
    amount DECIMAL(14, 2) NOT NULL,
    remark VARCHAR(255)
);

CREATE INDEX IF NOT EXISTS idx_purchase_item_order ON purchase_order_item (purchase_order_id);

CREATE TABLE IF NOT EXISTS inbound_order (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    inbound_no VARCHAR(64) NOT NULL,
    purchase_order_id BIGINT,
    supplier_id BIGINT NOT NULL,
    warehouse_name VARCHAR(128) NOT NULL,
    inbound_date DATE NOT NULL,
    status VARCHAR(32) NOT NULL,
    operator_id BIGINT NOT NULL,
    remark VARCHAR(500),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_inbound_order_no UNIQUE (inbound_no)
);

CREATE INDEX IF NOT EXISTS idx_inbound_status ON inbound_order (status, inbound_date);

CREATE TABLE IF NOT EXISTS inbound_order_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    inbound_order_id BIGINT NOT NULL,
    asset_name VARCHAR(128) NOT NULL,
    category_id BIGINT NOT NULL,
    brand_model VARCHAR(128),
    quantity INT NOT NULL,
    unit_price DECIMAL(14, 2) NOT NULL,
    department_id BIGINT NOT NULL,
    remark VARCHAR(255)
);

CREATE INDEX IF NOT EXISTS idx_inbound_item_order ON inbound_order_item (inbound_order_id);

CREATE TABLE IF NOT EXISTS maintenance_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    maintenance_no VARCHAR(64) NOT NULL,
    asset_id BIGINT NOT NULL,
    asset_status_before VARCHAR(32) NOT NULL,
    maintenance_type VARCHAR(32) NOT NULL,
    description VARCHAR(500) NOT NULL,
    cost DECIMAL(14, 2) NOT NULL DEFAULT 0,
    start_date DATE,
    end_date DATE,
    status VARCHAR(32) NOT NULL,
    operator_id BIGINT NOT NULL,
    result VARCHAR(500),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_maintenance_record_no UNIQUE (maintenance_no)
);

CREATE INDEX IF NOT EXISTS idx_maintenance_record_asset ON maintenance_record (asset_id, status);

CREATE TABLE IF NOT EXISTS inventory_check (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    check_no VARCHAR(64) NOT NULL,
    check_name VARCHAR(128) NOT NULL,
    department_id BIGINT,
    check_date DATE NOT NULL,
    status VARCHAR(32) NOT NULL,
    operator_id BIGINT NOT NULL,
    total_count INT NOT NULL DEFAULT 0,
    normal_count INT NOT NULL DEFAULT 0,
    abnormal_count INT NOT NULL DEFAULT 0,
    remark VARCHAR(500),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_inventory_check_no UNIQUE (check_no)
);

CREATE TABLE IF NOT EXISTS inventory_check_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    inventory_check_id BIGINT NOT NULL,
    asset_id BIGINT NOT NULL,
    expected_department_id BIGINT,
    actual_department_id BIGINT,
    expected_status VARCHAR(32) NOT NULL,
    actual_status VARCHAR(32),
    result VARCHAR(32) NOT NULL,
    remark VARCHAR(255)
);

CREATE INDEX IF NOT EXISTS idx_inventory_item_check ON inventory_check_item (inventory_check_id);

CREATE TABLE IF NOT EXISTS scrap_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    scrap_no VARCHAR(64) NOT NULL,
    asset_id BIGINT NOT NULL,
    reason VARCHAR(500) NOT NULL,
    applicant_id BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL,
    approved_by BIGINT,
    approved_at DATETIME,
    disposal_method VARCHAR(128),
    disposal_amount DECIMAL(14, 2),
    completed_at DATETIME,
    remark VARCHAR(500),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_scrap_record_no UNIQUE (scrap_no)
);

CREATE INDEX IF NOT EXISTS idx_scrap_asset_status ON scrap_record (asset_id, status);

INSERT INTO supplier (id, name, code, contact_name, phone, email, address, status) VALUES
    (1, '联想企业采购', 'SUP-LENOVO', '王经理', '010-88880001', 'lenovo@example.com', '北京市海淀区', 'ACTIVE'),
    (2, '京东企业购', 'SUP-JD', '李经理', '010-88880002', 'jd@example.com', '北京市大兴区', 'ACTIVE');

INSERT INTO purchase_order (
    id, order_no, supplier_id, applicant_id, order_date, expected_date, total_amount,
    status, remark, approved_by, approved_at
) VALUES
    (1, 'PO-2026-001', 1, 4, '2026-09-18', '2026-09-30', 17000.00,
     'APPROVED', '研发设备补充采购', 1, '2026-09-19 10:00:00');

INSERT INTO purchase_order_item (
    purchase_order_id, asset_name, category_id, quantity, unit_price, amount, remark
) VALUES
    (1, 'ThinkPad E14 笔记本', 4, 2, 6000.00, 12000.00, '研发备用电脑'),
    (1, '27 英寸显示器', 4, 1, 5000.00, 5000.00, '开发工位使用');

INSERT INTO inbound_order (
    id, inbound_no, purchase_order_id, supplier_id, warehouse_name, inbound_date,
    status, operator_id, remark
) VALUES
    (1, 'IN-2026-001', 1, 1, '总部资产库', '2026-09-22', 'DRAFT', 2, '等待确认入库');

INSERT INTO inbound_order_item (
    inbound_order_id, asset_name, category_id, brand_model, quantity, unit_price, department_id, remark
) VALUES
    (1, 'ThinkPad E14 笔记本', 4, 'Lenovo ThinkPad E14', 2, 6000.00, 2, '研发部'),
    (1, '27 英寸显示器', 4, 'Dell P2725H', 1, 5000.00, 2, '研发部');

INSERT INTO maintenance_record (
    id, maintenance_no, asset_id, asset_status_before, maintenance_type, description,
    cost, start_date, end_date, status, operator_id, result
) VALUES
    (1, 'MT-2026-001', 5, 'IDLE', 'REPAIR', '接口模块故障维修', 1800.00,
     '2026-09-10', NULL, 'PROCESSING', 2, NULL);

INSERT INTO inventory_check (
    id, check_no, check_name, department_id, check_date, status, operator_id,
    total_count, normal_count, abnormal_count, remark
) VALUES
    (1, 'IC-2026-001', '研发部季度盘点', 2, '2026-09-22', 'DRAFT', 2, 0, 0, 0, '季度常规盘点');

INSERT INTO inventory_check_item (
    inventory_check_id, asset_id, expected_department_id, actual_department_id,
    expected_status, actual_status, result, remark
) VALUES
    (1, 1, 2, NULL, 'IN_USE', NULL, 'PENDING', NULL),
    (1, 4, 2, NULL, 'IN_USE', NULL, 'PENDING', NULL),
    (1, 8, 2, NULL, 'IN_USE', NULL, 'PENDING', NULL);

INSERT INTO scrap_record (
    id, scrap_no, asset_id, reason, applicant_id, status, approved_by, approved_at,
    disposal_method, disposal_amount, completed_at, remark
) VALUES
    (1, 'SC-2026-001', 9, '设备老化且维修价值低', 4, 'COMPLETED', 1,
     '2026-09-01 10:00:00', '环保回收', 100.00, '2026-09-02 15:00:00', '已完成处置');

INSERT INTO sys_permission (name, code, module)
SELECT '供应商查看', 'supplier:read', 'supply'
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'supplier:read');

INSERT INTO sys_permission (name, code, module)
SELECT '供应商维护', 'supplier:write', 'supply'
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'supplier:write');

INSERT INTO sys_permission (name, code, module)
SELECT '采购查看', 'purchase:read', 'supply'
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'purchase:read');

INSERT INTO sys_permission (name, code, module)
SELECT '采购维护', 'purchase:write', 'supply'
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'purchase:write');

INSERT INTO sys_permission (name, code, module)
SELECT '采购审核', 'purchase:approve', 'supply'
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'purchase:approve');

INSERT INTO sys_permission (name, code, module)
SELECT '入库查看', 'inbound:read', 'supply'
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'inbound:read');

INSERT INTO sys_permission (name, code, module)
SELECT '入库维护', 'inbound:write', 'supply'
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'inbound:write');

INSERT INTO sys_permission (name, code, module)
SELECT '盘点查看', 'inventory:read', 'operation'
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'inventory:read');

INSERT INTO sys_permission (name, code, module)
SELECT '盘点维护', 'inventory:write', 'operation'
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'inventory:write');

INSERT INTO sys_permission (name, code, module)
SELECT '报废查看', 'scrap:read', 'operation'
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'scrap:read');

INSERT INTO sys_permission (name, code, module)
SELECT '报废维护', 'scrap:write', 'operation'
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'scrap:write');

INSERT INTO sys_permission (name, code, module)
SELECT '报废审核', 'scrap:approve', 'operation'
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'scrap:approve');

INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 1, id FROM sys_permission
WHERE code IN (
    'supplier:read', 'supplier:write', 'purchase:read', 'purchase:write', 'purchase:approve',
    'inbound:read', 'inbound:write', 'inventory:read', 'inventory:write',
    'scrap:read', 'scrap:write', 'scrap:approve'
)
AND NOT EXISTS (
    SELECT 1 FROM sys_role_permission rp
    WHERE rp.role_id = 1 AND rp.permission_id = sys_permission.id
);

INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 2, id FROM sys_permission
WHERE code IN (
    'supplier:read', 'supplier:write', 'purchase:read', 'purchase:write',
    'inbound:read', 'inbound:write', 'inventory:read', 'inventory:write',
    'scrap:read', 'scrap:write', 'scrap:approve'
)
AND NOT EXISTS (
    SELECT 1 FROM sys_role_permission rp
    WHERE rp.role_id = 2 AND rp.permission_id = sys_permission.id
);

INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 3, id FROM sys_permission
WHERE code IN ('supplier:read', 'purchase:read', 'inbound:read', 'inventory:read', 'scrap:read')
AND NOT EXISTS (
    SELECT 1 FROM sys_role_permission rp
    WHERE rp.role_id = 3 AND rp.permission_id = sys_permission.id
);
