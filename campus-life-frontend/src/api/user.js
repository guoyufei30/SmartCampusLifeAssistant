import request from './request'

export function getProfile(config = {}) {
  return request.get('/user/profile', config)
}

export function updateProfile(data) {
  return request.put('/user/profile', data)
}

export function updatePassword(data) {
  return request.put('/user/password', data)
}

export function uploadAvatar(file) {
  const formData = new FormData()
  formData.append('avatar', file)

  return request.post('/user/avatar', formData)
}

export function bindPhone(data) {
  return request.put('/user/phone/bind', data)
}

export function deleteAccount(data) {
  return request.delete('/user/account', { data })
}
