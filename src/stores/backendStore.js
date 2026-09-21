import { computed, reactive } from 'vue'
import { api, TOKEN_KEY, USER_KEY } from '@/api/http'

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

const menuActionMap = {
  dashboard: ['dashboard:read'],
  departments: ['department:read', 'department:write'],
  employees: ['employee:read', 'employee:write'],
  categories: ['category:read', 'category:write'],
  assets: ['asset:read', 'asset:write'],
  loans: ['loan:read', 'loan:manage', 'loan:apply', 'loan:return'],
  transfers: ['transfer:read', 'transfer:write'],
  system: ['user:read', 'user:write', 'role:read', 'role:write', 'log:read'],
  'my-assets': ['asset:read', 'loan:read', 'loan:return'],
  profile: ['profile:update'],
}

function emptyDashboard() {
  return {
    total: 0,
    idle: 0,
    inUse: 0,
    maintenance: 0,
    scrapped: 0,
    originalValue: 0,
    departmentCounts: [],
    statusCounts: [],
    recentChanges: [],
  }
}

const storedUser = localStorage.getItem(USER_KEY)

export const demoState = reactive({
  departments: [],
  employees: [],
  categories: [],
  assets: [],
  loanRecords: [],
  transferRecords: [],
  changeRecords: [],
  users: [],
  roles: [],
  permissions: [],
  dashboardSummary: emptyDashboard(),
  currentUser: storedUser ? JSON.parse(storedUser) : null,
  loaded: false,
})

function normalizeStatus(status) {
  return status ? String(status).toLowerCase() : status
}

function normalizeDepartment(item) {
  return { ...item, status: normalizeStatus(item.status) }
}

function normalizeEmployee(item) {
  return { ...item, status: normalizeStatus(item.status) }
}

function normalizeCategory(item) {
  return { ...item, status: normalizeStatus(item.status) }
}

function normalizeAsset(item) {
  return { ...item, status: normalizeStatus(item.status) }
}

function normalizeLoan(item) {
  return { ...item, status: normalizeStatus(item.status) }
}

function normalizeTransfer(item) {
  return { ...item, status: normalizeStatus(item.status) }
}

function normalizeUser(user) {
  const roles = user.roles || []
  const roleCode = roles[0] || 'EMPLOYEE'
  const roleNames = {
    ADMIN: '管理员',
    ASSET_MANAGER: '资产管理员',
    EMPLOYEE: '普通员工',
  }
  return {
    ...user,
    roleCode,
    roleName: roleNames[roleCode] || roleCode,
    permissions: user.permissions || [],
  }
}

function pageRecords(payload) {
  if (Array.isArray(payload)) return payload
  return payload?.records || []
}

function hasPermission(permission) {
  if (!demoState.currentUser) return false
  if (demoState.currentUser.roleCode === 'ADMIN') return true
  return demoState.currentUser.permissions?.includes(permission)
}

function canRead(menuCode) {
  return menuActionMap[menuCode]?.some((permission) => hasPermission(permission)) || false
}

async function safeLoad(permission, loader) {
  if (!hasPermission(permission)) return
  await loader()
}

export async function loadAll() {
  const tasks = [
    safeLoad('department:read', async () => {
      demoState.departments = (await api.get('/departments')).map(normalizeDepartment)
    }),
    safeLoad('employee:read', async () => {
      demoState.employees = (await api.get('/employees')).map(normalizeEmployee)
    }),
    safeLoad('category:read', async () => {
      demoState.categories = (await api.get('/categories')).map(normalizeCategory)
    }),
    safeLoad('asset:read', async () => {
      const payload = await api.get('/assets', { params: { page: 1, size: 100 } })
      demoState.assets = pageRecords(payload).map(normalizeAsset)
    }),
    safeLoad('loan:read', async () => {
      const payload = await api.get('/loans', { params: { page: 1, size: 100 } })
      demoState.loanRecords = pageRecords(payload).map(normalizeLoan)
    }),
    safeLoad('transfer:read', async () => {
      const payload = await api.get('/transfers', { params: { page: 1, size: 100 } })
      demoState.transferRecords = pageRecords(payload).map(normalizeTransfer)
    }),
    safeLoad('dashboard:read', async () => {
      const summary = await api.get('/dashboard/summary')
      demoState.dashboardSummary = summary
      demoState.changeRecords = summary.recentChanges || []
    }),
  ]
  await Promise.all(tasks)

  if (canRead('system')) {
    demoState.users = (await api.get('/system/users')).map((user) => ({
      ...user,
      employeeName: getEmployee(user.employeeId)?.name || '未关联员工',
    }))
    demoState.roles = await api.get('/system/roles')
    demoState.permissions = await api.get('/system/permissions')
    demoState.roles = await Promise.all(
      demoState.roles.map(async (role) => {
        const permissionIds = await api.get(`/system/roles/${role.id}/permissions`)
        const codes = demoState.permissions
          .filter((permission) => permissionIds.includes(permission.id))
          .map((permission) => permission.code)
        return {
          ...role,
          permissionIds,
          permissions: Object.entries(menuActionMap)
            .filter(([, actions]) => actions.some((action) => codes.includes(action)))
            .map(([menu]) => menu),
        }
      }),
    )
  }
  demoState.loaded = true
}

export async function bootstrapSession() {
  const token = localStorage.getItem(TOKEN_KEY)
  if (!token) return null
  try {
    const user = normalizeUser(await api.get('/auth/me'))
    demoState.currentUser = user
    localStorage.setItem(USER_KEY, JSON.stringify(user))
    await loadAll()
    return user
  } catch {
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(USER_KEY)
    demoState.currentUser = null
    return null
  }
}

export async function login(username, password) {
  const result = await api.post('/auth/login', { username, password })
  localStorage.setItem(TOKEN_KEY, result.token)
  demoState.currentUser = normalizeUser(result.user)
  localStorage.setItem(USER_KEY, JSON.stringify(demoState.currentUser))
  await loadAll()
  return demoState.currentUser
}

export async function logout() {
  try {
    await api.post('/auth/logout')
  } catch {
    // Stateless logout only requires clearing the client session.
  }
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
  demoState.currentUser = null
  demoState.loaded = false
}

export async function switchDemoIdentity() {
  const nextUsername = {
    ADMIN: 'manager',
    ASSET_MANAGER: 'employee',
    EMPLOYEE: 'admin',
  }[demoState.currentUser?.roleCode] || 'admin'
  return login(nextUsername, '123456')
}

export function isAdmin() {
  return demoState.currentUser?.roleCode === 'ADMIN'
}

export function getDepartment(id) {
  return demoState.departments.find((item) => String(item.id) === String(id))
}

export function getEmployee(id) {
  return demoState.employees.find((item) => String(item.id) === String(id))
}

export function getCategory(id) {
  return demoState.categories.find((item) => String(item.id) === String(id))
}

export function getAsset(id) {
  return demoState.assets.find((item) => String(item.id) === String(id))
}

export function getCurrentEmployee() {
  return getEmployee(demoState.currentUser?.employeeId)
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

export async function addDepartment(payload) {
  const item = await api.post('/departments', {
    ...payload,
    status: String(payload.status || 'active').toUpperCase(),
  })
  demoState.departments.push(normalizeDepartment(item))
  return item
}

export async function updateDepartment(id, payload) {
  const item = await api.put(`/departments/${id}`, {
    ...payload,
    status: String(payload.status || 'active').toUpperCase(),
  })
  const index = demoState.departments.findIndex((row) => row.id === id)
  if (index >= 0) demoState.departments[index] = normalizeDepartment(item)
  return item
}

export async function deleteDepartment(id) {
  await api.delete(`/departments/${id}`)
  demoState.departments = demoState.departments.filter((item) => item.id !== id)
}

export async function addEmployee(payload) {
  const item = await api.post('/employees', {
    ...payload,
    status: String(payload.status || 'active').toUpperCase(),
  })
  demoState.employees.push(normalizeEmployee(item))
  return item
}

export async function updateEmployee(id, payload) {
  const item = await api.put(`/employees/${id}`, {
    ...payload,
    status: String(payload.status || 'active').toUpperCase(),
  })
  const index = demoState.employees.findIndex((row) => row.id === id)
  if (index >= 0) demoState.employees[index] = normalizeEmployee(item)
  return item
}

export async function deleteEmployee(id) {
  await api.delete(`/employees/${id}`)
  demoState.employees = demoState.employees.filter((item) => item.id !== id)
}

export async function addCategory(payload) {
  const item = await api.post('/categories', {
    ...payload,
    status: String(payload.status || 'active').toUpperCase(),
  })
  demoState.categories.push(normalizeCategory(item))
  return item
}

export async function updateCategory(id, payload) {
  const item = await api.put(`/categories/${id}`, {
    ...payload,
    status: String(payload.status || 'active').toUpperCase(),
  })
  const index = demoState.categories.findIndex((row) => row.id === id)
  if (index >= 0) demoState.categories[index] = normalizeCategory(item)
  return item
}

export async function deleteCategory(id) {
  await api.delete(`/categories/${id}`)
  demoState.categories = demoState.categories.filter((item) => item.id !== id)
}

function assetPayload(payload) {
  return {
    ...payload,
    status: String(payload.status || 'IDLE').toUpperCase(),
  }
}

export async function addAsset(payload) {
  const item = await api.post('/assets', assetPayload(payload))
  demoState.assets.unshift(normalizeAsset(item))
  await loadAll()
  return item
}

export async function updateAsset(id, payload) {
  const item = await api.put(`/assets/${id}`, assetPayload(payload))
  const index = demoState.assets.findIndex((row) => row.id === id)
  if (index >= 0) demoState.assets[index] = normalizeAsset(item)
  await loadAll()
  return item
}

export async function deleteAsset(id) {
  await api.delete(`/assets/${id}`)
  demoState.assets = demoState.assets.filter((item) => item.id !== id)
}

export async function submitLoanRequest({ assetId, applicantId, departmentId, remark }) {
  const item = await api.post('/loans', { assetId, applicantId, departmentId, remark })
  await loadAll()
  return item
}

export async function approveLoan(id) {
  const item = await api.post(`/loans/${id}/approve`)
  await loadAll()
  return item
}

export async function rejectLoan(id, reason = '') {
  const item = await api.post(`/loans/${id}/reject`, { reason })
  await loadAll()
  return item
}

export async function requestReturn(id) {
  const item = await api.post(`/loans/${id}/request-return`)
  await loadAll()
  return item
}

export async function confirmReturn(id) {
  const item = await api.post(`/loans/${id}/confirm-return`)
  await loadAll()
  return item
}

export async function createTransfer(payload) {
  const item = await api.post('/transfers', payload)
  await loadAll()
  return item
}

export async function addUser(payload) {
  const item = await api.post('/system/users', payload)
  await loadAll()
  return item
}

export async function updateUser(id, payload) {
  const item = await api.put(`/system/users/${id}`, payload)
  await loadAll()
  return item
}

export async function updateRole(id, payload) {
  const permissionIds = demoState.permissions
    .filter((permission) =>
      (payload.permissions || []).some((menuCode) =>
        (menuActionMap[menuCode] || []).includes(permission.code),
      ),
    )
    .map((permission) => permission.id)
  const item = await api.put(`/system/roles/${id}`, {
    ...payload,
    permissionIds,
  })
  const index = demoState.roles.findIndex((row) => row.id === id)
  if (index >= 0) demoState.roles[index] = { ...item, permissions: payload.permissions, permissionIds }
  return item
}

export async function updateProfile(payload) {
  const user = normalizeUser(await api.put('/auth/profile', payload))
  demoState.currentUser = user
  localStorage.setItem(USER_KEY, JSON.stringify(user))
  const employee = getCurrentEmployee()
  if (employee) {
    employee.name = user.realName
    employee.phone = user.phone
    employee.email = user.email
  }
  return user
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
  return assetStatusMeta[normalizeStatus(status)] || { label: status, type: 'info' }
}

export function getLoanStatusMeta(status) {
  return loanStatusMeta[normalizeStatus(status)] || { label: status, type: 'info' }
}

export function getTransferStatusMeta(status) {
  return transferStatusMeta[normalizeStatus(status)] || { label: status, type: 'info' }
}

export const dashboardStats = computed(() => {
  const summary = demoState.dashboardSummary
  return {
    total: summary.total || demoState.assets.length,
    idle: summary.idle ?? demoState.assets.filter((item) => item.status === 'idle').length,
    inUse: summary.inUse ?? demoState.assets.filter((item) => item.status === 'in_use').length,
    maintenance:
      summary.maintenance ?? demoState.assets.filter((item) => item.status === 'maintenance').length,
    scrapped: summary.scrapped ?? demoState.assets.filter((item) => item.status === 'scrapped').length,
    originalValue:
      Number(summary.originalValue || 0) ||
      demoState.assets.reduce((sum, item) => sum + Number(item.originalValue || 0), 0),
    departmentCounts: summary.departmentCounts || [],
    statusCounts: (summary.statusCounts || []).map((item) => ({
      ...item,
      name: getAssetStatusMeta(item.status).label,
      color: getAssetStatusMeta(item.status).color,
    })),
  }
})

export const myAssets = computed(() => {
  const employee = getCurrentEmployee()
  if (!employee) return []
  return demoState.assets.filter(
    (item) => String(item.ownerId) === String(employee.id) && item.status === 'in_use',
  )
})

export function canAccess(permission) {
  return canRead(permission)
}
