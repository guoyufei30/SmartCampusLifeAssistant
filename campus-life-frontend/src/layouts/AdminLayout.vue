<script setup>
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const authStore = useAuthStore()

const menus = [
  { path: '/admin/dashboard', label: '仪表盘', icon: 'DataBoard' },
  { path: '/admin/users', label: '用户管理', icon: 'User' },
  { path: '/admin/admins', label: '管理员创建', icon: 'UserFilled' },
  { path: '/admin/logs', label: '日志管理', icon: 'Tickets' },
  { path: '/admin/announcements', label: '公告管理', icon: 'Bell' },
  { path: '/admin/config', label: '系统配置', icon: 'Setting' },
]

function logout() {
  authStore.logout()
  router.push('/login')
}
</script>

<template>
  <div class="admin-layout">
    <aside class="admin-sidebar">
      <div class="brand">
        <div class="brand-icon">
          <el-icon><School /></el-icon>
        </div>
        <span>BIT Admin</span>
      </div>

      <el-menu
        router
        :default-active="$route.path"
        background-color="#003d7c"
        text-color="#c7d7ee"
        active-text-color="#ffffff"
      >
        <el-menu-item v-for="menu in menus" :key="menu.path" :index="menu.path">
          <el-icon><component :is="menu.icon" /></el-icon>
          <span>{{ menu.label }}</span>
        </el-menu-item>
      </el-menu>

      <div class="admin-profile">
        <div>
          <strong>{{ authStore.user?.nickname || '系统管理员' }}</strong>
          <p>{{ authStore.role || 'super_admin' }}</p>
        </div>
        <el-button plain size="small" @click="logout">退出系统</el-button>
      </div>
    </aside>

    <main class="admin-main">
      <router-view />
    </main>
  </div>
</template>

<style scoped>
.admin-layout {
  min-height: 100vh;
}

.admin-sidebar {
  position: fixed;
  inset: 0 auto 0 0;
  display: flex;
  width: 256px;
  flex-direction: column;
  background: var(--bit-blue);
  color: #fff;
}

.brand {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 24px;
  font-size: 18px;
  font-weight: 700;
}

.brand-icon {
  display: grid;
  width: 34px;
  height: 34px;
  place-items: center;
  border-radius: 8px;
  background: #fff;
  color: var(--bit-blue);
}

.el-menu {
  flex: 1;
  border-right: 0;
}

.admin-profile {
  display: grid;
  gap: 14px;
  padding: 20px 24px;
  border-top: 1px solid rgb(255 255 255 / 12%);
}

.admin-profile p {
  margin: 4px 0 0;
  color: #bfd1e8;
  font-size: 12px;
}

.admin-main {
  min-height: 100vh;
  margin-left: 256px;
  padding: 28px;
}
</style>
