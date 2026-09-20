<script setup>
import { computed, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ArrowDownToLine,
  Check,
  ClipboardCheck,
  Clock3,
  Plus,
  RotateCcw,
  Send,
  UserRound,
  X,
} from 'lucide-vue-next'
import StatusTag from '@/components/StatusTag.vue'
import {
  approveLoan,
  categoryName,
  confirmReturn,
  demoState,
  departmentName,
  employeeName,
  getAsset,
  getCurrentEmployee,
  rejectLoan,
  requestReturn,
  submitLoanRequest,
} from '@/stores/demoStore'
import { formatDate } from '@/utils/format'

const isAdmin = computed(() => demoState.currentUser?.roleCode === 'ADMIN')
const adminTab = ref('pending')
const employeeTab = ref('mine')
const applyVisible = ref(false)
const formRef = ref()
const form = reactive({
  assetId: null,
  departmentId: null,
  remark: '',
})

const currentEmployee = computed(() => getCurrentEmployee())

function detail(record) {
  const asset = getAsset(record.assetId)
  return {
    ...record,
    asset,
    assetName: asset?.name || '-',
    assetNo: asset?.assetNo || '-',
    applicantName: employeeName(record.applicantId),
    departmentName: departmentName(record.departmentId),
    ownerName: employeeName(record.ownerId),
  }
}

const allRecords = computed(() =>
  demoState.loanRecords
    .map(detail)
    .sort((a, b) => String(b.requestedAt).localeCompare(String(a.requestedAt))),
)
const pendingRecords = computed(() => allRecords.value.filter((item) => item.status === 'pending'))
const activeRecords = computed(() =>
  allRecords.value.filter((item) => ['active', 'return_pending'].includes(item.status)),
)
const historyRecords = computed(() =>
  allRecords.value.filter((item) => ['returned', 'rejected'].includes(item.status)),
)
const myRecords = computed(() =>
  allRecords.value.filter((item) => item.applicantId === currentEmployee.value?.id),
)
const myActiveRecords = computed(() =>
  myRecords.value.filter((item) => ['active', 'return_pending'].includes(item.status)),
)
const idleAssets = computed(() =>
  demoState.assets.filter(
    (asset) =>
      asset.status === 'idle' &&
      !demoState.loanRecords.some(
        (loan) =>
          loan.assetId === asset.id &&
          ['pending', 'active', 'return_pending'].includes(loan.status),
      ),
  ),
)

function openApply() {
  if (!currentEmployee.value) {
    ElMessage.warning('当前账号未关联员工档案，暂不能提交领用申请')
    return
  }
  Object.assign(form, {
    assetId: null,
    departmentId: currentEmployee.value.departmentId,
    remark: '',
  })
  applyVisible.value = true
}

async function submitApply() {
  try {
    await formRef.value?.validate()
    submitLoanRequest({
      assetId: form.assetId,
      applicantId: currentEmployee.value.id,
      departmentId: form.departmentId,
      remark: form.remark,
    })
    applyVisible.value = false
    employeeTab.value = 'mine'
    ElMessage.success('领用申请已提交，等待管理员处理')
  } catch (error) {
    if (error instanceof Error) ElMessage.error(error.message)
  }
}

function approve(record) {
  try {
    approveLoan(record.id)
    ElMessage.success('领用已确认，资产状态已更新为在用')
  } catch (error) {
    ElMessage.error(error.message)
  }
}

async function reject(record) {
  try {
    const { value } = await ElMessageBox.prompt('可填写驳回原因', `驳回 ${record.assetName} 的领用申请`, {
      confirmButtonText: '确认驳回',
      cancelButtonText: '取消',
      inputPlaceholder: '例如：资产需要重新检测',
      inputValue: '',
    })
    rejectLoan(record.id, value)
    ElMessage.success('申请已驳回')
  } catch (error) {
    if (error instanceof Error) ElMessage.error(error.message)
  }
}

function requestRecordReturn(record) {
  try {
    requestReturn(record.id)
    ElMessage.success('归还申请已提交，等待管理员确认')
  } catch (error) {
    ElMessage.error(error.message)
  }
}

function confirmRecordReturn(record) {
  try {
    confirmReturn(record.id)
    ElMessage.success('归还已确认，资产状态已更新为闲置')
  } catch (error) {
    ElMessage.error(error.message)
  }
}
</script>

<template>
  <div class="page-stack">
    <section class="page-heading">
      <div>
        <span class="eyebrow">LOAN & RETURN</span>
        <h2>{{ isAdmin ? '领用与归还处理' : '我的领用服务' }}</h2>
        <p>
          {{
            isAdmin
              ? '处理员工领用申请，确认资产归还，保持资产状态与使用关系一致。'
              : '选择闲置资产提交领用申请，并管理自己正在使用的资产。'
          }}
        </p>
      </div>
      <el-button v-if="!isAdmin" type="primary" @click="openApply">
        <Plus :size="17" />
        申请领用
      </el-button>
    </section>

    <section v-if="isAdmin" class="compact-metrics">
      <article>
        <div class="compact-icon warning"><Clock3 :size="18" /></div>
        <div><span>待处理申请</span><strong>{{ pendingRecords.length }}</strong></div>
      </article>
      <article>
        <div class="compact-icon success"><ClipboardCheck :size="18" /></div>
        <div><span>使用中资产</span><strong>{{ activeRecords.filter((item) => item.status === 'active').length }}</strong></div>
      </article>
      <article>
        <div class="compact-icon primary"><ArrowDownToLine :size="18" /></div>
        <div><span>待归还确认</span><strong>{{ activeRecords.filter((item) => item.status === 'return_pending').length }}</strong></div>
      </article>
      <article>
        <div class="compact-icon muted"><RotateCcw :size="18" /></div>
        <div><span>历史记录</span><strong>{{ historyRecords.length }}</strong></div>
      </article>
    </section>

    <section v-else class="compact-metrics">
      <article>
        <div class="compact-icon success"><ClipboardCheck :size="18" /></div>
        <div><span>正在使用</span><strong>{{ myActiveRecords.filter((item) => item.status === 'active').length }}</strong></div>
      </article>
      <article>
        <div class="compact-icon warning"><Clock3 :size="18" /></div>
        <div><span>待管理员处理</span><strong>{{ myRecords.filter((item) => item.status === 'pending').length }}</strong></div>
      </article>
      <article>
        <div class="compact-icon primary"><RotateCcw :size="18" /></div>
        <div><span>归还确认中</span><strong>{{ myRecords.filter((item) => item.status === 'return_pending').length }}</strong></div>
      </article>
      <article>
        <div class="compact-icon muted"><UserRound :size="18" /></div>
        <div><span>历史使用记录</span><strong>{{ myRecords.filter((item) => item.status === 'returned').length }}</strong></div>
      </article>
    </section>

    <section v-if="isAdmin" class="panel">
      <el-tabs v-model="adminTab">
        <el-tab-pane :label="`待处理 ${pendingRecords.length}`" name="pending">
          <el-table :data="pendingRecords" stripe>
            <el-table-column label="资产" min-width="230">
              <template #default="{ row }">
                <div class="table-primary compact">
                  <div class="asset-symbol">
                    <ClipboardCheck :size="17" />
                  </div>
                  <div><strong>{{ row.assetName }}</strong><span>{{ row.assetNo }}</span></div>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="applicantName" label="申请人" width="110" />
            <el-table-column prop="departmentName" label="使用部门" width="125" />
            <el-table-column label="申请时间" width="165">
              <template #default="{ row }">{{ formatDate(row.requestedAt, true) }}</template>
            </el-table-column>
            <el-table-column prop="remark" label="申请说明" min-width="180" show-overflow-tooltip />
            <el-table-column label="操作" width="165" fixed="right">
              <template #default="{ row }">
                <el-button type="primary" size="small" @click="approve(row)">
                  <Check :size="14" />
                  通过
                </el-button>
                <el-button size="small" @click="reject(row)">
                  <X :size="14" />
                  驳回
                </el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="!pendingRecords.length" description="暂无待处理领用申请" />
        </el-tab-pane>

        <el-tab-pane :label="`使用中 ${activeRecords.length}`" name="active">
          <el-table :data="activeRecords" stripe>
            <el-table-column label="资产" min-width="230">
              <template #default="{ row }">
                <div class="table-primary compact">
                  <div class="asset-symbol"><ClipboardCheck :size="17" /></div>
                  <div><strong>{{ row.assetName }}</strong><span>{{ row.assetNo }}</span></div>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="ownerName" label="当前使用人" width="120" />
            <el-table-column prop="departmentName" label="使用部门" width="125" />
            <el-table-column label="领用时间" width="165">
              <template #default="{ row }">{{ formatDate(row.loanDate, true) }}</template>
            </el-table-column>
            <el-table-column label="状态" width="145">
              <template #default="{ row }"><StatusTag :status="row.status" type="loan" /></template>
            </el-table-column>
            <el-table-column label="操作" width="130" fixed="right">
              <template #default="{ row }">
                <el-button
                  v-if="row.status === 'return_pending'"
                  type="primary"
                  size="small"
                  @click="confirmRecordReturn(row)"
                >
                  <Check :size="14" />
                  确认归还
                </el-button>
                <span v-else class="muted-cell">等待员工归还</span>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="!activeRecords.length" description="暂无使用中的资产" />
        </el-tab-pane>

        <el-tab-pane :label="`历史记录 ${historyRecords.length}`" name="history">
          <el-table :data="historyRecords" stripe>
            <el-table-column prop="assetNo" label="资产编号" width="135" />
            <el-table-column prop="assetName" label="资产名称" min-width="210" />
            <el-table-column prop="applicantName" label="申请人" width="105" />
            <el-table-column label="领用时间" width="165">
              <template #default="{ row }">{{ formatDate(row.loanDate, true) }}</template>
            </el-table-column>
            <el-table-column label="归还时间" width="165">
              <template #default="{ row }">{{ formatDate(row.returnDate, true) }}</template>
            </el-table-column>
            <el-table-column label="状态" width="100">
              <template #default="{ row }"><StatusTag :status="row.status" type="loan" /></template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </section>

    <section v-else class="panel">
      <el-tabs v-model="employeeTab">
        <el-tab-pane :label="`我的申请 ${myRecords.length}`" name="mine">
          <el-table :data="myRecords" stripe>
            <el-table-column label="资产" min-width="230">
              <template #default="{ row }">
                <div class="table-primary compact">
                  <div class="asset-symbol"><ClipboardCheck :size="17" /></div>
                  <div><strong>{{ row.assetName }}</strong><span>{{ row.assetNo }}</span></div>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="申请时间" width="165">
              <template #default="{ row }">{{ formatDate(row.requestedAt, true) }}</template>
            </el-table-column>
            <el-table-column label="处理时间" width="165">
              <template #default="{ row }">{{ formatDate(row.approvedAt, true) }}</template>
            </el-table-column>
            <el-table-column label="状态" width="145">
              <template #default="{ row }"><StatusTag :status="row.status" type="loan" /></template>
            </el-table-column>
            <el-table-column label="操作" width="130" fixed="right">
              <template #default="{ row }">
                <el-button
                  v-if="row.status === 'active'"
                  type="primary"
                  size="small"
                  @click="requestRecordReturn(row)"
                >
                  <RotateCcw :size="14" />
                  申请归还
                </el-button>
                <span v-else-if="row.status === 'return_pending'" class="muted-cell">等待确认</span>
                <span v-else class="muted-cell">-</span>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane :label="`使用中 ${myActiveRecords.length}`" name="active">
          <div class="asset-card-grid">
            <article v-for="item in myActiveRecords" :key="item.id" class="my-asset-card">
              <div class="asset-symbol large"><ClipboardCheck :size="23" /></div>
              <div class="my-asset-main">
                <span>{{ item.assetNo }}</span>
                <h3>{{ item.assetName }}</h3>
                <p>{{ categoryName(item.asset?.categoryId) }} · {{ item.departmentName }}</p>
                <StatusTag :status="item.status" type="loan" />
              </div>
              <el-button
                v-if="item.status === 'active'"
                type="primary"
                plain
                @click="requestRecordReturn(item)"
              >
                <RotateCcw :size="15" />
                申请归还
              </el-button>
              <el-button v-else disabled>
                <Clock3 :size="15" />
                等待管理员确认
              </el-button>
            </article>
          </div>
          <el-empty v-if="!myActiveRecords.length" description="当前没有使用中的资产" />
        </el-tab-pane>
      </el-tabs>
    </section>

    <el-dialog v-model="applyVisible" title="申请领用资产" width="560px" destroy-on-close>
      <el-form ref="formRef" :model="form" label-position="top">
        <el-form-item
          label="选择闲置资产"
          prop="assetId"
          :rules="[{ required: true, message: '请选择资产' }]"
        >
          <el-select v-model="form.assetId" filterable placeholder="请选择需要领用的资产">
            <el-option
              v-for="asset in idleAssets"
              :key="asset.id"
              :label="`${asset.assetNo} · ${asset.name}`"
              :value="asset.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="使用部门">
          <el-select v-model="form.departmentId" disabled>
            <el-option
              v-for="department in demoState.departments"
              :key="department.id"
              :label="department.name"
              :value="department.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="申请说明">
          <el-input
            v-model="form.remark"
            type="textarea"
            :rows="3"
            placeholder="请简要说明领用用途"
          />
        </el-form-item>
        <div class="form-tip">
          <Send :size="16" />
          <span>提交后由管理员处理，确认领用时资产状态才会变为“在用”。</span>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="applyVisible = false">取消</el-button>
        <el-button type="primary" @click="submitApply">提交申请</el-button>
      </template>
    </el-dialog>
  </div>
</template>
