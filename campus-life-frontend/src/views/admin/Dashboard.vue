<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { getDashboard } from '../../api/admin'
import BaseChart from '../../components/BaseChart.vue'

const loading = ref(false)
const dashboard = reactive({
  totalUsers: 5000,
  dailyActiveUsers: 1200,
  dailyScheduleAdds: 350,
  dailyHealthLogs: 800,
})

const genderDistribution = ref([
  { name: '男', value: 2800 },
  { name: '女', value: 2200 },
])

const gradeDistribution = ref([
  { name: '大一', value: 1400 },
  { name: '大二', value: 1300 },
  { name: '大三', value: 1200 },
  { name: '大四', value: 1100 },
])

const departmentDistribution = ref([
  { name: '计算机学院', value: 800 },
  { name: '软件学院', value: 600 },
  { name: '自动化学院', value: 520 },
  { name: '管理学院', value: 460 },
])

const stats = computed(() => [
  { label: '总注册用户', value: formatNumber(dashboard.totalUsers), trend: '累计' },
  { label: '今日活跃 DAU', value: formatNumber(dashboard.dailyActiveUsers), trend: '今日' },
  { label: '今日日程添加', value: formatNumber(dashboard.dailyScheduleAdds), trend: '今日' },
  { label: '今日健康打卡', value: formatNumber(dashboard.dailyHealthLogs), trend: '今日' },
])

function formatNumber(value) {
  return Number(value || 0).toLocaleString()
}

async function loadDashboard() {
  loading.value = true

  try {
    const res = await getDashboard({ silentError: true })
    const data = res.data || {}

    Object.assign(dashboard, {
      totalUsers: data.totalUsers ?? dashboard.totalUsers,
      dailyActiveUsers: data.dailyActiveUsers ?? dashboard.dailyActiveUsers,
      dailyScheduleAdds: data.dailyScheduleAdds ?? dashboard.dailyScheduleAdds,
      dailyHealthLogs: data.dailyHealthLogs ?? dashboard.dailyHealthLogs,
    })
    genderDistribution.value = data.genderDistribution || genderDistribution.value
    gradeDistribution.value = data.gradeDistribution || gradeDistribution.value
    departmentDistribution.value = data.departmentDistribution || departmentDistribution.value
  } finally {
    loading.value = false
  }
}

const genderOption = computed(() => ({
  tooltip: { trigger: 'item' },
  series: [
    {
      type: 'pie',
      radius: ['42%', '70%'],
      label: { formatter: '{b}' },
      itemStyle: { borderColor: '#fff', borderWidth: 2 },
      data: genderDistribution.value,
    },
  ],
}))

const gradeOption = computed(() => ({
  tooltip: {
    trigger: 'axis',
    axisPointer: { type: 'shadow' },
    formatter: (params) => {
      const item = params[0]
      return `${item.name}<br/>人数：${formatNumber(item.value)}`
    },
  },
  grid: { top: 24, right: 16, bottom: 30, left: 40 },
  xAxis: { type: 'category', data: gradeDistribution.value.map((item) => item.name) },
  yAxis: { type: 'value' },
  series: [{ type: 'bar', data: gradeDistribution.value.map((item) => item.value), itemStyle: { color: '#2563eb' } }],
}))

const departmentOption = computed(() => ({
  tooltip: {
    trigger: 'axis',
    axisPointer: { type: 'shadow' },
    formatter: (params) => {
      const item = params[0]
      return `${item.name}<br/>人数：${formatNumber(item.value)}`
    },
  },
  grid: { top: 20, right: 24, bottom: 20, left: 90 },
  xAxis: { type: 'value' },
  yAxis: { type: 'category', data: departmentDistribution.value.map((item) => item.name) },
  series: [{ type: 'bar', data: departmentDistribution.value.map((item) => item.value), itemStyle: { color: '#10b981' } }],
}))

onMounted(loadDashboard)
</script>

<template>
  <section v-loading="loading">
    <header class="page-header">
      <div>
        <h1 class="page-title">数据总览</h1>
        <p class="page-subtitle">实时监控全站运行指标</p>
      </div>
      <el-tag type="success" size="large">系统运行状态：良好</el-tag>
    </header>

    <div class="stats-grid">
      <div v-for="item in stats" :key="item.label" class="stat-card panel">
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
        <p>{{ item.trend }}</p>
      </div>
    </div>

    <div class="chart-grid">
      <div class="panel chart-panel">
        <h2>用户性别比例</h2>
        <BaseChart :option="genderOption" />
      </div>
      <div class="panel chart-panel">
        <h2>各年级分布</h2>
        <BaseChart :option="gradeOption" />
      </div>
      <div class="panel chart-panel">
        <h2>院系注册人数分布</h2>
        <BaseChart :option="departmentOption" />
      </div>
    </div>
  </section>
</template>

<style scoped>
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
}

.stats-grid,
.chart-grid {
  display: grid;
  gap: 18px;
}

.stats-grid {
  grid-template-columns: repeat(4, minmax(0, 1fr));
  margin-bottom: 18px;
}

.chart-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.stat-card,
.chart-panel {
  padding: 22px;
}

.stat-card span {
  color: var(--text-muted);
  font-size: 13px;
}

.stat-card strong {
  display: block;
  margin-top: 12px;
  font-size: 30px;
}

.stat-card p {
  margin: 10px 0 0;
  color: #16a34a;
  font-size: 13px;
}

.chart-panel h2 {
  margin: 0 0 12px;
  font-size: 17px;
}
</style>
