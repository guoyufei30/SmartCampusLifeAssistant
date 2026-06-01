import request from './request'

export function login(data, config = {}) {
  return request.post('/auth/login', data, config)
}

export function refreshToken(data, config = {}) {
  return request.post('/auth/refresh', data, config)
}

export function register(data) {
  return request.post('/auth/register', data)
}

export function sendVerifyCode(data) {
  return request.post('/auth/send_verify_code', data)
}

export function logout() {
  return request.post('/auth/logout')
}
