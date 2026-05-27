<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { freezeUser, getUsers, unfreezeUser } from '../../api/admin'
import { FREEZE_REASONS, USER_STATUS_TEXT } from '../../constants/enums'

const filters = reactive({
  keyword: '',
  status: '',
  page: 1,
  pageSize: 20,
})

const loading = ref(false)
const total = ref(2)
const freezeDialogVisible = ref(false)
const currentUser = ref(null)
const freezeForm = reactive({
  reason: '',
})

const users = ref([
  { userId: 'usr_000001', phone: '138****8000', nickname: '学霸小明', gender: '男', grade: '大二', status: 'normal', createTime: '2026-01-01 10:00:00', lastLoginTime: '2026-05-25 08:30:00' },
  { userId: 'usr_000002', phone: '139****1234', nickname: '用户_A1b2C3d4', gender: '女', grade: '大一', status: 'frozen', createTime: '2026-02-05 13:20:00', lastLoginTime: '2026-05-20 19:12:00' },
])

async function loadUsers() {
  loading.value = true

  try {
    const res = await getUsers(filters, { silentError: true })
    const data = res.data || {}

    users.value = data.list || users.value
    total.value = data.total ?? users.value.length
    filters.page = data.page || filters.page
    filters.pageSize = data.pageSize || filters.pageSize
  } finally {
    loading.value = false
  }
}

function searchUsers() {
  filters.page = 1
  loadUsers()
}

function openFreezeDialog(row) {
  currentUser.value = row
  freezeForm.reason = ''
  freezeDialogVisible.value = true
}

async function freeze() {
  if (!freezeForm.reason) {
    ElMessage.error('请选择冻结原因')
    return
  }

  await freezeUser(currentUser.value.userId, { reason: freezeForm.reason })
  freezeDialogVisible.value = false
  ElMessage.success('冻结成功')
  loadUsers()
}

async function unfreeze(row) {
  try {
    await ElMessageBox.confirm('确认解封该账号？', '解封确认', {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      type: 'warning',
    })
  } catch {
    return
  }

  await unfreezeUser(row.userId)
  ElMessage.success('解封成功')
  loadUsers()
}

function handlePageChange(page) {
  filters.page = page
  loadUsers()
}

onMounted(loadUsers)
</script>

<template>
  <section>
    <header class="page-header">
      <div>
        <h1 class="page-title">用户管理</h1>
        <p class="page-subtitle">检索、冻结或解封学生账号</p>
      </div>
    </header>

    <div class="panel filter-panel">
      <el-input v-model="filters.keyword" clearable placeholder="搜索用户ID、手机号、昵称">
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
      <el-select v-model="filters.status" clearable placeholder="全部状态">
        <el-option label="正常" value="normal" />
        <el-option label="已冻结" value="frozen" />
      </el-select>
      <el-button type="primary" :loading="loading" @click="searchUsers">查询</el-button>
    </div>

    <div class="panel table-panel">
      <el-table v-loading="loading" :data="users" stripe>
        <el-table-column prop="userId" label="用户ID" width="140" />
        <el-table-column prop="phone" label="手机号" width="140" />
        <el-table-column prop="nickname" label="昵称" />
        <el-table-column prop="gender" label="性别" width="90" />
        <el-table-column prop="grade" label="年级" width="100" />
        <el-table-column prop="createTime" label="注册时间" width="180" />
        <el-table-column prop="lastLoginTime" label="最后登录" width="180" />
        <el-table-column label="冻结原因" width="140">
          <template #default="{ row }">
            <span class="muted-text">{{ row.status === 'frozen' ? row.freezeReason || '未记录' : '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="row.status === 'normal' ? 'success' : 'danger'">
              {{ USER_STATUS_TEXT[row.status] || row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 'normal'" link type="danger" @click="openFreezeDialog(row)">冻结</el-button>
            <el-button v-else link type="primary" @click="unfreeze(row)">解封</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination">
        <el-pagination
          layout="prev, pager, next, total"
          :current-page="filters.page"
          :page-size="filters.pageSize"
          :total="total"
          @current-change="handlePageChange"
        />
      </div>
    </div>

    <el-dialog v-model="freezeDialogVisible" title="冻结账号" width="420px">
      <el-form :model="freezeForm" label-position="top">
        <el-form-item label="冻结原因" required>
          <el-select v-model="freezeForm.reason" class="full-width" placeholder="请选择冻结原因">
            <el-option v-for="item in FREEZE_REASONS" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="freezeDialogVisible = false">取消</el-button>
        <el-button type="danger" @click="freeze">确认冻结</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<style scoped>
.page-header {
  margin-bottom: 24px;
}

.filter-panel {
  display: grid;
  grid-template-columns: 1fr 180px auto;
  gap: 12px;
  margin-bottom: 18px;
  padding: 18px;
}

.table-panel {
  padding: 18px;
}

.pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 18px;
}

.full-width {
  width: 100%;
}

.muted-text {
  color: var(--text-muted);
}
</style>
