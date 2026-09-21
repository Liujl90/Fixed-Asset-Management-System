<script setup>
import { computed, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  CalendarClock,
  CheckCircle2,
  Database,
  Play,
  Plus,
  RefreshCcw,
  Wrench,
} from 'lucide-vue-next'
import {
  completeMaintenancePlan,
  createMaintenancePlan,
  demoState,
  getAsset,
  runDepreciation,
  runMaintenanceCheck,
} from '@/stores/backendStore'
import { formatCurrency, formatDate } from '@/utils/format'

const activeTab = ref('maintenance')
const dialogVisible = ref(false)
const submitting = ref(false)
const currentMonth = new Date().toISOString().slice(0, 7)
const depreciationMonth = ref(currentMonth)
const form = reactive({
  assetId: null,
  planName: '',
  maintenanceType: 'MAINTENANCE',
  planDate: new Date().toISOString().slice(0, 10),
  cycleMonths: 12,
  remark: '',
})

const maintenanceRows = computed(() =>
  demoState.maintenancePlans.map((item) => ({
    ...item,
    assetName: getAsset(item.assetId)?.name || '-',
    assetNo: getAsset(item.assetId)?.assetNo || '-',
  })),
)

const depreciationRows = computed(() =>
  demoState.depreciationRecords.map((item) => ({
    ...item,
    assetName: getAsset(item.assetId)?.name || '-',
    assetNo: getAsset(item.assetId)?.assetNo || '-',
  })),
)

function openCreate() {
  Object.assign(form, {
    assetId: null,
    planName: '',
    maintenanceType: 'MAINTENANCE',
    planDate: new Date().toISOString().slice(0, 10),
    cycleMonths: 12,
    remark: '',
  })
  dialogVisible.value = true
}

async function submitPlan() {
  if (!form.assetId || !form.planName || !form.planDate) {
    ElMessage.warning('请填写资产、计划名称和计划日期')
    return
  }
  submitting.value = true
  try {
    await createMaintenancePlan({ ...form })
    dialogVisible.value = false
    ElMessage.success('保养计划已创建')
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    submitting.value = false
  }
}

async function completePlan(row) {
  try {
    await completeMaintenancePlan(row.id)
    ElMessage.success('保养计划已完成')
  } catch (error) {
    ElMessage.error(error.message)
  }
}

async function triggerMaintenanceCheck() {
  try {
    const result = await runMaintenanceCheck()
    ElMessage.success(`保养检查完成，更新 ${result.updated || 0} 条计划`)
  } catch (error) {
    ElMessage.error(error.message)
  }
}

async function triggerDepreciation() {
  if (!depreciationMonth.value) {
    ElMessage.warning('请选择折旧月份')
    return
  }
  try {
    const result = await runDepreciation(depreciationMonth.value)
    ElMessage.success(`折旧计算完成，新增 ${result.created || 0} 条记录`)
  } catch (error) {
    ElMessage.error(error.message)
  }
}
</script>

<template>
  <div class="page-stack">
    <section class="page-heading">
      <div>
        <span class="eyebrow">BACKGROUND OPERATIONS</span>
        <h2>运维任务与折旧</h2>
        <p>管理保养计划，执行保养到期检查，并运行月度资产折旧任务。</p>
      </div>
      <el-button type="primary" @click="openCreate">
        <Plus :size="17" />
        新增保养计划
      </el-button>
    </section>

    <section class="operation-action-grid">
      <article class="operation-action-card">
        <div class="operation-action-icon rust">
          <Wrench :size="21" />
        </div>
        <div>
          <span>保养到期检查</span>
          <strong>Quartz Job</strong>
          <small>每日 08:00 自动执行，也可手动触发</small>
        </div>
        <el-button type="primary" plain @click="triggerMaintenanceCheck">
          <RefreshCcw :size="15" />
          立即检查
        </el-button>
      </article>

      <article class="operation-action-card">
        <div class="operation-action-icon teal">
          <CalendarClock :size="21" />
        </div>
        <div>
          <span>月度折旧计算</span>
          <strong>Quartz Job</strong>
          <small>每月 1 日执行，支持补算指定月份</small>
        </div>
        <div class="operation-inline-form">
          <el-date-picker
            v-model="depreciationMonth"
            type="month"
            value-format="YYYY-MM"
            placeholder="选择月份"
          />
          <el-button type="primary" @click="triggerDepreciation">
            <Play :size="15" />
            执行
          </el-button>
        </div>
      </article>
    </section>

    <section class="panel">
      <el-tabs v-model="activeTab">
        <el-tab-pane :label="`保养计划 ${maintenanceRows.length}`" name="maintenance">
          <el-table :data="maintenanceRows" stripe>
            <el-table-column label="资产" min-width="220">
              <template #default="{ row }">
                <div class="table-primary compact">
                  <div class="asset-symbol"><Wrench :size="17" /></div>
                  <div><strong>{{ row.assetName }}</strong><span>{{ row.assetNo }}</span></div>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="planName" label="计划名称" min-width="180" />
            <el-table-column prop="maintenanceType" label="类型" width="110" />
            <el-table-column label="计划日期" width="120">
              <template #default="{ row }">{{ formatDate(row.planDate) }}</template>
            </el-table-column>
            <el-table-column prop="cycleMonths" label="周期(月)" width="90" align="center" />
            <el-table-column label="状态" width="110">
              <template #default="{ row }">
                <el-tag
                  :type="row.status === 'completed' ? 'success' : row.status === 'overdue' ? 'danger' : 'warning'"
                  round
                >
                  {{ row.status === 'completed' ? '已完成' : row.status === 'due' ? '即将到期' : row.status === 'overdue' ? '已逾期' : '待处理' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="remark" label="备注" min-width="160" show-overflow-tooltip />
            <el-table-column label="操作" width="120" fixed="right">
              <template #default="{ row }">
                <el-button
                  v-if="row.status !== 'completed'"
                  link
                  type="primary"
                  @click="completePlan(row)"
                >
                  <CheckCircle2 :size="15" />
                  完成
                </el-button>
                <span v-else class="muted-cell">已完成</span>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane :label="`折旧记录 ${depreciationRows.length}`" name="depreciation">
          <el-table :data="depreciationRows" stripe>
            <el-table-column label="资产" min-width="220">
              <template #default="{ row }">
                <div class="table-primary compact">
                  <div class="asset-symbol"><Database :size="17" /></div>
                  <div><strong>{{ row.assetName }}</strong><span>{{ row.assetNo }}</span></div>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="depreciationMonth" label="折旧月份" width="110" />
            <el-table-column label="资产原值" width="125" align="right">
              <template #default="{ row }">{{ formatCurrency(row.originalValue) }}</template>
            </el-table-column>
            <el-table-column label="本月折旧" width="125" align="right">
              <template #default="{ row }">{{ formatCurrency(row.monthlyAmount) }}</template>
            </el-table-column>
            <el-table-column label="累计折旧" width="125" align="right">
              <template #default="{ row }">{{ formatCurrency(row.accumulatedAmount) }}</template>
            </el-table-column>
          </el-table>
          <el-empty v-if="!depreciationRows.length" description="尚未执行折旧任务" />
        </el-tab-pane>
      </el-tabs>
    </section>

    <el-dialog v-model="dialogVisible" title="新增保养计划" width="560px" destroy-on-close>
      <el-form :model="form" label-position="top">
        <el-form-item label="选择资产">
          <el-select v-model="form.assetId" filterable placeholder="请选择资产">
            <el-option
              v-for="asset in demoState.assets"
              :key="asset.id"
              :label="`${asset.assetNo} · ${asset.name}`"
              :value="asset.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="计划名称">
          <el-input v-model="form.planName" placeholder="例如：年度设备校准" />
        </el-form-item>
        <div class="form-grid">
          <el-form-item label="保养类型">
            <el-select v-model="form.maintenanceType">
              <el-option label="维修" value="REPAIR" />
              <el-option label="保养" value="MAINTENANCE" />
              <el-option label="校准" value="CALIBRATION" />
              <el-option label="巡检" value="INSPECTION" />
            </el-select>
          </el-form-item>
          <el-form-item label="计划日期">
            <el-date-picker
              v-model="form.planDate"
              type="date"
              value-format="YYYY-MM-DD"
              placeholder="选择日期"
            />
          </el-form-item>
          <el-form-item label="周期（月）">
            <el-input-number v-model="form.cycleMonths" :min="1" :max="120" controls-position="right" />
          </el-form-item>
        </div>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitPlan">保存计划</el-button>
      </template>
    </el-dialog>
  </div>
</template>
