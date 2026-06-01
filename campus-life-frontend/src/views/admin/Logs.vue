<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { cleanLogs, getExceptionLogs, getOperationLogs } from '../../api/admin'
import { LOG_ACTION_TEXT } from '../../constants/enums'
import { useAuthStore } from '../../stores/auth'

const authStore = useAuthStore()
const activeTab = ref('operation')
const loading = ref(false)
const operationTotal = ref(0)
const exceptionTotal = ref(0)
const operationLogs = ref([])
const exceptionLogs = ref([])

const operationFilters = reactive({
  action: '',
  targetType: '',
  startDate: '',
  endDate: '',
  page: 1,
  pageSize: 20,
})

const exceptionFilters = reactive({
  exceptionType: '',
  startDate: '',
  endDate: '',
  page: 1,
  pageSize: 20,
})

const cleanForm = reactive({
  logType: 'all',
  beforeDate: '',
})

async function loadOperationLogs() {
  loading.value = true
  try {
    const res = await getOperationLogs(operationFilters, { silentError: true })
    const data = res.data || {}
    operationLogs.value = data.list || []
    operationTotal.value = data.total || 0
  } finally {
    loading.value = false
  }
}

async function loadExceptionLogs() {
  if (authStore.role !== 'super_admin') {
    exceptionLogs.value = []
    exceptionTotal.value = 0
    return
  }

  loading.value = true
  try {
    const res = await getExceptionLogs(exceptionFilters, { silentError: true })
    const data = res.data || {}
    exceptionLogs.value = data.list || []
    exceptionTotal.value = data.total || 0
  } finally {
    loading.value = false
  }
}

function searchOperationLogs() {
  operationFilters.page = 1
  loadOperationLogs()
}

function searchExceptionLogs() {
  exceptionFilters.page = 1
  loadExceptionLogs()
}

async function clean() {
  if (authStore.role !== 'super_admin') {
    ElMessage.error('仅超级管理员可以清理日志')
    return
  }

  try {
    await ElMessageBox.confirm('清理日志属于高风险操作，确认继续？', '清理日志确认', {
      confirmButtonText: '确认清理',
      cancelButtonText: '取消',
      type: 'warning',
    })
  } catch {
    return
  }

  const res = await cleanLogs(cleanForm)
  ElMessage.success(`日志清理成功，删除 ${res.data?.deletedCount || 0} 条`)
  loadOperationLogs()
  loadExceptionLogs()
}

function changeOperationPage(page) {
  operationFilters.page = page
  loadOperationLogs()
}

function changeExceptionPage(page) {
  exceptionFilters.page = page
  loadExceptionLogs()
}

onMounted(() => {
  loadOperationLogs()
  loadExceptionLogs()
})
</script>

<template>
  <section>
    <header class="page-header">
      <div>
        <h1 class="page-title">日志管理</h1>
        <p class="page-subtitle">查询管理操作记录与异常行为证据</p>
      </div>
    </header>

    <el-tabs v-model="activeTab" class="log-tabs">
      <el-tab-pane label="操作日志" name="operation">
        <div class="panel filter-panel">
          <el-select v-model="operationFilters.action" clearable placeholder="操作类型">
            <el-option v-for="(label, value) in LOG_ACTION_TEXT" :key="value" :label="label" :value="value" />
          </el-select>
          <el-select v-model="operationFilters.targetType" clearable placeholder="目标类型">
            <el-option label="用户" value="user" />
            <el-option label="公告" value="announcement" />
            <el-option label="配置" value="config" />
          </el-select>
          <el-date-picker v-model="operationFilters.startDate" type="date" value-format="YYYY-MM-DD" placeholder="开始日期" />
          <el-date-picker v-model="operationFilters.endDate" type="date" value-format="YYYY-MM-DD" placeholder="结束日期" />
          <el-button type="primary" :loading="loading" @click="searchOperationLogs">查询</el-button>
        </div>

        <div class="panel table-panel">
          <el-table v-loading="loading" :data="operationLogs" stripe>
            <el-table-column prop="logId" label="ID" width="90" />
            <el-table-column prop="adminNickname" label="操作者" width="130" />
            <el-table-column label="操作" width="150">
              <template #default="{ row }">{{ row.actionText || LOG_ACTION_TEXT[row.action] || row.action }}</template>
            </el-table-column>
            <el-table-column prop="targetNickname" label="目标" min-width="140" />
            <el-table-column prop="reasonText" label="原因" width="120" />
            <el-table-column prop="ipAddress" label="IP地址" width="150" />
            <el-table-column prop="createTime" label="时间" width="180" />
          </el-table>
          <div class="pagination">
            <el-pagination
              layout="prev, pager, next, total"
              :current-page="operationFilters.page"
              :page-size="operationFilters.pageSize"
              :total="operationTotal"
              @current-change="changeOperationPage"
            />
          </div>
        </div>
      </el-tab-pane>

      <el-tab-pane label="异常日志" name="exception">
        <el-alert
          v-if="authStore.role !== 'super_admin'"
          title="异常日志仅超级管理员可查看。"
          type="warning"
          :closable="false"
          class="permission-alert"
        />

        <template v-else>
          <div class="panel filter-panel exception-filter">
            <el-select v-model="exceptionFilters.exceptionType" clearable placeholder="异常类型">
              <el-option label="异常打卡" value="abnormal_checkin" />
              <el-option label="SQL注入" value="sql_injection" />
              <el-option label="脚本注入" value="xss" />
            </el-select>
            <el-date-picker v-model="exceptionFilters.startDate" type="date" value-format="YYYY-MM-DD" placeholder="开始日期" />
            <el-date-picker v-model="exceptionFilters.endDate" type="date" value-format="YYYY-MM-DD" placeholder="结束日期" />
            <el-button type="primary" :loading="loading" @click="searchExceptionLogs">查询</el-button>
          </div>

          <div class="panel table-panel">
            <el-table v-loading="loading" :data="exceptionLogs" stripe>
              <el-table-column prop="logId" label="ID" width="90" />
              <el-table-column prop="userId" label="用户ID" width="140" />
              <el-table-column prop="exceptionType" label="异常类型" width="140" />
              <el-table-column prop="exceptionDetail" label="异常详情" min-width="320" show-overflow-tooltip />
              <el-table-column prop="requestUrl" label="请求地址" width="180" />
              <el-table-column prop="ipAddress" label="IP地址" width="150" />
              <el-table-column prop="createTime" label="时间" width="180" />
            </el-table>
            <div class="pagination">
              <el-pagination
                layout="prev, pager, next, total"
                :current-page="exceptionFilters.page"
                :page-size="exceptionFilters.pageSize"
                :total="exceptionTotal"
                @current-change="changeExceptionPage"
              />
            </div>
          </div>
        </template>
      </el-tab-pane>

      <el-tab-pane label="清理日志" name="clean">
        <div class="panel form-panel">
          <el-alert title="仅超级管理员可清理日志，操作前请确认清理范围。" type="warning" :closable="false" class="permission-alert" />
          <el-form :model="cleanForm" label-position="top">
            <el-form-item label="日志类型">
              <el-select v-model="cleanForm.logType" class="full-width">
                <el-option label="全部" value="all" />
                <el-option label="操作日志" value="operation" />
                <el-option label="异常日志" value="exception" />
              </el-select>
            </el-form-item>
            <el-form-item label="清理此日期之前的日志">
              <el-date-picker v-model="cleanForm.beforeDate" class="full-width" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" />
            </el-form-item>
          </el-form>
          <el-button type="danger" @click="clean">清理日志</el-button>
        </div>
      </el-tab-pane>
    </el-tabs>
  </section>
</template>

<style scoped>
.page-header {
  margin-bottom: 18px;
}

.filter-panel {
  display: grid;
  grid-template-columns: 180px 180px 180px 180px auto;
  gap: 12px;
  margin-bottom: 18px;
  padding: 18px;
}

.exception-filter {
  grid-template-columns: 180px 180px 180px auto;
}

.table-panel,
.form-panel {
  padding: 18px;
}

.form-panel {
  max-width: 520px;
}

.permission-alert {
  margin-bottom: 18px;
}

.pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 18px;
}

.full-width {
  width: 100%;
}
</style>
