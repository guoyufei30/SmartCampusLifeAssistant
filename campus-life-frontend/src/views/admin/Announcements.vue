<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { createAnnouncement, getAnnouncements, offlineAnnouncement } from '../../api/admin'
import { ANNOUNCEMENT_STATUS_TEXT, ANNOUNCEMENT_TYPE_TEXT } from '../../constants/enums'

const dialogVisible = ref(false)
const loading = ref(false)
const form = reactive({
  content: '',
  type: 'system',
})

const announcements = ref([
  { id: 1, content: '系统将于今晚23:00进行维护，请提前保存数据。', type: 'system', status: 'online', createTime: '2026-05-25 09:00:00' },
  { id: 2, content: '运动进度落后，明天下午14:00-16:00没课，去操场跑两圈吧？', type: 'sport', status: 'offline', createTime: '2026-05-24 18:30:00' },
])

async function loadAnnouncements() {
  loading.value = true

  try {
    const res = await getAnnouncements({ silentError: true })
    announcements.value = Array.isArray(res.data) ? res.data : announcements.value
  } finally {
    loading.value = false
  }
}

function openDialog() {
  form.content = ''
  form.type = 'system'
  dialogVisible.value = true
}

async function publish() {
  if (!form.content.trim()) {
    ElMessage.error('公告正文不能为空')
    return
  }

  await createAnnouncement({
    content: form.content.trim(),
    type: form.type,
  })
  dialogVisible.value = false
  ElMessage.success('发布成功')
  loadAnnouncements()
}

async function offline(row) {
  try {
    await ElMessageBox.confirm('下架后学生端将停止展示该公告，后台仍会保留历史记录。确认下架？', '下架公告确认', {
      confirmButtonText: '确认下架',
      cancelButtonText: '取消',
      type: 'warning',
    })
  } catch {
    return
  }

  await offlineAnnouncement(row.id)
  ElMessage.success('下架成功')
  loadAnnouncements()
}

onMounted(loadAnnouncements)
</script>

<template>
  <section>
    <header class="page-header">
      <div>
        <h1 class="page-title">公告管理</h1>
        <p class="page-subtitle">发布系统公告和运动提醒</p>
      </div>
      <el-button type="primary" @click="openDialog">发布公告</el-button>
    </header>

    <div class="panel table-panel">
      <el-table v-loading="loading" :data="announcements" stripe>
        <el-table-column prop="content" label="公告内容" min-width="360" />
        <el-table-column label="类型" width="120">
          <template #default="{ row }">
            <el-tag :type="row.type === 'system' ? 'primary' : 'success'">
              {{ ANNOUNCEMENT_TYPE_TEXT[row.type] || row.type }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="row.status === 'online' ? 'success' : 'info'">
              {{ ANNOUNCEMENT_STATUS_TEXT[row.status] || row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="发布时间" width="180" />
        <el-table-column label="操作" width="110">
          <template #default="{ row }">
            <el-button :disabled="row.status === 'offline'" link type="danger" @click="offline(row)">下架</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dialogVisible" title="发布公告" width="520px">
      <el-form :model="form" label-position="top">
        <el-form-item label="公告类型">
          <el-select v-model="form.type" class="full-width">
            <el-option label="系统公告" value="system" />
            <el-option label="运动提醒" value="sport" />
          </el-select>
        </el-form-item>
        <el-form-item label="公告正文">
          <el-input v-model="form.content" type="textarea" maxlength="500" show-word-limit :rows="5" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="publish">发布</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<style scoped>
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 24px;
}

.table-panel {
  padding: 18px;
}

.full-width {
  width: 100%;
}
</style>
