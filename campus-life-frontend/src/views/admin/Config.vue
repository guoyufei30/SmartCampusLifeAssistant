<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getConfig, updateConfig } from '../../api/admin'

const loading = ref(false)
const config = reactive({
  health: {
    defaultExerciseTarget: 150,
  },
  api: {
    apiWeights: {
      courseHours: 30,
      examCount: 15,
      ddlCount: 5,
      completionRate: 20,
    },
  },
  system: {
    logRetentionDays: 90,
    abnormalCheckinThreshold: 50,
    procrastinationThreshold: 70,
  },
})

async function loadConfig() {
  loading.value = true

  try {
    const res = await getConfig({ silentError: true })
    const data = res.data || {}

    Object.assign(config.health, data.health || {})
    Object.assign(config.api.apiWeights, data.api?.apiWeights || {})
    Object.assign(config.system, data.system || {})
  } finally {
    loading.value = false
  }
}

async function saveConfig() {
  try {
    await ElMessageBox.confirm('配置保存后会影响全站新用户的运动目标，以及后续学业压力指数计算。确认保存？', '保存系统配置', {
      confirmButtonText: '确认保存',
      cancelButtonText: '取消',
      type: 'warning',
    })
  } catch {
    return
  }

  await updateConfig(config)
  ElMessage.success('配置保存成功')
  loadConfig()
}

onMounted(loadConfig)
</script>

<template>
  <section v-loading="loading">
    <header class="page-header">
      <h1 class="page-title">系统配置</h1>
      <p class="page-subtitle">维护健康目标和学业压力指数参数</p>
    </header>

    <div class="config-grid">
      <div class="panel config-panel">
        <h2>健康目标参数</h2>
        <el-form label-position="top">
          <el-form-item label="默认每周运动目标（分钟）">
            <el-input-number v-model="config.health.defaultExerciseTarget" :min="1" :max="600" />
          </el-form-item>
        </el-form>
      </div>

      <div class="panel config-panel">
        <h2>学业压力指数权重</h2>
        <el-form label-position="top" class="weight-form">
          <el-form-item label="课程时长权重">
            <el-input-number v-model="config.api.apiWeights.courseHours" :min="0" :max="100" />
          </el-form-item>
          <el-form-item label="考试数量权重">
            <el-input-number v-model="config.api.apiWeights.examCount" :min="0" :max="100" />
          </el-form-item>
          <el-form-item label="DDL数量权重">
            <el-input-number v-model="config.api.apiWeights.ddlCount" :min="0" :max="100" />
          </el-form-item>
          <el-form-item label="完成率权重">
            <el-input-number v-model="config.api.apiWeights.completionRate" :min="0" :max="100" />
          </el-form-item>
        </el-form>
      </div>

      <div class="panel config-panel">
        <h2>系统风控参数</h2>
        <el-form label-position="top" class="weight-form">
          <el-form-item label="日志保留天数">
            <el-input-number v-model="config.system.logRetentionDays" :min="1" :max="3650" />
          </el-form-item>
          <el-form-item label="异常打卡阈值（次/分钟）">
            <el-input-number v-model="config.system.abnormalCheckinThreshold" :min="1" :max="1000" />
          </el-form-item>
          <el-form-item label="拖延症预警阈值（%）">
            <el-input-number v-model="config.system.procrastinationThreshold" :min="0" :max="100" />
          </el-form-item>
        </el-form>
      </div>
    </div>

    <div class="actions">
      <el-button type="primary" @click="saveConfig">保存配置</el-button>
    </div>
  </section>
</template>

<style scoped>
.page-header {
  margin-bottom: 24px;
}

.config-grid {
  display: grid;
  max-width: 900px;
  gap: 18px;
}

.config-panel {
  padding: 24px;
}

.config-panel h2 {
  margin: 0 0 18px;
  font-size: 18px;
}

.weight-form {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 18px;
}

.actions {
  margin-top: 18px;
}
</style>
