<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { ArrowRight, ArrowRightLeft, Building2, Plus, UserRound } from 'lucide-vue-next'
import StatusTag from '@/components/StatusTag.vue'
import {
  createTransfer,
  demoState,
  departmentName,
  employeeName,
  getAsset,
} from '@/stores/backendStore'
import { formatDate } from '@/utils/format'

const dialogVisible = ref(false)
const formRef = ref()
const form = reactive({
  assetId: null,
  toDepartmentId: null,
  toOwnerId: null,
  reason: '',
})

const activeAssets = computed(() => demoState.assets.filter((item) => item.status === 'in_use'))
const selectedAsset = computed(() => getAsset(form.assetId))
const targetOwners = computed(() =>
  demoState.employees.filter(
    (item) => item.status === 'active' && item.departmentId === form.toDepartmentId,
  ),
)
const records = computed(() =>
  demoState.transferRecords
    .map((item) => ({
      ...item,
      asset: getAsset(item.assetId),
      fromDepartment: departmentName(item.fromDepartmentId),
      toDepartment: departmentName(item.toDepartmentId),
      fromOwner: employeeName(item.fromOwnerId),
      toOwner: employeeName(item.toOwnerId),
    }))
    .sort((a, b) => String(b.transferredAt).localeCompare(String(a.transferredAt))),
)

watch(
  () => form.toDepartmentId,
  () => {
    if (form.toOwnerId && !targetOwners.value.some((item) => item.id === form.toOwnerId)) {
      form.toOwnerId = null
    }
  },
)

function openCreate() {
  Object.assign(form, {
    assetId: null,
    toDepartmentId: null,
    toOwnerId: null,
    reason: '',
  })
  dialogVisible.value = true
}

async function submitForm() {
  try {
    await formRef.value?.validate()
    await createTransfer({ ...form })
    dialogVisible.value = false
    ElMessage.success('调拨完成，资产部门和负责人已同步更新')
  } catch (error) {
    if (error instanceof Error) ElMessage.error(error.message)
  }
}
</script>

<template>
  <div class="page-stack">
    <section class="page-heading">
      <div>
        <span class="eyebrow">TRANSFER</span>
        <h2>资产调拨</h2>
        <p>记录资产在不同部门和负责人之间的归属变化，调拨后自动更新资产台账。</p>
      </div>
      <el-button type="primary" @click="openCreate">
        <Plus :size="17" />
        发起调拨
      </el-button>
    </section>

    <section class="transfer-guide panel">
      <div class="guide-step">
        <span>1</span>
        <div><strong>选择在用资产</strong><small>确认资产当前归属</small></div>
      </div>
      <ArrowRight :size="20" />
      <div class="guide-step">
        <span>2</span>
        <div><strong>指定目标部门</strong><small>选择接收部门</small></div>
      </div>
      <ArrowRight :size="20" />
      <div class="guide-step">
        <span>3</span>
        <div><strong>指定新负责人</strong><small>同步更新资产台账</small></div>
      </div>
    </section>

    <section class="panel">
      <div class="panel-heading">
        <div>
          <span class="eyebrow">TRANSFER HISTORY</span>
          <h3>调拨记录</h3>
        </div>
        <span class="panel-meta">共 {{ records.length }} 条记录</span>
      </div>

      <el-table :data="records" stripe>
        <el-table-column label="资产" min-width="220">
          <template #default="{ row }">
            <div class="table-primary compact">
              <div class="asset-symbol"><ArrowRightLeft :size="17" /></div>
              <div><strong>{{ row.asset?.name }}</strong><span>{{ row.asset?.assetNo }}</span></div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="原归属" min-width="190">
          <template #default="{ row }">
            <div class="owner-stack">
              <span><Building2 :size="14" /> {{ row.fromDepartment }}</span>
              <span><UserRound :size="14" /> {{ row.fromOwner }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="调拨方向" width="70" align="center">
          <template #default>
            <div class="transfer-arrow"><ArrowRight :size="18" /></div>
          </template>
        </el-table-column>
        <el-table-column label="新归属" min-width="190">
          <template #default="{ row }">
            <div class="owner-stack highlight">
              <span><Building2 :size="14" /> {{ row.toDepartment }}</span>
              <span><UserRound :size="14" /> {{ row.toOwner }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="reason" label="调拨原因" min-width="190" show-overflow-tooltip />
        <el-table-column label="调拨时间" width="165">
          <template #default="{ row }">{{ formatDate(row.transferredAt, true) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }"><StatusTag :status="row.status" type="transfer" /></template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!records.length" description="暂无调拨记录" />
    </section>

    <el-dialog v-model="dialogVisible" title="发起资产调拨" width="660px" destroy-on-close>
      <el-form ref="formRef" :model="form" label-position="top">
        <el-form-item
          label="选择在用资产"
          prop="assetId"
          :rules="[{ required: true, message: '请选择资产' }]"
        >
          <el-select v-model="form.assetId" filterable placeholder="请选择需要调拨的资产">
            <el-option
              v-for="asset in activeAssets"
              :key="asset.id"
              :label="`${asset.assetNo} · ${asset.name}`"
              :value="asset.id"
            />
          </el-select>
        </el-form-item>

        <div v-if="selectedAsset" class="current-owner-panel">
          <span>当前归属</span>
          <strong>{{ departmentName(selectedAsset.departmentId) }}</strong>
          <small>负责人：{{ employeeName(selectedAsset.ownerId) }}</small>
        </div>

        <div class="form-grid">
          <el-form-item
            label="目标部门"
            prop="toDepartmentId"
            :rules="[{ required: true, message: '请选择目标部门' }]"
          >
            <el-select v-model="form.toDepartmentId" placeholder="请选择接收部门">
              <el-option
                v-for="department in demoState.departments"
                :key="department.id"
                :label="department.name"
                :value="department.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item
            label="新负责人"
            prop="toOwnerId"
            :rules="[{ required: true, message: '请选择新负责人' }]"
          >
            <el-select
              v-model="form.toOwnerId"
              filterable
              placeholder="请先选择目标部门"
              :disabled="!form.toDepartmentId"
            >
              <el-option
                v-for="employee in targetOwners"
                :key="employee.id"
                :label="employee.name"
                :value="employee.id"
              />
            </el-select>
          </el-form-item>
        </div>
        <el-form-item
          label="调拨原因"
          prop="reason"
          :rules="[{ required: true, message: '请填写调拨原因' }]"
        >
          <el-input v-model="form.reason" type="textarea" :rows="3" placeholder="请说明本次调拨原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm">确认调拨</el-button>
      </template>
    </el-dialog>
  </div>
</template>
