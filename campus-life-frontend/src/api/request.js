import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '../stores/auth'

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 10000,
})

request.interceptors.request.use((config) => {
  const authStore = useAuthStore()

  if (authStore.token) {
    config.headers.Authorization = `Bearer ${authStore.token}`
  }

  return config
})

request.interceptors.response.use(
  (response) => {
    const body = response.data

    if (body instanceof Blob) {
      return body
    }

    if (body?.code && body.code !== 200) {
      if (!response.config.silentError) {
        ElMessage.error(body.message || '请求失败')
      }
      return Promise.reject(body)
    }

    return body
  },
  (error) => {
    const authStore = useAuthStore()
    const status = error.response?.status
    const body = error.response?.data || {}
    const message = body.message || error.message || '网络请求失败'
    const originalConfig = error.config || {}

    if (originalConfig.silentError) {
      return Promise.reject(error)
    }

    if (status === 401 && authStore.refreshToken && !originalConfig._retry && originalConfig.url !== '/auth/refresh') {
      originalConfig._retry = true

      return axios
        .post(`${import.meta.env.VITE_API_BASE_URL}/auth/refresh`, {
          refreshToken: authStore.refreshToken,
        })
        .then((refreshResponse) => {
          const refreshData = refreshResponse.data?.data || {}
          authStore.setToken(refreshData)
          originalConfig.headers = originalConfig.headers || {}
          originalConfig.headers.Authorization = `Bearer ${authStore.token}`
          return request(originalConfig)
        })
        .catch(() => {
          authStore.logout()
          ElMessage.error('登录已过期，请重新登录')
          window.location.href = '/login'
          return Promise.reject(error)
        })
    }

    if (status === 401) {
      authStore.logout()
      ElMessage.error('登录已过期，请重新登录')
      window.location.href = '/login'
    } else if (status === 403 && body.subCode === 'account_frozen') {
      authStore.logout()
      ElMessage.error(message || '账号已被冻结，请联系管理员')
      window.location.href = '/login'
    } else if (status === 403 && body.subCode === 'temp_password_required') {
      authStore.forceChangePassword = true
      ElMessage.warning(message || '请先修改临时密码')
    } else {
      ElMessage.error(message)
    }

    return Promise.reject(error)
  },
)

export default request
