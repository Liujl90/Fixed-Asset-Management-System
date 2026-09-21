CREATE TABLE IF NOT EXISTS maintenance_plan (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    asset_id BIGINT NOT NULL,
    plan_name VARCHAR(128) NOT NULL,
    maintenance_type VARCHAR(32) NOT NULL,
    plan_date DATE NOT NULL,
    cycle_months INT,
    status VARCHAR(32) NOT NULL,
    completed_at DATETIME,
    remark VARCHAR(500),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_maintenance_asset ON maintenance_plan (asset_id);
CREATE INDEX IF NOT EXISTS idx_maintenance_status_date ON maintenance_plan (status, plan_date);

CREATE TABLE IF NOT EXISTS depreciation_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    asset_id BIGINT NOT NULL,
    depreciation_month VARCHAR(7) NOT NULL,
    original_value DECIMAL(14, 2) NOT NULL,
    monthly_amount DECIMAL(14, 2) NOT NULL,
    accumulated_amount DECIMAL(14, 2) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_depreciation_asset_month UNIQUE (asset_id, depreciation_month)
);

CREATE INDEX IF NOT EXISTS idx_depreciation_month ON depreciation_record (depreciation_month);

INSERT INTO maintenance_plan (
    asset_id, plan_name, maintenance_type, plan_date, cycle_months, status, remark
) VALUES
    (5, '高精度检测仪年度校准', 'CALIBRATION', '2026-10-15', 12, 'PENDING', '年度校准计划'),
    (8, '研发测试服务器巡检', 'INSPECTION', '2026-09-28', 3, 'PENDING', '检查磁盘和散热'),
    (10, '生产校准设备季度保养', 'MAINTENANCE', '2026-09-30', 3, 'PENDING', '现场设备季度保养');

INSERT INTO sys_permission (name, code, module)
SELECT '保养查看', 'maintenance:read', 'operation'
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'maintenance:read');

INSERT INTO sys_permission (name, code, module)
SELECT '保养维护', 'maintenance:write', 'operation'
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'maintenance:write');

INSERT INTO sys_permission (name, code, module)
SELECT '折旧查看', 'depreciation:read', 'operation'
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'depreciation:read');

INSERT INTO sys_permission (name, code, module)
SELECT '折旧执行', 'depreciation:run', 'operation'
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE code = 'depreciation:run');

INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 1, id FROM sys_permission
WHERE code IN ('maintenance:read', 'maintenance:write', 'depreciation:read', 'depreciation:run')
  AND NOT EXISTS (
      SELECT 1 FROM sys_role_permission rp
      WHERE rp.role_id = 1 AND rp.permission_id = sys_permission.id
  );

INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 2, id FROM sys_permission
WHERE code IN ('maintenance:read', 'maintenance:write', 'depreciation:read', 'depreciation:run')
  AND NOT EXISTS (
      SELECT 1 FROM sys_role_permission rp
      WHERE rp.role_id = 2 AND rp.permission_id = sys_permission.id
  );
