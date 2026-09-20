<script setup>
import { computed, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Pencil, Plus, Search, Trash2, UserRound } from 'lucide-vue-next'
import {
  addEmployee,
  deleteEmployee,
  demoState,
  departmentName,
  updateEmployee,
} from '@/stores/demoStore'
import { formatDate } from '@/utils/format'

const filters = reactive({
  keyword: '',
  departmentId: null,
  status: null,
})
const dialogVisible = ref(false)
const editingId = ref(null)
const formRef = ref()
const form = reactive({
  employeeNo: '',
  name: '',
  departmentId: null,
  phone: '',
  email: '',
  joinDate: '',
  status: 'active',
})

const rows = computed(() => {
  const keyword = filters.keyword.trim().toLowerCase()
  return demoState.employees.filter((item) => {
    const matchesKeyword =
      !keyword ||
      `${item.name}${item.employeeNo}${item.phone}`.toLowerCase().includes(keyword)
    return (
      matchesKeyword &&
      (!filters.departmentId || item.departmentId === filters.departmentId) &&
      (!filters.status || item.status === filters.status)
    )
  })
})

function openCreate() {
  editingId.value = null
  Object.assign(form, {
    employeeNo: `E${1000 + demoState.employees.length + 1}`,
    name: '',
    departmentId: null,
    phone: '',
    email: '',
    joinDate: new Date().toISOString().slice(0, 10),
    status: 'active',
  })
  dialogVisible.value = true
}

function openEdit(row) {
  editingId.value = row.id
  Object.assign(form, row)
  dialogVisible.value = true
}

async function submitForm() {
  try {
    await formRef.value?.validate()
    if (editingId.value) {
      updateEmployee(editingId.value, { ...form })
      ElMessage.success('员工信息已更新')
    } else {
      addEmployee({ ...form })
      ElMessage.success('员工已创建')
    }
    dialogVisible.value = false
  } catch (error) {
    if (error instanceof Error) ElMessage.error(error.message)
  }
}

async function remove(row) {
  try {
    await ElMessageBox.confirm(`确认删除员工“${row.name}”吗？`, '删除员工', {
      confirmButtonText: '确认删除',
      cancelButtonText: '取消',
      type: 'warning',
    })
    deleteEmployee(row.id)
    ElMessage.success('员工已删除')
  } catch (error) {
    if (error instanceof Error) ElMessage.error(error.message)
  }
}
</script>

<template>
  <div class="page-stack">
    <section class="page-heading">
      <div>
        <span class="eyebrow">PEOPLE</span>
        <h2>员工管理</h2>
        <p>维护员工、所属部门和在职状态，作为资产责任人基础档案。</p>
      </div>
      <el-button type="primary" @click="openCreate">
        <Plus :size="17" />
        新增员工
      </el-button>
    </section>

    <section class="panel">
      <div class="toolbar filter-toolbar">
        <el-input
          v-model="filters.keyword"
          clearable
          placeholder="姓名、工号或手机号"
          class="search-input"
        >
          <template #prefix>
            <Search :size="16" />
          </template>
        </el-input>
        <el-select v-model="filters.departmentId" clearable placeholder="所属部门">
          <el-option
            v-for="department in demoState.departments"
            :key="department.id"
            :label="department.name"
            :value="department.id"
          />
        </el-select>
        <el-select v-model="filters.status" clearable placeholder="员工状态">
          <el-option label="在职" value="active" />
          <el-option label="离职" value="inactive" />
        </el-select>
        <div class="toolbar-summary">
          找到 <strong>{{ rows.length }}</strong> 名员工
        </div>
      </div>

      <el-table :data="rows" stripe>
        <el-table-column label="员工" min-width="190">
          <template #default="{ row }">
            <div class="table-primary">
              <div class="table-avatar amber">
                <UserRound :size="17" />
              </div>
              <div>
                <strong>{{ row.name }}</strong>
                <span>{{ row.employeeNo }}</span>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="所属部门" min-width="130">
          <template #default="{ row }">{{ departmentName(row.departmentId) }}</template>
        </el-table-column>
        <el-table-column prop="phone" label="手机号" min-width="135" />
        <el-table-column prop="email" label="邮箱" min-width="200" show-overflow-tooltip />
        <el-table-column label="入职日期" min-width="120">
          <template #default="{ row }">{{ formatDate(row.joinDate) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 'active' ? 'success' : 'info'" round>
              {{ row.status === 'active' ? '在职' : '离职' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">
              <Pencil :size="15" />
              编辑
            </el-button>
            <el-button link type="danger" @click="remove(row)">
              <Trash2 :size="15" />
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <el-dialog
      v-model="dialogVisible"
      :title="editingId ? '编辑员工' : '新增员工'"
      width="620px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" label-position="top" class="form-grid">
        <el-form-item
          label="员工姓名"
          prop="name"
          :rules="[{ required: true, message: '请输入员工姓名' }]"
        >
          <el-input v-model="form.name" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item
          label="员工工号"
          prop="employeeNo"
          :rules="[{ required: true, message: '请输入员工工号' }]"
        >
          <el-input v-model="form.employeeNo" placeholder="例如：E1006" />
        </el-form-item>
        <el-form-item
          label="所属部门"
          prop="departmentId"
          :rules="[{ required: true, message: '请选择所属部门' }]"
        >
          <el-select v-model="form.departmentId" placeholder="请选择">
            <el-option
              v-for="department in demoState.departments"
              :key="department.id"
              :label="department.name"
              :value="department.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="入职日期">
          <el-date-picker
            v-model="form.joinDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="选择日期"
          />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="form.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="form.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="员工状态">
          <el-radio-group v-model="form.status">
            <el-radio value="active">在职</el-radio>
            <el-radio value="inactive">离职</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>
