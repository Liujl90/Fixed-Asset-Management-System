<script setup>
import { computed, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Building2, Pencil, Plus, Search, Trash2, Users } from 'lucide-vue-next'
import {
  addDepartment,
  deleteDepartment,
  demoState,
  employeeName,
  updateDepartment,
} from '@/stores/backendStore'

const keyword = ref('')
const dialogVisible = ref(false)
const editingId = ref(null)
const formRef = ref()
const form = reactive({
  name: '',
  code: '',
  managerId: null,
  status: 'active',
})

const rows = computed(() => {
  const key = keyword.value.trim().toLowerCase()
  return demoState.departments
    .filter((item) => !key || `${item.name}${item.code}`.toLowerCase().includes(key))
    .map((item) => ({
      ...item,
      employeeCount: demoState.employees.filter((employee) => employee.departmentId === item.id).length,
      assetCount: demoState.assets.filter((asset) => asset.departmentId === item.id).length,
    }))
})

function openCreate() {
  editingId.value = null
  Object.assign(form, { name: '', code: '', managerId: null, status: 'active' })
  dialogVisible.value = true
}

function openEdit(row) {
  editingId.value = row.id
  Object.assign(form, {
    name: row.name,
    code: row.code,
    managerId: row.managerId,
    status: row.status,
  })
  dialogVisible.value = true
}

async function submitForm() {
  try {
    await formRef.value?.validate()
    if (editingId.value) {
      await updateDepartment(editingId.value, { ...form })
      ElMessage.success('部门信息已更新')
    } else {
      await addDepartment({ ...form })
      ElMessage.success('部门已创建')
    }
    dialogVisible.value = false
  } catch (error) {
    if (error instanceof Error) ElMessage.error(error.message)
  }
}

async function remove(row) {
  try {
    await ElMessageBox.confirm(`确认删除部门“${row.name}”吗？`, '删除部门', {
      confirmButtonText: '确认删除',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await deleteDepartment(row.id)
    ElMessage.success('部门已删除')
  } catch (error) {
    if (error instanceof Error) ElMessage.error(error.message)
  }
}
</script>

<template>
  <div class="page-stack">
    <section class="page-heading">
      <div>
        <span class="eyebrow">ORGANIZATION</span>
        <h2>部门管理</h2>
        <p>维护组织架构，为员工、资产归属和部门统计提供基础数据。</p>
      </div>
      <el-button type="primary" @click="openCreate">
        <Plus :size="17" />
        新增部门
      </el-button>
    </section>

    <section class="panel">
      <div class="toolbar">
        <el-input v-model="keyword" clearable placeholder="搜索部门名称或编码" class="search-input">
          <template #prefix>
            <Search :size="16" />
          </template>
        </el-input>
        <div class="toolbar-summary">
          共 <strong>{{ rows.length }}</strong> 个部门
        </div>
      </div>

      <el-table :data="rows" stripe>
        <el-table-column label="部门" min-width="190">
          <template #default="{ row }">
            <div class="table-primary">
              <div class="table-avatar teal">
                <Building2 :size="17" />
              </div>
              <div>
                <strong>{{ row.name }}</strong>
                <span>{{ row.code }}</span>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="负责人" min-width="130">
          <template #default="{ row }">{{ employeeName(row.managerId) }}</template>
        </el-table-column>
        <el-table-column label="员工数" width="110" align="center">
          <template #default="{ row }">
            <span class="count-pill">
              <Users :size="14" />
              {{ row.employeeCount }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="assetCount" label="资产数" width="100" align="center" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'active' ? 'success' : 'info'" round>
              {{ row.status === 'active' ? '启用' : '停用' }}
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
      :title="editingId ? '编辑部门' : '新增部门'"
      width="520px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" label-position="top">
        <el-form-item label="部门名称" prop="name" :rules="[{ required: true, message: '请输入部门名称' }]">
          <el-input v-model="form.name" placeholder="例如：研发部" />
        </el-form-item>
        <el-form-item label="部门编码" prop="code" :rules="[{ required: true, message: '请输入部门编码' }]">
          <el-input v-model="form.code" placeholder="例如：RND" />
        </el-form-item>
        <el-form-item label="部门负责人">
          <el-select v-model="form.managerId" clearable filterable placeholder="请选择员工">
            <el-option
              v-for="employee in demoState.employees"
              :key="employee.id"
              :label="employee.name"
              :value="employee.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio value="active">启用</el-radio>
            <el-radio value="inactive">停用</el-radio>
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
