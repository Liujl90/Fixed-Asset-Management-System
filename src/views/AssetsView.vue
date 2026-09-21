<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Boxes,
  Eye,
  Filter,
  Pencil,
  Plus,
  Search,
  Trash2,
} from 'lucide-vue-next'
import StatusTag from '@/components/StatusTag.vue'
import {
  addAsset,
  assetStatuses,
  categoryName,
  deleteAsset,
  demoState,
  departmentName,
  employeeName,
  getAsset,
  updateAsset,
} from '@/stores/backendStore'
import { formatCurrency, formatDate } from '@/utils/format'

const filters = reactive({
  keyword: '',
  categoryId: null,
  status: null,
  departmentId: null,
})
const page = ref(1)
const pageSize = ref(8)
const dialogVisible = ref(false)
const detailVisible = ref(false)
const editingId = ref(null)
const selectedAssetId = ref(null)
const formRef = ref()

const emptyForm = () => ({
  assetNo: '',
  name: '',
  categoryId: null,
  brandModel: '',
  purchaseDate: new Date().toISOString().slice(0, 10),
  originalValue: 0,
  usefulLife: 5,
  departmentId: null,
  ownerId: null,
  status: 'idle',
  remark: '',
})
const form = reactive(emptyForm())

const filteredRows = computed(() => {
  const keyword = filters.keyword.trim().toLowerCase()
  return demoState.assets.filter((item) => {
    const matchesKeyword =
      !keyword ||
      `${item.assetNo}${item.name}${item.brandModel}`.toLowerCase().includes(keyword)
    return (
      matchesKeyword &&
      (!filters.categoryId || item.categoryId === filters.categoryId) &&
      (!filters.status || item.status === filters.status) &&
      (!filters.departmentId || item.departmentId === filters.departmentId)
    )
  })
})

const rows = computed(() => {
  const start = (page.value - 1) * pageSize.value
  return filteredRows.value.slice(start, start + pageSize.value)
})

const selectedAsset = computed(() => getAsset(selectedAssetId.value))
const selectedAssetChanges = computed(() =>
  demoState.changeRecords.filter((item) => item.assetId === selectedAssetId.value).slice(0, 8),
)

const statusSummary = computed(() =>
  assetStatuses.map((status) => ({
    ...status,
    count: demoState.assets.filter((item) => item.status === status.value).length,
  })),
)

const eligibleOwners = computed(() =>
  demoState.employees.filter(
    (employee) =>
      employee.status === 'active' &&
      (!form.departmentId || employee.departmentId === form.departmentId),
  ),
)

watch(
  () => [filters.keyword, filters.categoryId, filters.status, filters.departmentId],
  () => {
    page.value = 1
  },
)

function resetFilters() {
  Object.assign(filters, { keyword: '', categoryId: null, status: null, departmentId: null })
}

function openCreate() {
  editingId.value = null
  Object.assign(form, emptyForm(), {
    assetNo: `FA-${new Date().getFullYear()}-${String(demoState.assets.length + 1).padStart(3, '0')}`,
  })
  dialogVisible.value = true
}

function openEdit(row) {
  editingId.value = row.id
  Object.assign(form, row)
  dialogVisible.value = true
}

function openDetail(row) {
  selectedAssetId.value = row.id
  detailVisible.value = true
}

function handleDepartmentChange() {
  if (form.ownerId && !eligibleOwners.value.some((item) => item.id === form.ownerId)) {
    form.ownerId = null
  }
}

async function submitForm() {
  try {
    await formRef.value?.validate()
    const payload = {
      ...form,
      originalValue: Number(form.originalValue || 0),
      usefulLife: Number(form.usefulLife || 0),
      ownerId: form.status === 'idle' ? null : form.ownerId,
    }
    if (editingId.value) {
      await updateAsset(editingId.value, payload)
      ElMessage.success('资产信息已更新')
    } else {
      await addAsset(payload)
      ElMessage.success('资产已登记')
    }
    dialogVisible.value = false
  } catch (error) {
    if (error instanceof Error) ElMessage.error(error.message)
  }
}

async function remove(row) {
  try {
    await ElMessageBox.confirm(
      `确认删除资产“${row.name}”吗？相关演示记录也会一并删除。`,
      '删除资产',
      {
        confirmButtonText: '确认删除',
        cancelButtonText: '取消',
        type: 'warning',
      },
    )
    await deleteAsset(row.id)
    ElMessage.success('资产已删除')
  } catch (error) {
    if (error instanceof Error) ElMessage.error(error.message)
  }
}
</script>

<template>
  <div class="page-stack">
    <section class="page-heading">
      <div>
        <span class="eyebrow">ASSET REGISTRY</span>
        <h2>固定资产台账</h2>
        <p>维护资产基础信息、当前状态、所属部门和负责人。</p>
      </div>
      <el-button type="primary" @click="openCreate">
        <Plus :size="17" />
        登记资产
      </el-button>
    </section>

    <section class="asset-summary-strip">
      <button
        v-for="item in statusSummary"
        :key="item.value"
        :class="{ active: filters.status === item.value }"
        @click="filters.status = filters.status === item.value ? null : item.value"
      >
        <span class="summary-dot" :style="{ background: item.color }" />
        <span>{{ item.label }}</span>
        <strong>{{ item.count }}</strong>
      </button>
      <button :class="{ active: !filters.status }" @click="filters.status = null">
        <span class="summary-dot all" />
        <span>全部资产</span>
        <strong>{{ demoState.assets.length }}</strong>
      </button>
    </section>

    <section class="panel">
      <div class="toolbar filter-toolbar">
        <el-input
          v-model="filters.keyword"
          class="search-input wide"
          clearable
          placeholder="资产编号、名称或品牌型号"
        >
          <template #prefix>
            <Search :size="16" />
          </template>
        </el-input>
        <el-select v-model="filters.categoryId" clearable placeholder="资产分类">
          <el-option
            v-for="category in demoState.categories"
            :key="category.id"
            :label="category.name"
            :value="category.id"
          />
        </el-select>
        <el-select v-model="filters.departmentId" clearable placeholder="所属部门">
          <el-option
            v-for="department in demoState.departments"
            :key="department.id"
            :label="department.name"
            :value="department.id"
          />
        </el-select>
        <el-button @click="resetFilters">
          <Filter :size="16" />
          重置
        </el-button>
        <div class="toolbar-summary">
          共 <strong>{{ filteredRows.length }}</strong> 条
        </div>
      </div>

      <el-table :data="rows" stripe>
        <el-table-column label="资产" min-width="250">
          <template #default="{ row }">
            <div class="table-primary">
              <div class="asset-symbol">
                <Boxes :size="18" />
              </div>
              <div>
                <strong>{{ row.name }}</strong>
                <span>{{ row.assetNo }} · {{ row.brandModel }}</span>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="分类" min-width="115">
          <template #default="{ row }">{{ categoryName(row.categoryId) }}</template>
        </el-table-column>
        <el-table-column label="所属部门" min-width="115">
          <template #default="{ row }">{{ departmentName(row.departmentId) }}</template>
        </el-table-column>
        <el-table-column label="当前负责人" min-width="110">
          <template #default="{ row }">{{ employeeName(row.ownerId) }}</template>
        </el-table-column>
        <el-table-column label="原值" width="120" align="right">
          <template #default="{ row }">{{ formatCurrency(row.originalValue) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="95">
          <template #default="{ row }">
            <StatusTag :status="row.status" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="190" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">
              <Eye :size="15" />
              详情
            </el-button>
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

      <div class="table-pagination">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="pageSize"
          layout="total, sizes, prev, pager, next"
          :page-sizes="[8, 12, 20]"
          :total="filteredRows.length"
        />
      </div>
    </section>

    <el-dialog
      v-model="dialogVisible"
      :title="editingId ? '编辑资产' : '登记资产'"
      width="760px"
      destroy-on-close
      class="asset-form-dialog"
    >
      <el-form ref="formRef" :model="form" label-position="top" class="form-grid">
        <el-form-item
          label="资产编号"
          prop="assetNo"
          :rules="[{ required: true, message: '请输入资产编号' }]"
        >
          <el-input v-model="form.assetNo" placeholder="例如：FA-2026-011" />
        </el-form-item>
        <el-form-item
          label="资产名称"
          prop="name"
          :rules="[{ required: true, message: '请输入资产名称' }]"
        >
          <el-input v-model="form.name" placeholder="请输入资产名称" />
        </el-form-item>
        <el-form-item
          label="资产分类"
          prop="categoryId"
          :rules="[{ required: true, message: '请选择资产分类' }]"
        >
          <el-select v-model="form.categoryId" filterable placeholder="请选择分类">
            <el-option
              v-for="category in demoState.categories"
              :key="category.id"
              :label="category.name"
              :value="category.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="品牌 / 型号">
          <el-input v-model="form.brandModel" placeholder="例如：Lenovo ThinkPad T14" />
        </el-form-item>
        <el-form-item label="购买日期">
          <el-date-picker
            v-model="form.purchaseDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="选择购买日期"
          />
        </el-form-item>
        <el-form-item label="资产原值（元）">
          <el-input-number v-model="form.originalValue" :min="0" :step="100" controls-position="right" />
        </el-form-item>
        <el-form-item label="使用年限（年）">
          <el-input-number v-model="form.usefulLife" :min="0" :max="50" controls-position="right" />
        </el-form-item>
        <el-form-item label="资产状态">
          <el-select v-model="form.status">
            <el-option
              v-for="status in assetStatuses"
              :key="status.value"
              :label="status.label"
              :value="status.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="所属部门">
          <el-select
            v-model="form.departmentId"
            clearable
            placeholder="请选择部门"
            @change="handleDepartmentChange"
          >
            <el-option
              v-for="department in demoState.departments"
              :key="department.id"
              :label="department.name"
              :value="department.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="当前负责人">
          <el-select
            v-model="form.ownerId"
            clearable
            filterable
            placeholder="闲置资产可不指定"
            :disabled="form.status === 'idle'"
          >
            <el-option
              v-for="employee in eligibleOwners"
              :key="employee.id"
              :label="employee.name"
              :value="employee.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="备注" class="span-two">
          <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="补充说明" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm">保存资产</el-button>
      </template>
    </el-dialog>

    <el-drawer
      v-model="detailVisible"
      title="资产详情"
      size="520px"
      class="detail-drawer"
      destroy-on-close
    >
      <div v-if="selectedAsset" class="asset-detail">
        <div class="detail-hero">
          <div class="asset-symbol large">
            <Boxes :size="26" />
          </div>
          <div>
            <span>{{ selectedAsset.assetNo }}</span>
            <h3>{{ selectedAsset.name }}</h3>
            <StatusTag :status="selectedAsset.status" />
          </div>
        </div>

        <div class="detail-section">
          <h4>基础信息</h4>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="资产分类">
              {{ categoryName(selectedAsset.categoryId) }}
            </el-descriptions-item>
            <el-descriptions-item label="品牌 / 型号">
              {{ selectedAsset.brandModel }}
            </el-descriptions-item>
            <el-descriptions-item label="购买日期">
              {{ formatDate(selectedAsset.purchaseDate) }}
            </el-descriptions-item>
            <el-descriptions-item label="资产原值">
              {{ formatCurrency(selectedAsset.originalValue) }}
            </el-descriptions-item>
            <el-descriptions-item label="使用年限">
              {{ selectedAsset.usefulLife }} 年
            </el-descriptions-item>
            <el-descriptions-item label="所属部门">
              {{ departmentName(selectedAsset.departmentId) }}
            </el-descriptions-item>
            <el-descriptions-item label="当前负责人">
              {{ employeeName(selectedAsset.ownerId) }}
            </el-descriptions-item>
            <el-descriptions-item label="登记时间">
              {{ formatDate(selectedAsset.createdAt) }}
            </el-descriptions-item>
          </el-descriptions>
        </div>

        <div class="detail-section">
          <h4>备注</h4>
          <div class="detail-note">{{ selectedAsset.remark || '暂无备注' }}</div>
        </div>

        <div class="detail-section">
          <h4>最近变动</h4>
          <div v-if="selectedAssetChanges.length" class="mini-timeline">
            <div v-for="item in selectedAssetChanges" :key="item.id" class="timeline-item">
              <span class="timeline-dot" />
              <div>
                <strong>{{ item.type }}</strong>
                <p>{{ item.description }}</p>
                <time>{{ formatDate(item.createdAt, true) }} · {{ item.operator }}</time>
              </div>
            </div>
          </div>
          <el-empty v-else description="暂无变动记录" :image-size="72" />
        </div>
      </div>
    </el-drawer>
  </div>
</template>
