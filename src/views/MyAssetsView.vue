<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  ArrowRight,
  Boxes,
  CalendarDays,
  CircleDollarSign,
  PackageCheck,
  RotateCcw,
} from 'lucide-vue-next'
import {
  categoryName,
  demoState,
} from '@/stores/demoStore'
import {
  getCurrentEmployee,
  getDepartment,
  myAssets,
  requestReturn,
} from '@/stores/demoStore'
import { formatCurrency, formatDate } from '@/utils/format'

const router = useRouter()
const employee = computed(() => getCurrentEmployee())
const department = computed(() => getDepartment(employee.value?.departmentId))
const activeLoans = computed(() =>
  demoState.loanRecords.filter(
    (item) =>
      item.applicantId === employee.value?.id &&
      ['active', 'return_pending'].includes(item.status),
  ),
)
const pendingCount = computed(
  () =>
    demoState.loanRecords.filter(
      (item) =>
        item.applicantId === employee.value?.id && ['pending', 'return_pending'].includes(item.status),
    ).length,
)

function loanForAsset(assetId) {
  return activeLoans.value.find((item) => item.assetId === assetId)
}

function handleReturn(asset) {
  const loan = loanForAsset(asset.id)
  if (!loan) return
  try {
    requestReturn(loan.id)
    ElMessage.success('归还申请已提交，等待管理员确认')
  } catch (error) {
    ElMessage.error(error.message)
  }
}
</script>

<template>
  <div class="page-stack">
    <section class="welcome-banner">
      <div class="welcome-copy">
        <span class="eyebrow">PERSONAL WORKSPACE</span>
        <h2>你好，{{ employee?.name || demoState.currentUser?.realName }}</h2>
        <p>
          你当前归属{{ department?.name || '未设置部门' }}，正在使用
          <strong>{{ myAssets.length }}</strong> 项固定资产。
        </p>
      </div>
      <el-button type="primary" plain @click="router.push('/loans')">
        前往领用服务
        <ArrowRight :size="16" />
      </el-button>
    </section>

    <section class="compact-metrics">
      <article>
        <div class="compact-icon success"><PackageCheck :size="18" /></div>
        <div><span>正在使用</span><strong>{{ myAssets.length }}</strong></div>
      </article>
      <article>
        <div class="compact-icon warning"><CalendarDays :size="18" /></div>
        <div><span>待处理事项</span><strong>{{ pendingCount }}</strong></div>
      </article>
      <article>
        <div class="compact-icon primary"><CircleDollarSign :size="18" /></div>
        <div>
          <span>资产原值</span>
          <strong class="metric-money">
            {{ formatCurrency(myAssets.reduce((sum, item) => sum + Number(item.originalValue), 0)) }}
          </strong>
        </div>
      </article>
    </section>

    <section class="panel">
      <div class="panel-heading">
        <div>
          <span class="eyebrow">MY ASSETS</span>
          <h3>我正在使用的资产</h3>
        </div>
        <span class="panel-meta">{{ myAssets.length }} 项</span>
      </div>

      <div class="asset-card-grid">
        <article v-for="asset in myAssets" :key="asset.id" class="my-asset-card detailed">
          <div class="asset-card-head">
            <div class="asset-symbol large"><Boxes :size="23" /></div>
            <el-tag type="success" round>在用</el-tag>
          </div>
          <div class="my-asset-main">
            <span>{{ asset.assetNo }}</span>
            <h3>{{ asset.name }}</h3>
            <p>{{ categoryName(asset.categoryId) }} · {{ asset.brandModel }}</p>
          </div>
          <div class="asset-card-meta">
            <span>购买日期</span>
            <strong>{{ formatDate(asset.purchaseDate) }}</strong>
          </div>
          <el-button
            type="primary"
            plain
            :disabled="loanForAsset(asset.id)?.status === 'return_pending'"
            @click="handleReturn(asset)"
          >
            <component
              :is="loanForAsset(asset.id)?.status === 'return_pending' ? CalendarDays : RotateCcw"
              :size="15"
            />
            {{ loanForAsset(asset.id)?.status === 'return_pending' ? '等待管理员确认' : '申请归还' }}
          </el-button>
        </article>
      </div>
      <el-empty v-if="!myAssets.length" description="当前没有使用中的资产">
        <el-button type="primary" @click="router.push('/loans')">提交领用申请</el-button>
      </el-empty>
    </section>
  </div>
</template>
