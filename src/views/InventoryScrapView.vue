<script setup>
import { computed, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  CheckCircle2,
  ClipboardList,
  Plus,
  Trash2,
  X,
} from 'lucide-vue-next'
import {
  approveScrap,
  completeInventoryCheck,
  completeScrap,
  createInventoryCheck,
  createScrap,
  demoState,
  employeeName,
  getAsset,
} from '@/stores/backendStore'
import { formatCurrency, formatDate } from '@/utils/format'

const activeTab = ref('inventory')
const inventoryDialog = ref(false)
const scrapDialog = ref(false)
const scrapCompleteDialog = ref(false)
const selectedScrap = ref(null)

const inventoryForm = reactive({
  checkNo: '',
  checkName: '',
  departmentId: null,
  checkDate: new Date().toISOString().slice(0, 10),
  operatorId: null,
  remark: '',
})

const scrapForm = reactive({
  scrapNo: '',
  assetId: null,
  reason: '',
  applicantId: null,
  remark: '',
})

const scrapCompleteForm = reactive({
  disposalMethod: '环保回收',
  disposalAmount: 0,
  remark: '',
})

const inventoryRows = computed(() =>
  demoState.inventoryChecks.map((item) => ({
    ...item,
    departmentName:
      demoState.departments.find((department) => department.id === item.departmentId)?.name || '全公司',
    operatorName: employeeName(item.operatorId),
  })),
)

const scrapRows = computed(() =>
  demoState.scrapRecords.map((item) => ({
    ...item,
    assetNo: getAsset(item.assetId)?.assetNo || '-',
    assetName: getAsset(item.assetId)?.name || '-',
    applicantName: employeeName(item.applicantId),
  })),
)

function openInventory() {
  Object.assign(inventoryForm, {
    checkNo: `IC-${new Date().getFullYear()}-${String(demoState.inventoryChecks.length + 1).padStart(3, '0')}`,
    checkName: '资产盘点',
    departmentId: null,
    checkDate: new Date().toISOString().slice(0, 10),
    operatorId: demoState.employees[0]?.id || null,
    remark: '',
  })
  inventoryDialog.value = true
}

function openScrap() {
  Object.assign(scrapForm, {
    scrapNo: `SC-${new Date().getFullYear()}-${String(demoState.scrapRecords.length + 1).padStart(3, '0')}`,
    assetId: null,
    reason: '',
    applicantId: demoState.employees[0]?.id || null,
    remark: '',
  })
  scrapDialog.value = true
}

async function saveInventory() {
  if (!inventoryForm.checkNo || !inventoryForm.checkName || !inventoryForm.operatorId) {
    ElMessage.warning('请完善盘点任务信息')
    return
  }
  try {
    await createInventoryCheck({ ...inventoryForm })
    inventoryDialog.value = false
    ElMessage.success('盘点任务已创建')
  } catch (error) {
    ElMessage.error(error.message)
  }
}

async function completeInventory(row) {
  try {
    await completeInventoryCheck(row.id)
    ElMessage.success('盘点已完成')
  } catch (error) {
    ElMessage.error(error.message)
  }
}

async function saveScrap() {
  if (!scrapForm.scrapNo || !scrapForm.assetId || !scrapForm.applicantId || !scrapForm.reason) {
    ElMessage.warning('请完善报废申请')
    return
  }
  try {
    await createScrap({ ...scrapForm })
    scrapDialog.value = false
    ElMessage.success('报废申请已提交')
  } catch (error) {
    ElMessage.error(error.message)
  }
}

async function approve(row) {
  try {
    await approveScrap(row.id)
    ElMessage.success('报废申请已审核通过')
  } catch (error) {
    ElMessage.error(error.message)
  }
}

function openComplete(row) {
  selectedScrap.value = row
  Object.assign(scrapCompleteForm, {
    disposalMethod: '环保回收',
    disposalAmount: 0,
    remark: '',
  })
  scrapCompleteDialog.value = true
}

async function complete() {
  try {
    await completeScrap(selectedScrap.value.id, { ...scrapCompleteForm })
    scrapCompleteDialog.value = false
    ElMessage.success('资产报废已完成')
  } catch (error) {
    ElMessage.error(error.message)
  }
}

function statusType(status) {
  return {
    completed: 'success',
    approved: 'success',
    in_progress: 'warning',
    pending: 'warning',
    rejected: 'danger',
    cancelled: 'info',
  }[status] || 'info'
}

function statusLabel(status) {
  return {
    in_progress: '进行中',
    completed: '已完成',
    pending: '待审核',
    approved: '已审核',
    rejected: '已驳回',
    cancelled: '已取消',
  }[status] || status
}
</script>

<template>
  <div class="page-stack">
    <section class="page-heading">
      <div>
        <span class="eyebrow">INVENTORY & DISPOSAL</span>
        <h2>盘点与报废</h2>
        <p>创建资产盘点任务，记录差异结果，并完成报废申请、审核与处置。</p>
      </div>
    </section>

    <section class="panel">
      <el-tabs v-model="activeTab">
        <el-tab-pane :label="`盘点任务 ${inventoryRows.length}`" name="inventory">
          <div class="tab-toolbar">
            <el-button type="primary" @click="openInventory"><Plus :size="16" />创建盘点</el-button>
          </div>
          <el-table :data="inventoryRows" stripe>
            <el-table-column label="盘点任务" min-width="220">
              <template #default="{ row }">
                <div class="table-primary compact">
                  <div class="table-avatar blue"><ClipboardList :size="17" /></div>
                  <div><strong>{{ row.checkName }}</strong><span>{{ row.checkNo }}</span></div>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="departmentName" label="范围" width="120" />
            <el-table-column prop="operatorName" label="盘点人" width="100" />
            <el-table-column label="盘点日期" width="120">
              <template #default="{ row }">{{ formatDate(row.checkDate) }}</template>
            </el-table-column>
            <el-table-column prop="totalCount" label="总数" width="80" align="center" />
            <el-table-column prop="normalCount" label="正常" width="80" align="center" />
            <el-table-column prop="abnormalCount" label="异常" width="80" align="center" />
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="statusType(row.status)" round>{{ statusLabel(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="110" fixed="right">
              <template #default="{ row }">
                <el-button v-if="row.status === 'in_progress'" link type="success" @click="completeInventory(row)">
                  <CheckCircle2 :size="14" />完成
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane :label="`报废记录 ${scrapRows.length}`" name="scrap">
          <div class="tab-toolbar">
            <el-button type="primary" @click="openScrap"><Plus :size="16" />提交报废申请</el-button>
          </div>
          <el-table :data="scrapRows" stripe>
            <el-table-column label="资产" min-width="220">
              <template #default="{ row }">
                <div class="table-primary compact">
                  <div class="asset-symbol"><Trash2 :size="17" /></div>
                  <div><strong>{{ row.assetName }}</strong><span>{{ row.assetNo }}</span></div>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="applicantName" label="申请人" width="100" />
            <el-table-column prop="reason" label="报废原因" min-width="180" show-overflow-tooltip />
            <el-table-column label="处置金额" width="115" align="right">
              <template #default="{ row }">{{ formatCurrency(row.disposalAmount || 0) }}</template>
            </el-table-column>
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="statusType(row.status)" round>{{ statusLabel(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="160" fixed="right">
              <template #default="{ row }">
                <el-button v-if="row.status === 'pending'" link type="success" @click="approve(row)">
                  <CheckCircle2 :size="14" />通过
                </el-button>
                <el-button v-if="row.status === 'approved'" link type="primary" @click="openComplete(row)">
                  完成处置
                </el-button>
                <el-button v-if="row.status === 'pending'" link type="danger">
                  <X :size="14" />驳回
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </section>

    <el-dialog v-model="inventoryDialog" title="创建盘点任务" width="560px">
      <el-form :model="inventoryForm" label-position="top" class="form-grid">
        <el-form-item label="盘点单号"><el-input v-model="inventoryForm.checkNo" /></el-form-item>
        <el-form-item label="盘点名称"><el-input v-model="inventoryForm.checkName" /></el-form-item>
        <el-form-item label="盘点范围">
          <el-select v-model="inventoryForm.departmentId" clearable placeholder="不选择表示全公司">
            <el-option v-for="item in demoState.departments" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="盘点人">
          <el-select v-model="inventoryForm.operatorId">
            <el-option v-for="item in demoState.employees" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="盘点日期">
          <el-date-picker v-model="inventoryForm.checkDate" type="date" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item label="备注"><el-input v-model="inventoryForm.remark" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="inventoryDialog = false">取消</el-button>
        <el-button type="primary" @click="saveInventory">创建</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="scrapDialog" title="提交报废申请" width="560px">
      <el-form :model="scrapForm" label-position="top">
        <el-form-item label="报废单号"><el-input v-model="scrapForm.scrapNo" /></el-form-item>
        <el-form-item label="选择资产">
          <el-select v-model="scrapForm.assetId" filterable>
            <el-option
              v-for="asset in demoState.assets.filter((item) => ['idle', 'maintenance'].includes(item.status))"
              :key="asset.id"
              :label="`${asset.assetNo} · ${asset.name}`"
              :value="asset.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="申请人">
          <el-select v-model="scrapForm.applicantId">
            <el-option v-for="item in demoState.employees" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="报废原因"><el-input v-model="scrapForm.reason" type="textarea" :rows="3" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="scrapForm.remark" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="scrapDialog = false">取消</el-button>
        <el-button type="primary" @click="saveScrap">提交</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="scrapCompleteDialog" title="完成报废处置" width="520px">
      <el-form :model="scrapCompleteForm" label-position="top">
        <el-form-item label="处置方式"><el-input v-model="scrapCompleteForm.disposalMethod" /></el-form-item>
        <el-form-item label="处置金额"><el-input-number v-model="scrapCompleteForm.disposalAmount" :min="0" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="scrapCompleteForm.remark" type="textarea" :rows="3" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="scrapCompleteDialog = false">取消</el-button>
        <el-button type="primary" @click="complete">确认完成</el-button>
      </template>
    </el-dialog>
  </div>
</template>
