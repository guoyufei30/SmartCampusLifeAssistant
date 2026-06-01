<script setup>
import { reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { login } from '../api/auth'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const authStore = useAuthStore()

const form = reactive({
  phone: '18800188000',
  password: 'Aa123456',
})

async function submitLogin() {
  const res = await login(form)
  const user = res.data

  if (!['admin', 'super_admin'].includes(user.role)) {
    ElMessage.error('当前账号不是管理员，无法进入管理后台')
    return
  }

  authStore.setLogin(user)
  ElMessage.success('登录成功')
  router.push('/admin/dashboard')
}
</script>

<template>
  <div class="login-page">
    <div class="login-panel panel">
      <h1>智慧校园生活助手</h1>
      <p>管理端登录</p>

      <el-form :model="form" label-position="top" class="login-form">
        <el-form-item label="手机号">
          <el-input v-model="form.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" show-password placeholder="请输入密码" />
        </el-form-item>
        <el-button type="primary" class="login-button" @click="submitLogin">登录</el-button>
      </el-form>
    </div>
  </div>
</template>

<style scoped>
.login-page {
  display: grid;
  min-height: 100vh;
  place-items: center;
  background: linear-gradient(135deg, #eef5ff 0%, #f8fafc 55%, #eefdf7 100%);
}

.login-panel {
  width: 420px;
  padding: 36px;
}

.login-panel h1 {
  margin: 0;
  color: var(--bit-blue);
  font-size: 28px;
}

.login-panel p {
  margin: 8px 0 28px;
  color: var(--text-muted);
}

.login-button {
  width: 100%;
}
</style>
