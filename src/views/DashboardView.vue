<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import * as echarts from 'echarts/core'
import { BarChart, PieChart } from 'echarts/charts'
import { GridComponent, LegendComponent, TooltipComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import {
  AlertTriangle,
  Archive,
  Banknote,
  Boxes,
  PackageCheck,
  RefreshCw,
} from 'lucide-vue-next'
import StatusTag from '@/components/StatusTag.vue'
import {
  dashboardStats,
  demoState,
  getAsset,
  getAssetStatusMeta,
} from '@/stores/demoStore'
import { formatCurrency, formatDate } from '@/utils/format'

echarts.use([BarChart, PieChart, GridComponent, LegendComponent, TooltipComponent, CanvasRenderer])

const departmentChartRef = ref(null)
const statusChartRef = ref(null)
let departmentChart
let statusChart

const statCards = computed(() => [
  {
    label: '资产总数',
    value: dashboardStats.value.total,
    suffix: '台/件',
    icon: Boxes,
    tone: 'teal',
  },
  {
    label: '在用资产',
    value: dashboardStats.value.inUse,
    suffix: '台/件',
    icon: PackageCheck,
    tone: 'green',
  },
  {
    label: '闲置资产',
    value: dashboardStats.value.idle,
    suffix: '台/件',
    icon: Archive,
    tone: 'gray',
  },
  {
    label: '维修与报废',
    value: dashboardStats.value.maintenance + dashboardStats.value.scrapped,
    suffix: '台/件',
    icon: AlertTriangle,
    tone: 'orange',
  },
])

const recentChanges = computed(() => demoState.changeRecords.slice(0, 7))

function renderDepartmentChart() {
  if (!departmentChartRef.value) return
  departmentChart ||= echarts.init(departmentChartRef.value)
  departmentChart.setOption({
    color: ['#16826f'],
    grid: { left: 18, right: 22, top: 20, bottom: 12, containLabel: true },
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter: (params) => `${params[0].name}<br/>资产数量：${params[0].value} 台/件`,
    },
    xAxis: {
      type: 'category',
      data: dashboardStats.value.departmentCounts.map((item) => item.name),
      axisLine: { lineStyle: { color: '#d9ded8' } },
      axisTick: { show: false },
      axisLabel: { color: '#61706e', interval: 0, fontSize: 11 },
    },
    yAxis: {
      type: 'value',
      minInterval: 1,
      splitLine: { lineStyle: { color: '#edf0ec', type: 'dashed' } },
      axisLabel: { color: '#84908e' },
    },
    series: [
      {
        type: 'bar',
        data: dashboardStats.value.departmentCounts.map((item) => item.value),
        barWidth: 28,
        itemStyle: {
          borderRadius: [5, 5, 0, 0],
          color: {
            type: 'linear',
            x: 0,
            y: 0,
            x2: 0,
            y2: 1,
            colorStops: [
              { offset: 0, color: '#16826f' },
              { offset: 1, color: '#79b8a5' },
            ],
          },
        },
      },
    ],
  })
}

function renderStatusChart() {
  if (!statusChartRef.value) return
  statusChart ||= echarts.init(statusChartRef.value)
  statusChart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}<br/>{c} 台/件（{d}%）' },
    legend: {
      bottom: 0,
      icon: 'circle',
      itemWidth: 9,
      textStyle: { color: '#61706e' },
    },
    series: [
      {
        type: 'pie',
        radius: ['54%', '76%'],
        center: ['50%', '43%'],
        avoidLabelOverlap: true,
        itemStyle: { borderColor: '#fff', borderWidth: 4, borderRadius: 4 },
        label: { show: false },
        data: dashboardStats.value.statusCounts.map((item) => ({
          name: item.name,
          value: item.value,
          itemStyle: { color: item.color },
        })),
      },
    ],
  })
}

function handleResize() {
  departmentChart?.resize()
  statusChart?.resize()
}

watch(
  dashboardStats,
  async () => {
    await nextTick()
    renderDepartmentChart()
    renderStatusChart()
  },
  { deep: true },
)

onMounted(() => {
  renderDepartmentChart()
  renderStatusChart()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  departmentChart?.dispose()
  statusChart?.dispose()
})
</script>

<template>
  <div class="page-stack">
    <section class="page-heading">
      <div>
        <span class="eyebrow">ASSET OVERVIEW</span>
        <h2>资产运行概览</h2>
        <p>聚合当前资产状态、部门分布与最近业务变化。</p>
      </div>
      <div class="heading-note">
        <RefreshCw :size="16" />
        <span>数据随演示操作实时更新</span>
      </div>
    </section>

    <section class="metric-grid">
      <article v-for="card in statCards" :key="card.label" class="metric-card">
        <div class="metric-icon" :class="card.tone">
          <component :is="card.icon" :size="21" />
        </div>
        <div class="metric-content">
          <span>{{ card.label }}</span>
          <div>
            <strong>{{ card.value }}</strong>
            <small>{{ card.suffix }}</small>
          </div>
        </div>
        <div class="metric-accent" :class="card.tone" />
      </article>
    </section>

    <section class="dashboard-chart-grid">
      <article class="panel chart-panel department-chart-panel">
        <div class="panel-heading">
          <div>
            <span class="eyebrow">DEPARTMENT</span>
            <h3>各部门资产数量</h3>
          </div>
          <span class="panel-meta">按当前所属部门统计</span>
        </div>
        <div ref="departmentChartRef" class="chart-canvas" />
      </article>

      <article class="panel chart-panel status-chart-panel">
        <div class="panel-heading">
          <div>
            <span class="eyebrow">STATUS</span>
            <h3>资产状态占比</h3>
          </div>
          <span class="panel-meta">实时状态</span>
        </div>
        <div ref="statusChartRef" class="chart-canvas" />
        <div class="status-total">
          <Banknote :size="17" />
          <span>资产原值</span>
          <strong>{{ formatCurrency(dashboardStats.originalValue) }}</strong>
        </div>
      </article>
    </section>

    <section class="panel recent-panel">
      <div class="panel-heading">
        <div>
          <span class="eyebrow">RECENT ACTIVITY</span>
          <h3>最近资产变动</h3>
        </div>
        <span class="panel-meta">{{ recentChanges.length }} 条近期记录</span>
      </div>

      <div v-if="recentChanges.length" class="activity-list">
        <div v-for="item in recentChanges" :key="item.id" class="activity-item">
          <div class="activity-symbol">
            <component
              :is="item.type.includes('领用') ? PackageCheck : item.type.includes('调拨') ? Archive : AlertTriangle"
              :size="17"
            />
          </div>
          <div class="activity-main">
            <strong>{{ item.description }}</strong>
            <span>
              {{ item.assetNo || getAsset(item.assetId)?.assetNo || '-' }}
              ·
              {{ item.operator || '系统' }}
            </span>
          </div>
          <div class="activity-side">
            <StatusTag v-if="getAsset(item.assetId)" :status="getAsset(item.assetId).status" />
            <time>{{ formatDate(item.createdAt, true) }}</time>
          </div>
        </div>
      </div>
      <el-empty v-else description="暂无资产变动记录" />
    </section>
  </div>
</template>
