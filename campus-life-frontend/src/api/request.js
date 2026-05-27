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
    const message = error.response?.data?.message || error.message || '网络请求失败'

    if (error.config?.silentError) {
      return Promise.reject(error)
    }

    if (status === 401) {
      authStore.logout()
      ElMessage.error('登录已过期，请重新登录')
      window.location.href = '/login'
    } else {
      ElMessage.error(message)
    }

    return Promise.reject(error)
  },
)

export default request
