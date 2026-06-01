import { defineStore } from 'pinia'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: '',
    refreshToken: '',
    expiresIn: 0,
    forceChangePassword: false,
    user: null,
  }),
  getters: {
    isLoggedIn: (state) => Boolean(state.token),
    role: (state) => state.user?.role || '',
    isAdmin: (state) => ['admin', 'super_admin'].includes(state.user?.role),
  },
  actions: {
    setLogin(data) {
      this.token = data.token
      this.refreshToken = data.refreshToken || ''
      this.expiresIn = data.expiresIn || 0
      this.forceChangePassword = Boolean(data.forceChangePassword)
      this.user = {
        userId: data.userId,
        nickname: data.nickname,
        avatar: data.avatar,
        role: data.role,
      }
    },
    setToken(data) {
      this.token = data.token
      this.refreshToken = data.refreshToken || this.refreshToken
      this.expiresIn = data.expiresIn || this.expiresIn
    },
    logout() {
      this.token = ''
      this.refreshToken = ''
      this.expiresIn = 0
      this.forceChangePassword = false
      this.user = null
    },
  },
  persist: true,
})
