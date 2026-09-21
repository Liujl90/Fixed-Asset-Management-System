<script setup>
import { computed, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  KeyRound,
  Pencil,
  Plus,
  ShieldCheck,
  UserCog,
  UserRound,
} from 'lucide-vue-next'
import {
  addUser,
  demoState,
  updateRole,
  updateUser,
} from '@/stores/backendStore'

const activeTab = ref('users')
const userDialogVisible = ref(false)
const roleDialogVisible = ref(false)
const editingUserId = ref(null)
const editingRoleId = ref(null)
const userFormRef = ref()
const userForm = reactive({
  username: '',
  password: '123456',
  realName: '',
  roleId: null,
  employeeId: null,
  phone: '',
  email: '',
  status: 'active',
})
const roleForm = reactive({
  name: '',
  code: '',
  description: '',
  permissions: [],
})

const permissionOptions = [
  { value: 'dashboard', label: '资产总览' },
  { value: 'departments', label: '部门管理' },
  { value: 'employees', label: '员工管理' },
  { value: 'categories', label: '资产分类' },
  { value: 'assets', label: '固定资产' },
  { value: 'loans', label: '领用与归还' },
  { value: 'transfers', label: '资产调拨' },
  { value: 'system', label: '用户与角色' },
  { value: 'my-assets', label: '我的资产' },
  { value: 'profile', label: '个人信息' },
]

const users = computed(() =>
  demoState.users.map((item) => ({
    ...item,
    roleName: demoState.roles.find((role) => role.id === item.roleId)?.name || '-',
    employeeName:
      demoState.employees.find((employee) => employee.id === item.employeeId)?.name || '未关联员工',
  })),
)

function openCreateUser() {
  editingUserId.value = null
  Object.assign(userForm, {
    username: '',
    password: '123456',
    realName: '',
    roleId: 'role-employee',
    employeeId: null,
    phone: '',
    email: '',
    status: 'active',
  })
  userDialogVisible.value = true
}

function openEditUser(row) {
  editingUserId.value = row.id
  Object.assign(userForm, {
    username: row.username,
    password: row.password,
    realName: row.realName,
    roleId: row.roleId,
    employeeId: row.employeeId,
    phone: row.phone,
    email: row.email,
    status: row.status,
  })
  userDialogVisible.value = true
}

async function submitUser() {
  try {
    await userFormRef.value?.validate()
    if (editingUserId.value) {
      await updateUser(editingUserId.value, { ...userForm })
      ElMessage.success('用户信息已更新')
    } else {
      await addUser({ ...userForm })
      ElMessage.success('用户已创建')
    }
    userDialogVisible.value = false
  } catch (error) {
    if (error instanceof Error) ElMessage.error(error.message)
  }
}

function openRoleEdit(row) {
  editingRoleId.value = row.id
  Object.assign(roleForm, {
    name: row.name,
    code: row.code,
    description: row.description,
    permissions: [...row.permissions],
  })
  roleDialogVisible.value = true
}

async function submitRole() {
  try {
    await updateRole(editingRoleId.value, { ...roleForm, permissions: [...roleForm.permissions] })
    roleDialogVisible.value = false
    ElMessage.success('角色权限已更新')
  } catch (error) {
    ElMessage.error(error.message)
  }
}
</script>

<template>
  <div class="page-stack">
    <section class="page-heading">
      <div>
        <span class="eyebrow">ACCESS CONTROL</span>
        <h2>用户与角色</h2>
        <p>维护登录账号、角色和菜单权限，区分管理员与普通员工的系统能力。</p>
      </div>
      <el-button v-if="activeTab === 'users'" type="primary" @click="openCreateUser">
        <Plus :size="17" />
        新增用户
      </el-button>
    </section>

    <section class="panel">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="用户管理" name="users">
          <el-table :data="users" stripe>
            <el-table-column label="用户" min-width="210">
              <template #default="{ row }">
                <div class="table-primary">
                  <div class="table-avatar blue"><UserRound :size="17" /></div>
                  <div><strong>{{ row.realName }}</strong><span>{{ row.username }}</span></div>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="roleName" label="角色" width="120" />
            <el-table-column prop="employeeName" label="关联员工" min-width="130" />
            <el-table-column prop="phone" label="手机号" min-width="135" />
            <el-table-column prop="email" label="邮箱" min-width="190" show-overflow-tooltip />
            <el-table-column label="状态" width="90">
              <template #default="{ row }">
                <el-tag :type="row.status === 'active' ? 'success' : 'info'" round>
                  {{ row.status === 'active' ? '启用' : '停用' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="100" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="openEditUser(row)">
                  <Pencil :size="15" />
                  编辑
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="角色权限" name="roles">
          <div class="role-grid">
            <article v-for="role in demoState.roles" :key="role.id" class="role-card">
              <div class="role-card-top">
                <div class="role-icon">
                  <ShieldCheck :size="22" />
                </div>
                <el-tag v-if="role.builtIn" type="info" round>内置角色</el-tag>
              </div>
              <h3>{{ role.name }}</h3>
              <span class="role-code">{{ role.code }}</span>
              <p>{{ role.description }}</p>
              <div class="permission-preview">
                <span v-for="permission in role.permissions.slice(0, 5)" :key="permission">
                  {{ permissionOptions.find((item) => item.value === permission)?.label || permission }}
                </span>
                <span v-if="role.permissions.length > 5">+{{ role.permissions.length - 5 }}</span>
              </div>
              <el-button plain @click="openRoleEdit(role)">
                <KeyRound :size="15" />
                配置权限
              </el-button>
            </article>
          </div>
        </el-tab-pane>
      </el-tabs>
    </section>

    <el-dialog
      v-model="userDialogVisible"
      :title="editingUserId ? '编辑用户' : '新增用户'"
      width="650px"
      destroy-on-close
    >
      <el-form ref="userFormRef" :model="userForm" label-position="top" class="form-grid">
        <el-form-item
          label="登录账号"
          prop="username"
          :rules="[{ required: true, message: '请输入登录账号' }]"
        >
          <el-input v-model="userForm.username" placeholder="请输入登录账号" />
        </el-form-item>
        <el-form-item
          label="姓名"
          prop="realName"
          :rules="[{ required: true, message: '请输入姓名' }]"
        >
          <el-input v-model="userForm.realName" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="登录密码">
          <el-input v-model="userForm.password" type="password" show-password />
        </el-form-item>
        <el-form-item
          label="角色"
          prop="roleId"
          :rules="[{ required: true, message: '请选择角色' }]"
        >
          <el-select v-model="userForm.roleId">
            <el-option
              v-for="role in demoState.roles"
              :key="role.id"
              :label="role.name"
              :value="role.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="关联员工">
          <el-select v-model="userForm.employeeId" clearable filterable placeholder="可选择员工档案">
            <el-option
              v-for="employee in demoState.employees"
              :key="employee.id"
              :label="employee.name"
              :value="employee.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="userForm.status">
            <el-radio value="active">启用</el-radio>
            <el-radio value="inactive">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="userForm.phone" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="userForm.email" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="userDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitUser">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="roleDialogVisible" title="配置角色权限" width="620px" destroy-on-close>
      <el-form :model="roleForm" label-position="top">
        <div class="form-grid">
          <el-form-item label="角色名称">
            <el-input v-model="roleForm.name" />
          </el-form-item>
          <el-form-item label="角色编码">
            <el-input v-model="roleForm.code" />
          </el-form-item>
        </div>
        <el-form-item label="角色说明">
          <el-input v-model="roleForm.description" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="菜单权限">
          <el-checkbox-group v-model="roleForm.permissions" class="permission-checkboxes">
            <el-checkbox v-for="item in permissionOptions" :key="item.value" :value="item.value">
              {{ item.label }}
            </el-checkbox>
          </el-checkbox-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="roleDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitRole">保存权限</el-button>
      </template>
    </el-dialog>
  </div>
</template>
