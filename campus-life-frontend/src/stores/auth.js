import { defineStore } from 'pinia'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: '',
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
      this.user = {
        userId: data.userId,
        nickname: data.nickname,
        avatar: data.avatar,
        role: data.role,
      }
    },
    logout() {
      this.token = ''
      this.user = null
    },
  },
  persist: true,
})
