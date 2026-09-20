import { computed, reactive } from 'vue'
import { nowISO, uid } from '@/utils/format'

const STORAGE_KEY = 'fixed-asset-demo-v1'

const assetStatusMeta = {
  idle: { label: '闲置', type: 'info', color: '#64748b' },
  in_use: { label: '在用', type: 'success', color: '#0f8a72' },
  maintenance: { label: '维修中', type: 'warning', color: '#d97706' },
  scrapped: { label: '报废', type: 'danger', color: '#b6473d' },
}

const loanStatusMeta = {
  pending: { label: '待处理', type: 'warning' },
  active: { label: '使用中', type: 'success' },
  return_pending: { label: '待归还确认', type: 'primary' },
  returned: { label: '已归还', type: 'info' },
  rejected: { label: '已驳回', type: 'danger' },
}

const transferStatusMeta = {
  completed: { label: '已完成', type: 'success' },
}

function buildSeedData() {
  return {
    departments: [
      { id: 'dept-1', name: '综合管理部', code: 'ADM', managerId: 'emp-4', status: 'active' },
      { id: 'dept-2', name: '研发部', code: 'RND', managerId: 'emp-1', status: 'active' },
      { id: 'dept-3', name: '生产部', code: 'PRD', managerId: 'emp-3', status: 'active' },
      { id: 'dept-4', name: '财务部', code: 'FIN', managerId: 'emp-2', status: 'active' },
    ],
    employees: [
      {
        id: 'emp-1',
        employeeNo: 'E1001',
        name: '张明',
        departmentId: 'dept-2',
        phone: '13800001001',
        email: 'zhangming@example.com',
        status: 'active',
        joinDate: '2022-03-15',
      },
      {
        id: 'emp-2',
        employeeNo: 'E1002',
        name: '李娜',
        departmentId: 'dept-4',
        phone: '13800001002',
        email: 'lina@example.com',
        status: 'active',
        joinDate: '2021-08-20',
      },
      {
        id: 'emp-3',
        employeeNo: 'E1003',
        name: '王强',
        departmentId: 'dept-3',
        phone: '13800001003',
        email: 'wangqiang@example.com',
        status: 'active',
        joinDate: '2020-05-10',
      },
      {
        id: 'emp-4',
        employeeNo: 'E1004',
        name: '赵敏',
        departmentId: 'dept-1',
        phone: '13800001004',
        email: 'zhaomin@example.com',
        status: 'active',
        joinDate: '2019-11-02',
      },
      {
        id: 'emp-5',
        employeeNo: 'E1005',
        name: '陈晨',
        departmentId: 'dept-2',
        phone: '13800001005',
        email: 'chenchen@example.com',
        status: 'active',
        joinDate: '2023-02-13',
      },
    ],
    categories: [
      { id: 'cat-1', name: '办公设备', parentId: null, code: 'OFFICE', status: 'active' },
      { id: 'cat-2', name: '生产设备', parentId: null, code: 'PROD', status: 'active' },
      { id: 'cat-3', name: '办公家具', parentId: null, code: 'FURN', status: 'active' },
      { id: 'cat-11', name: '计算机设备', parentId: 'cat-1', code: 'OFFICE-PC', status: 'active' },
      { id: 'cat-12', name: '打印设备', parentId: 'cat-1', code: 'OFFICE-PRINT', status: 'active' },
      { id: 'cat-21', name: '检测仪器', parentId: 'cat-2', code: 'PROD-TEST', status: 'active' },
      { id: 'cat-31', name: '桌椅家具', parentId: 'cat-3', code: 'FURN-TABLE', status: 'active' },
    ],
    assets: [
      {
        id: 'asset-1',
        assetNo: 'FA-2026-001',
        name: 'ThinkPad T14 笔记本电脑',
        categoryId: 'cat-11',
        brandModel: 'Lenovo ThinkPad T14',
        purchaseDate: '2024-01-12',
        originalValue: 8500,
        usefulLife: 5,
        departmentId: 'dept-2',
        ownerId: 'emp-1',
        status: 'in_use',
        remark: '研发日常开发使用',
        createdAt: '2026-01-12T09:00:00.000Z',
      },
      {
        id: 'asset-2',
        assetNo: 'FA-2026-002',
        name: '戴尔台式计算机',
        categoryId: 'cat-11',
        brandModel: 'Dell OptiPlex 7010',
        purchaseDate: '2024-03-08',
        originalValue: 5200,
        usefulLife: 5,
        departmentId: 'dept-1',
        ownerId: null,
        status: 'idle',
        remark: '备用办公设备',
        createdAt: '2026-03-08T09:00:00.000Z',
      },
      {
        id: 'asset-3',
        assetNo: 'FA-2026-003',
        name: '惠普激光打印机',
        categoryId: 'cat-12',
        brandModel: 'HP LaserJet M405',
        purchaseDate: '2023-09-16',
        originalValue: 3200,
        usefulLife: 5,
        departmentId: 'dept-1',
        ownerId: null,
        status: 'idle',
        remark: '一楼打印区备用',
        createdAt: '2026-09-16T09:00:00.000Z',
      },
      {
        id: 'asset-4',
        assetNo: 'FA-2026-004',
        name: 'MacBook Pro 开发笔记本',
        categoryId: 'cat-11',
        brandModel: 'Apple MacBook Pro 14',
        purchaseDate: '2024-05-20',
        originalValue: 16999,
        usefulLife: 5,
        departmentId: 'dept-2',
        ownerId: 'emp-5',
        status: 'in_use',
        remark: '移动端开发使用',
        createdAt: '2026-05-20T09:00:00.000Z',
      },
      {
        id: 'asset-5',
        assetNo: 'FA-2026-005',
        name: '高精度检测仪',
        categoryId: 'cat-21',
        brandModel: 'Keysight 34461A',
        purchaseDate: '2022-11-02',
        originalValue: 28000,
        usefulLife: 8,
        departmentId: 'dept-3',
        ownerId: 'emp-3',
        status: 'maintenance',
        remark: '待校准并更换接口模块',
        createdAt: '2026-11-02T09:00:00.000Z',
      },
      {
        id: 'asset-6',
        assetNo: 'FA-2026-006',
        name: '员工办公桌',
        categoryId: 'cat-31',
        brandModel: '定制 1.4 米',
        purchaseDate: '2023-04-10',
        originalValue: 1600,
        usefulLife: 8,
        departmentId: 'dept-1',
        ownerId: null,
        status: 'idle',
        remark: '仓库待分配',
        createdAt: '2026-04-10T09:00:00.000Z',
      },
      {
        id: 'asset-7',
        assetNo: 'FA-2026-007',
        name: '财务档案柜',
        categoryId: 'cat-31',
        brandModel: '钢制四门',
        purchaseDate: '2022-07-18',
        originalValue: 2400,
        usefulLife: 10,
        departmentId: 'dept-4',
        ownerId: 'emp-2',
        status: 'in_use',
        remark: '财务档案室专用',
        createdAt: '2026-07-18T09:00:00.000Z',
      },
      {
        id: 'asset-8',
        assetNo: 'FA-2026-008',
        name: '研发测试服务器',
        categoryId: 'cat-11',
        brandModel: 'Dell PowerEdge R750',
        purchaseDate: '2023-12-01',
        originalValue: 68000,
        usefulLife: 6,
        departmentId: 'dept-2',
        ownerId: 'emp-5',
        status: 'in_use',
        remark: '内部测试环境',
        createdAt: '2026-12-01T09:00:00.000Z',
      },
      {
        id: 'asset-9',
        assetNo: 'FA-2026-009',
        name: '旧款办公计算机',
        categoryId: 'cat-11',
        brandModel: 'HP ProDesk 400',
        purchaseDate: '2018-06-12',
        originalValue: 4300,
        usefulLife: 5,
        departmentId: 'dept-1',
        ownerId: null,
        status: 'scrapped',
        remark: '已停止使用，等待后续资产报废流程处理',
        createdAt: '2026-06-12T09:00:00.000Z',
      },
      {
        id: 'asset-10',
        assetNo: 'FA-2026-010',
        name: '生产校准设备',
        categoryId: 'cat-21',
        brandModel: 'Fluke 754',
        purchaseDate: '2023-03-25',
        originalValue: 42000,
        usefulLife: 8,
        departmentId: 'dept-3',
        ownerId: 'emp-3',
        status: 'in_use',
        remark: '生产现场使用',
        createdAt: '2026-03-25T09:00:00.000Z',
      },
    ],
    loanRecords: [
      {
        id: 'loan-1',
        assetId: 'asset-1',
        applicantId: 'emp-1',
        departmentId: 'dept-2',
        ownerId: 'emp-1',
        requestedAt: '2026-09-02T02:30:00.000Z',
        approvedAt: '2026-09-02T03:10:00.000Z',
        loanDate: '2026-09-02T03:10:00.000Z',
        returnDate: null,
        status: 'active',
        remark: '研发开发设备',
      },
      {
        id: 'loan-2',
        assetId: 'asset-4',
        applicantId: 'emp-5',
        departmentId: 'dept-2',
        ownerId: 'emp-5',
        requestedAt: '2026-08-18T01:20:00.000Z',
        approvedAt: '2026-08-18T02:00:00.000Z',
        loanDate: '2026-08-18T02:00:00.000Z',
        returnDate: null,
        status: 'active',
        remark: '移动端项目开发',
      },
      {
        id: 'loan-3',
        assetId: 'asset-8',
        applicantId: 'emp-5',
        departmentId: 'dept-2',
        ownerId: 'emp-5',
        requestedAt: '2026-07-01T03:00:00.000Z',
        approvedAt: '2026-07-01T04:00:00.000Z',
        loanDate: '2026-07-01T04:00:00.000Z',
        returnDate: null,
        status: 'return_pending',
        remark: '测试环境迁移，准备归还',
        returnRequestedAt: '2026-09-20T07:40:00.000Z',
      },
      {
        id: 'loan-4',
        assetId: 'asset-2',
        applicantId: 'emp-2',
        departmentId: 'dept-4',
        ownerId: null,
        requestedAt: '2026-09-21T01:30:00.000Z',
        approvedAt: null,
        loanDate: null,
        returnDate: null,
        status: 'pending',
        remark: '财务办公电脑替换',
      },
      {
        id: 'loan-5',
        assetId: 'asset-6',
        applicantId: 'emp-4',
        departmentId: 'dept-1',
        ownerId: 'emp-4',
        requestedAt: '2025-12-05T02:00:00.000Z',
        approvedAt: '2025-12-05T02:40:00.000Z',
        loanDate: '2025-12-05T02:40:00.000Z',
        returnDate: '2026-03-20T08:00:00.000Z',
        status: 'returned',
        remark: '工位调整后归还',
      },
    ],
    transferRecords: [
      {
        id: 'transfer-1',
        assetId: 'asset-10',
        fromDepartmentId: 'dept-1',
        toDepartmentId: 'dept-3',
        fromOwnerId: 'emp-4',
        toOwnerId: 'emp-3',
        reason: '生产现场校准任务增加',
        transferredAt: '2026-08-12T05:20:00.000Z',
        status: 'completed',
      },
      {
        id: 'transfer-2',
        assetId: 'asset-7',
        fromDepartmentId: 'dept-1',
        toDepartmentId: 'dept-4',
        fromOwnerId: 'emp-4',
        toOwnerId: 'emp-2',
        reason: '财务档案集中管理',
        transferredAt: '2026-07-05T03:40:00.000Z',
        status: 'completed',
      },
    ],
    changeRecords: [
      {
        id: 'change-1',
        assetId: 'asset-8',
        type: '归还申请',
        description: '陈晨提交了研发测试服务器的归还申请',
        operator: '陈晨',
        createdAt: '2026-09-20T07:40:00.000Z',
      },
      {
        id: 'change-2',
        assetId: 'asset-2',
        type: '领用申请',
        description: '李娜提交了戴尔台式计算机的领用申请',
        operator: '李娜',
        createdAt: '2026-09-21T01:30:00.000Z',
      },
      {
        id: 'change-3',
        assetId: 'asset-5',
        type: '状态调整',
        description: '高精度检测仪状态由闲置调整为维修中',
        operator: '系统管理员',
        createdAt: '2026-09-18T06:30:00.000Z',
      },
      {
        id: 'change-4',
        assetId: 'asset-10',
        type: '资产调拨',
        description: '生产校准设备由综合管理部调拨至生产部',
        operator: '系统管理员',
        createdAt: '2026-08-12T05:20:00.000Z',
      },
      {
        id: 'change-5',
        assetId: 'asset-1',
        type: '资产领用',
        description: '张明领用 ThinkPad T14 笔记本电脑',
        operator: '系统管理员',
        createdAt: '2026-09-02T03:10:00.000Z',
      },
    ],
    users: [
      {
        id: 'user-1',
        username: 'admin',
        password: '123456',
        realName: '系统管理员',
        roleId: 'role-admin',
        employeeId: null,
        status: 'active',
        phone: '13800000001',
        email: 'admin@example.com',
      },
      {
        id: 'user-2',
        username: 'employee',
        password: '123456',
        realName: '张明',
        roleId: 'role-employee',
        employeeId: 'emp-1',
        status: 'active',
        phone: '13800001001',
        email: 'zhangming@example.com',
      },
      {
        id: 'user-3',
        username: 'lina',
        password: '123456',
        realName: '李娜',
        roleId: 'role-employee',
        employeeId: 'emp-2',
        status: 'active',
        phone: '13800001002',
        email: 'lina@example.com',
      },
    ],
    roles: [
      {
        id: 'role-admin',
        name: '管理员',
        code: 'ADMIN',
        description: '拥有 MVP 范围内的系统管理与资产业务权限',
        permissions: [
          'dashboard',
          'departments',
          'employees',
          'categories',
          'assets',
          'loans',
          'transfers',
          'system',
          'profile',
        ],
        builtIn: true,
      },
      {
        id: 'role-employee',
        name: '普通员工',
        code: 'EMPLOYEE',
        description: '可提交领用与归还申请，并查看个人资产',
        permissions: ['my-assets', 'loans', 'profile'],
        builtIn: true,
      },
    ],
  }
}

function loadState() {
  try {
    const saved = localStorage.getItem(STORAGE_KEY)
    return saved ? JSON.parse(saved) : buildSeedData()
  } catch {
    return buildSeedData()
  }
}

const initialState = loadState()

export const demoState = reactive({
  ...initialState,
  currentUser: initialState.currentUser || null,
})

function persist() {
  localStorage.setItem(STORAGE_KEY, JSON.stringify({ ...demoState }))
}

export function resetDemo() {
  const seed = buildSeedData()
  Object.keys(demoState).forEach((key) => {
    delete demoState[key]
  })
  Object.assign(demoState, seed, { currentUser: null })
  persist()
}

export function login(username, password) {
  const user = demoState.users.find(
    (item) => item.username === username && item.password === password && item.status === 'active',
  )
  if (!user) {
    throw new Error('账号或密码错误')
  }
  const role = demoState.roles.find((item) => item.id === user.roleId)
  demoState.currentUser = {
    ...user,
    roleCode: role?.code || 'EMPLOYEE',
    roleName: role?.name || '普通员工',
    permissions: role?.permissions || [],
  }
  persist()
  return demoState.currentUser
}

export function logout() {
  demoState.currentUser = null
  persist()
}

export function switchDemoIdentity() {
  if (!demoState.currentUser) return null
  const targetUsername = demoState.currentUser.roleCode === 'ADMIN' ? 'employee' : 'admin'
  return login(targetUsername, '123456')
}

export function isAdmin() {
  return demoState.currentUser?.roleCode === 'ADMIN'
}

export function getDepartment(id) {
  return demoState.departments.find((item) => item.id === id)
}

export function getEmployee(id) {
  return demoState.employees.find((item) => item.id === id)
}

export function getCategory(id) {
  return demoState.categories.find((item) => item.id === id)
}

export function getAsset(id) {
  return demoState.assets.find((item) => item.id === id)
}

export function getCurrentEmployee() {
  if (!demoState.currentUser?.employeeId) return null
  return getEmployee(demoState.currentUser.employeeId)
}

export function departmentName(id) {
  return getDepartment(id)?.name || '-'
}

export function employeeName(id) {
  return getEmployee(id)?.name || '-'
}

export function categoryName(id) {
  return getCategory(id)?.name || '-'
}

export function addDepartment(payload) {
  const item = { id: uid('dept'), status: 'active', ...payload }
  demoState.departments.push(item)
  persist()
  return item
}

export function updateDepartment(id, payload) {
  const item = demoState.departments.find((row) => row.id === id)
  if (!item) throw new Error('部门不存在')
  Object.assign(item, payload)
  persist()
  return item
}

export function deleteDepartment(id) {
  const hasEmployee = demoState.employees.some((item) => item.departmentId === id)
  const hasAsset = demoState.assets.some((item) => item.departmentId === id)
  if (hasEmployee || hasAsset) throw new Error('该部门仍被员工或资产引用，不能删除')
  demoState.departments = demoState.departments.filter((item) => item.id !== id)
  persist()
}

export function addEmployee(payload) {
  const item = { id: uid('emp'), status: 'active', ...payload }
  demoState.employees.push(item)
  persist()
  return item
}

export function updateEmployee(id, payload) {
  const item = demoState.employees.find((row) => row.id === id)
  if (!item) throw new Error('员工不存在')
  Object.assign(item, payload)
  persist()
  return item
}

export function deleteEmployee(id) {
  const hasAsset = demoState.assets.some((item) => item.ownerId === id)
  const hasActiveLoan = demoState.loanRecords.some(
    (item) => ['active', 'return_pending'].includes(item.status) && item.ownerId === id,
  )
  if (hasAsset || hasActiveLoan) throw new Error('该员工仍关联资产或在用记录，不能删除')
  demoState.employees = demoState.employees.filter((item) => item.id !== id)
  persist()
}

export function addCategory(payload) {
  const item = { id: uid('cat'), status: 'active', ...payload }
  demoState.categories.push(item)
  persist()
  return item
}

export function updateCategory(id, payload) {
  const item = demoState.categories.find((row) => row.id === id)
  if (!item) throw new Error('分类不存在')
  Object.assign(item, payload)
  persist()
  return item
}

export function deleteCategory(id) {
  const hasChildren = demoState.categories.some((item) => item.parentId === id)
  const hasAssets = demoState.assets.some((item) => item.categoryId === id)
  if (hasChildren || hasAssets) throw new Error('该分类仍有子分类或关联资产，不能删除')
  demoState.categories = demoState.categories.filter((item) => item.id !== id)
  persist()
}

export function addAsset(payload) {
  if (demoState.assets.some((item) => item.assetNo === payload.assetNo)) {
    throw new Error('资产编号已存在')
  }
  const item = {
    id: uid('asset'),
    createdAt: nowISO(),
    ...payload,
  }
  demoState.assets.push(item)
  addChangeRecord(item.id, '资产登记', `${item.name}完成资产登记`, demoState.currentUser?.realName)
  persist()
  return item
}

export function updateAsset(id, payload) {
  const item = getAsset(id)
  if (!item) throw new Error('资产不存在')
  if (demoState.assets.some((row) => row.id !== id && row.assetNo === payload.assetNo)) {
    throw new Error('资产编号已存在')
  }
  if (payload.status === 'in_use' && (!payload.departmentId || !payload.ownerId)) {
    throw new Error('在用资产必须指定所属部门和负责人')
  }
  const oldStatus = item.status
  Object.assign(item, payload)
  if (oldStatus !== payload.status) {
    addChangeRecord(
      id,
      '状态调整',
      `${item.name}状态由${assetStatusMeta[oldStatus].label}调整为${assetStatusMeta[payload.status].label}`,
      demoState.currentUser?.realName,
    )
  }
  persist()
  return item
}

export function deleteAsset(id) {
  const asset = getAsset(id)
  if (!asset) throw new Error('资产不存在')
  const hasBusinessRecord =
    demoState.loanRecords.some((item) => item.assetId === id) ||
    demoState.transferRecords.some((item) => item.assetId === id)
  if (hasBusinessRecord || asset.status === 'in_use') {
    throw new Error('该资产已有在用关系或历史业务记录，不能删除')
  }
  demoState.assets = demoState.assets.filter((item) => item.id !== id)
  demoState.changeRecords = demoState.changeRecords.filter((item) => item.assetId !== id)
  persist()
}

export function submitLoanRequest({ assetId, applicantId, departmentId, remark }) {
  const asset = getAsset(assetId)
  if (!asset) throw new Error('资产不存在')
  if (asset.status !== 'idle') throw new Error('只有闲置资产可以申请领用')
  const duplicated = demoState.loanRecords.some(
    (item) => item.assetId === assetId && ['pending', 'active', 'return_pending'].includes(item.status),
  )
  if (duplicated) throw new Error('该资产已存在进行中的领用记录')
  const item = {
    id: uid('loan'),
    assetId,
    applicantId,
    departmentId,
    ownerId: null,
    requestedAt: nowISO(),
    approvedAt: null,
    loanDate: null,
    returnDate: null,
    status: 'pending',
    remark,
  }
  demoState.loanRecords.unshift(item)
  addChangeRecord(asset.id, '领用申请', `${employeeName(applicantId)}提交了${asset.name}的领用申请`, employeeName(applicantId))
  persist()
  return item
}

export function approveLoan(id) {
  const record = demoState.loanRecords.find((item) => item.id === id)
  if (!record || record.status !== 'pending') throw new Error('领用申请状态已变化')
  const asset = getAsset(record.assetId)
  if (!asset || asset.status !== 'idle') throw new Error('资产当前不可领用')
  const timestamp = nowISO()
  record.status = 'active'
  record.approvedAt = timestamp
  record.loanDate = timestamp
  record.ownerId = record.applicantId
  asset.status = 'in_use'
  asset.departmentId = record.departmentId
  asset.ownerId = record.applicantId
  addChangeRecord(
    asset.id,
    '资产领用',
    `${employeeName(record.applicantId)}领用${asset.name}`,
    demoState.currentUser?.realName,
  )
  persist()
  return record
}

export function rejectLoan(id, reason = '') {
  const record = demoState.loanRecords.find((item) => item.id === id)
  if (!record || record.status !== 'pending') throw new Error('领用申请状态已变化')
  record.status = 'rejected'
  record.remark = reason || record.remark
  const asset = getAsset(record.assetId)
  addChangeRecord(
    record.assetId,
    '领用驳回',
    `${employeeName(record.applicantId)}的${asset?.name || '资产'}领用申请被驳回`,
    demoState.currentUser?.realName,
  )
  persist()
  return record
}

export function requestReturn(id) {
  const record = demoState.loanRecords.find((item) => item.id === id)
  if (!record || record.status !== 'active') throw new Error('当前领用记录不可归还')
  record.status = 'return_pending'
  record.returnRequestedAt = nowISO()
  const asset = getAsset(record.assetId)
  addChangeRecord(
    record.assetId,
    '归还申请',
    `${employeeName(record.applicantId)}提交了${asset?.name || '资产'}的归还申请`,
    employeeName(record.applicantId),
  )
  persist()
  return record
}

export function confirmReturn(id) {
  const record = demoState.loanRecords.find((item) => item.id === id)
  if (!record || record.status !== 'return_pending') throw new Error('归还申请状态已变化')
  const asset = getAsset(record.assetId)
  if (!asset) throw new Error('资产不存在')
  record.status = 'returned'
  record.returnDate = nowISO()
  asset.status = 'idle'
  asset.ownerId = null
  addChangeRecord(
    asset.id,
    '资产归还',
    `${employeeName(record.ownerId)}归还${asset.name}`,
    demoState.currentUser?.realName,
  )
  persist()
  return record
}

export function createTransfer({ assetId, toDepartmentId, toOwnerId, reason }) {
  const asset = getAsset(assetId)
  if (!asset) throw new Error('资产不存在')
  if (asset.status !== 'in_use') throw new Error('只有在用资产可以调拨')
  if (asset.departmentId === toDepartmentId && asset.ownerId === toOwnerId) {
    throw new Error('目标部门和负责人不能与当前归属相同')
  }
  const record = {
    id: uid('transfer'),
    assetId,
    fromDepartmentId: asset.departmentId,
    toDepartmentId,
    fromOwnerId: asset.ownerId,
    toOwnerId,
    reason,
    transferredAt: nowISO(),
    status: 'completed',
  }
  demoState.transferRecords.unshift(record)
  asset.departmentId = toDepartmentId
  asset.ownerId = toOwnerId
  addChangeRecord(
    asset.id,
    '资产调拨',
    `${asset.name}由${departmentName(record.fromDepartmentId)}调拨至${departmentName(toDepartmentId)}`,
    demoState.currentUser?.realName,
  )
  persist()
  return record
}

export function addChangeRecord(assetId, type, description, operator = '系统') {
  demoState.changeRecords.unshift({
    id: uid('change'),
    assetId,
    type,
    description,
    operator,
    createdAt: nowISO(),
  })
  demoState.changeRecords = demoState.changeRecords.slice(0, 100)
}

export function addUser(payload) {
  if (demoState.users.some((item) => item.username === payload.username)) {
    throw new Error('登录账号已存在')
  }
  const item = { id: uid('user'), status: 'active', ...payload }
  demoState.users.push(item)
  persist()
  return item
}

export function updateUser(id, payload) {
  const item = demoState.users.find((row) => row.id === id)
  if (!item) throw new Error('用户不存在')
  if (demoState.users.some((row) => row.id !== id && row.username === payload.username)) {
    throw new Error('登录账号已存在')
  }
  Object.assign(item, payload)
  if (demoState.currentUser?.id === id) {
    Object.assign(demoState.currentUser, payload)
  }
  persist()
  return item
}

export function updateRole(id, payload) {
  const item = demoState.roles.find((row) => row.id === id)
  if (!item) throw new Error('角色不存在')
  Object.assign(item, payload)
  if (demoState.currentUser?.roleId === id) {
    Object.assign(demoState.currentUser, {
      roleName: item.name,
      roleCode: item.code,
      permissions: [...item.permissions],
    })
  }
  persist()
  return item
}

export function updateProfile(payload) {
  if (!demoState.currentUser) throw new Error('请先登录')
  const user = demoState.users.find((item) => item.id === demoState.currentUser.id)
  if (!user) throw new Error('用户不存在')
  Object.assign(user, {
    realName: payload.realName,
    phone: payload.phone,
    email: payload.email,
  })
  const employee = demoState.currentUser.employeeId
    ? getEmployee(demoState.currentUser.employeeId)
    : null
  if (employee) {
    employee.name = payload.realName
    employee.phone = payload.phone
    employee.email = payload.email
  }
  Object.assign(demoState.currentUser, {
    realName: payload.realName,
    phone: payload.phone,
    email: payload.email,
  })
  persist()
}

export const assetStatuses = Object.entries(assetStatusMeta).map(([value, meta]) => ({
  value,
  ...meta,
}))

export const loanStatuses = Object.entries(loanStatusMeta).map(([value, meta]) => ({
  value,
  ...meta,
}))

export const transferStatuses = Object.entries(transferStatusMeta).map(([value, meta]) => ({
  value,
  ...meta,
}))

export function getAssetStatusMeta(status) {
  return assetStatusMeta[status] || { label: status, type: 'info' }
}

export function getLoanStatusMeta(status) {
  return loanStatusMeta[status] || { label: status, type: 'info' }
}

export function getTransferStatusMeta(status) {
  return transferStatusMeta[status] || { label: status, type: 'info' }
}

export const dashboardStats = computed(() => {
  const total = demoState.assets.length
  const countByStatus = (status) => demoState.assets.filter((item) => item.status === status).length
  const departmentCounts = demoState.departments.map((department) => ({
    name: department.name,
    value: demoState.assets.filter((item) => item.departmentId === department.id).length,
  }))
  const statusCounts = assetStatuses.map((status) => ({
    name: status.label,
    value: countByStatus(status.value),
    color: status.color,
  }))
  return {
    total,
    idle: countByStatus('idle'),
    inUse: countByStatus('in_use'),
    maintenance: countByStatus('maintenance'),
    scrapped: countByStatus('scrapped'),
    originalValue: demoState.assets.reduce((sum, item) => sum + Number(item.originalValue || 0), 0),
    departmentCounts,
    statusCounts,
  }
})

export const myAssets = computed(() => {
  const employee = getCurrentEmployee()
  if (!employee) return []
  return demoState.assets.filter((item) => item.ownerId === employee.id && item.status === 'in_use')
})

export function canAccess(permission) {
  return Boolean(demoState.currentUser?.permissions?.includes(permission))
}
