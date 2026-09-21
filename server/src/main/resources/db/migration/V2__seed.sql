INSERT INTO department (id, name, code, manager_id, status) VALUES
    (1, '综合管理部', 'ADM', NULL, 'ACTIVE'),
    (2, '研发部', 'RND', NULL, 'ACTIVE'),
    (3, '生产部', 'PRD', NULL, 'ACTIVE'),
    (4, '财务部', 'FIN', NULL, 'ACTIVE');

INSERT INTO employee (id, employee_no, name, department_id, phone, email, join_date, status) VALUES
    (1, 'E1001', '张明', 2, '13800001001', 'zhangming@example.com', '2022-03-15', 'ACTIVE'),
    (2, 'E1002', '李娜', 4, '13800001002', 'lina@example.com', '2021-08-20', 'ACTIVE'),
    (3, 'E1003', '王强', 3, '13800001003', 'wangqiang@example.com', '2020-05-10', 'ACTIVE'),
    (4, 'E1004', '赵敏', 1, '13800001004', 'zhaomin@example.com', '2019-11-02', 'ACTIVE'),
    (5, 'E1005', '陈晨', 2, '13800001005', 'chenchen@example.com', '2023-02-13', 'ACTIVE');

UPDATE department SET manager_id = 4 WHERE id = 1;
UPDATE department SET manager_id = 1 WHERE id = 2;
UPDATE department SET manager_id = 3 WHERE id = 3;
UPDATE department SET manager_id = 2 WHERE id = 4;

INSERT INTO sys_role (id, name, code, description) VALUES
    (1, '管理员', 'ADMIN', '拥有系统管理和全部业务权限'),
    (2, '资产管理员', 'ASSET_MANAGER', '负责资产全生命周期业务'),
    (3, '普通员工', 'EMPLOYEE', '可提交领用和归还申请');

INSERT INTO sys_permission (id, name, code, module) VALUES
    (1, '资产总览查看', 'dashboard:read', 'dashboard'),
    (2, '部门查看', 'department:read', 'organization'),
    (3, '部门维护', 'department:write', 'organization'),
    (4, '员工查看', 'employee:read', 'organization'),
    (5, '员工维护', 'employee:write', 'organization'),
    (6, '分类查看', 'category:read', 'asset'),
    (7, '分类维护', 'category:write', 'asset'),
    (8, '资产查看', 'asset:read', 'asset'),
    (9, '资产维护', 'asset:write', 'asset'),
    (10, '领用查看', 'loan:read', 'lifecycle'),
    (11, '领用审批', 'loan:manage', 'lifecycle'),
    (12, '领用申请', 'loan:apply', 'lifecycle'),
    (13, '归还申请', 'loan:return', 'lifecycle'),
    (14, '调拨查看', 'transfer:read', 'lifecycle'),
    (15, '调拨维护', 'transfer:write', 'lifecycle'),
    (16, '用户查看', 'user:read', 'system'),
    (17, '用户维护', 'user:write', 'system'),
    (18, '角色查看', 'role:read', 'system'),
    (19, '角色维护', 'role:write', 'system'),
    (20, '日志查看', 'log:read', 'system'),
    (21, '个人信息维护', 'profile:update', 'profile');

INSERT INTO sys_role_permission (role_id, permission_id)
SELECT 1, id FROM sys_permission;

INSERT INTO sys_role_permission (role_id, permission_id) VALUES
    (2, 1), (2, 2), (2, 4), (2, 5), (2, 6), (2, 7), (2, 8), (2, 9),
    (2, 10), (2, 11), (2, 14), (2, 15), (2, 20), (2, 21);

INSERT INTO sys_role_permission (role_id, permission_id) VALUES
    (3, 1), (3, 2), (3, 4), (3, 6), (3, 8), (3, 10), (3, 12), (3, 13), (3, 14), (3, 21);

INSERT INTO sys_user (id, username, password, real_name, phone, email, employee_id, status) VALUES
    (1, 'admin', 'INIT:123456', '系统管理员', '13800000001', 'admin@example.com', NULL, 'ACTIVE'),
    (2, 'manager', 'INIT:123456', '资产管理员', '13800000002', 'manager@example.com', 4, 'ACTIVE'),
    (3, 'employee', 'INIT:123456', '张明', '13800001001', 'zhangming@example.com', 1, 'ACTIVE');

INSERT INTO sys_user_role (user_id, role_id) VALUES
    (1, 1), (2, 2), (3, 3);

INSERT INTO asset_category (id, name, code, parent_id, status) VALUES
    (1, '办公设备', 'OFFICE', NULL, 'ACTIVE'),
    (2, '生产设备', 'PROD', NULL, 'ACTIVE'),
    (3, '办公家具', 'FURN', NULL, 'ACTIVE'),
    (4, '计算机设备', 'OFFICE-PC', 1, 'ACTIVE'),
    (5, '打印设备', 'OFFICE-PRINT', 1, 'ACTIVE'),
    (6, '检测仪器', 'PROD-TEST', 2, 'ACTIVE'),
    (7, '桌椅家具', 'FURN-TABLE', 3, 'ACTIVE');

INSERT INTO asset (id, asset_no, name, category_id, brand_model, purchase_date, original_value, useful_life, department_id, owner_id, status, remark) VALUES
    (1, 'FA-2026-001', 'ThinkPad T14 笔记本电脑', 4, 'Lenovo ThinkPad T14', '2024-01-12', 8500, 5, 2, 1, 'IN_USE', '研发日常开发使用'),
    (2, 'FA-2026-002', '戴尔台式计算机', 4, 'Dell OptiPlex 7010', '2024-03-08', 5200, 5, 1, NULL, 'IDLE', '备用办公设备'),
    (3, 'FA-2026-003', '惠普激光打印机', 5, 'HP LaserJet M405', '2023-09-16', 3200, 5, 1, NULL, 'IDLE', '一楼打印区备用'),
    (4, 'FA-2026-004', 'MacBook Pro 开发笔记本', 4, 'Apple MacBook Pro 14', '2024-05-20', 16999, 5, 2, 5, 'IN_USE', '移动端开发使用'),
    (5, 'FA-2026-005', '高精度检测仪', 6, 'Keysight 34461A', '2022-11-02', 28000, 8, 3, 3, 'MAINTENANCE', '待校准'),
    (6, 'FA-2026-006', '员工办公桌', 7, '定制 1.4 米', '2023-04-10', 1600, 8, 1, NULL, 'IDLE', '仓库待分配'),
    (7, 'FA-2026-007', '财务档案柜', 7, '钢制四门', '2022-07-18', 2400, 10, 4, 2, 'IN_USE', '财务档案室专用'),
    (8, 'FA-2026-008', '研发测试服务器', 4, 'Dell PowerEdge R750', '2023-12-01', 68000, 6, 2, 5, 'IN_USE', '内部测试环境'),
    (9, 'FA-2026-009', '旧款办公计算机', 4, 'HP ProDesk 400', '2018-06-12', 4300, 5, 1, NULL, 'SCRAPPED', '已停止使用'),
    (10, 'FA-2026-010', '生产校准设备', 6, 'Fluke 754', '2023-03-25', 42000, 8, 3, 3, 'IN_USE', '生产现场使用');

INSERT INTO loan_record (id, asset_id, applicant_id, department_id, owner_id, requested_at, approved_at, loan_date, return_requested_at, return_date, status, remark) VALUES
    (1, 1, 1, 2, 1, '2026-09-02 10:30:00', '2026-09-02 11:10:00', '2026-09-02 11:10:00', NULL, NULL, 'ACTIVE', '研发开发设备'),
    (2, 4, 5, 2, 5, '2026-08-18 09:20:00', '2026-08-18 10:00:00', '2026-08-18 10:00:00', NULL, NULL, 'ACTIVE', '移动端项目开发'),
    (3, 8, 5, 2, 5, '2026-07-01 11:00:00', '2026-07-01 12:00:00', '2026-07-01 12:00:00', '2026-09-20 15:40:00', NULL, 'RETURN_PENDING', '准备归还'),
    (4, 2, 2, 4, NULL, '2026-09-21 09:30:00', NULL, NULL, NULL, NULL, 'PENDING', '财务办公电脑替换');

INSERT INTO transfer_record (id, asset_id, from_department_id, to_department_id, from_owner_id, to_owner_id, reason, transferred_at, status) VALUES
    (1, 10, 1, 3, 4, 3, '生产现场校准任务增加', '2026-08-12 13:20:00', 'COMPLETED'),
    (2, 7, 1, 4, 4, 2, '财务档案集中管理', '2026-07-05 11:40:00', 'COMPLETED');

INSERT INTO asset_change_record (asset_id, type, description, operator, created_at) VALUES
    (1, '资产领用', '张明领用 ThinkPad T14 笔记本电脑', '系统管理员', '2026-09-02 11:10:00'),
    (8, '归还申请', '陈晨提交了研发测试服务器的归还申请', '陈晨', '2026-09-20 15:40:00'),
    (2, '领用申请', '李娜提交了戴尔台式计算机的领用申请', '李娜', '2026-09-21 09:30:00'),
    (5, '状态调整', '高精度检测仪状态调整为维修中', '系统管理员', '2026-09-18 14:30:00'),
    (10, '资产调拨', '生产校准设备由综合管理部调拨至生产部', '系统管理员', '2026-08-12 13:20:00');
