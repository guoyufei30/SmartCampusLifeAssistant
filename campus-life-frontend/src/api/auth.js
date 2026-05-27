import request from './request'

export function login(data, config = {}) {
  return request.post('/auth/login', data, config)
}

export function adminLogin(data, config = {}) {
  return request.post('/admin/login', data, config)
}

export function register(data) {
  return request.post('/auth/register', data)
}

export function sendVerifyCode(data) {
  return request.post('/auth/sendVerifyCode', data)
}
