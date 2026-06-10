<script setup>
import { reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { createAdmin } from '../../api/admin'
import { sendVerifyCode } from '../../api/auth'
import { useAuthStore } from '../../stores/auth'

const authStore = useAuthStore()
const loading = ref(false)
const codeLoading = ref(false)
const form = reactive({
  phone: '',
  password: '',
  nickname: '',
  verifyCode: '',
})

async function sendCode() {
  if (!form.phone) {
    ElMessage.error('请先输入手机号')
    return
  }

  codeLoading.value = true
  try {
    await sendVerifyCode({ phone: form.phone, type: 'admin_create' })
    ElMessage.success('验证码已发送')
  } finally {
    codeLoading.value = false
  }
}

async function submit() {
  if (!authStore.user || authStore.role !== 'super_admin') {
    ElMessage.error('仅超级管理员可以创建普通管理员')
    return
  }

  if (!form.phone || !form.password || !form.nickname || !form.verifyCode) {
    ElMessage.error('请填写完整管理员信息')
    return
  }

  try {
    await ElMessageBox.confirm('确认创建普通管理员账号？创建后该账号可进入管理后台。', '创建管理员确认', {
      confirmButtonText: '确认创建',
      cancelButtonText: '取消',
      type: 'warning',
    })
  } catch {
    return
  }

  loading.value = true
  try {
    const res = await createAdmin(form)
    ElMessage.success(`创建成功：${res.data?.userId || form.phone}`)
    Object.assign(form, { phone: '', password: '', nickname: '', verifyCode: '' })
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <section>
    <header class="page-header">
      <h1 class="page-title">管理员创建</h1>
      <p class="page-subtitle">超级管理员创建普通管理员账号</p>
    </header>

    <div class="panel form-panel">
      <el-alert
        v-if="authStore.role !== 'super_admin'"
        title="当前账号不是超级管理员，只能查看此页面，不能创建新管理员。"
        type="warning"
        :closable="false"
        class="permission-alert"
      />

      <el-form :model="form" label-position="top">
        <el-form-item label="手机号" required>
          <el-input v-model="form.phone" maxlength="11" placeholder="请输入管理员手机号" />
        </el-form-item>
        <el-form-item label="登录密码" required>
          <el-input v-model="form.password" show-password placeholder="8-16位，必须包含字母与数字" />
        </el-form-item>
        <el-form-item label="昵称" required>
          <el-input v-model="form.nickname" maxlength="15" placeholder="请输入管理员昵称" />
        </el-form-item>
        <el-form-item label="短信验证码" required>
          <div class="verify-row">
            <el-input v-model="form.verifyCode" maxlength="6" placeholder="请输入6位验证码" />
            <el-button :loading="codeLoading" @click="sendCode">发送验证码</el-button>
          </div>
        </el-form-item>
      </el-form>

      <el-button type="primary" :loading="loading" @click="submit">创建管理员</el-button>
    </div>
  </section>
</template>

<style scoped>
.page-header {
  margin-bottom: 24px;
}

.form-panel {
  max-width: 560px;
  padding: 24px;
}

.permission-alert {
  margin-bottom: 18px;
}

.verify-row {
  display: grid;
  width: 100%;
  grid-template-columns: 1fr auto;
  gap: 12px;
}
</style>
