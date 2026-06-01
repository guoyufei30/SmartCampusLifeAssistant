import request from './request'

export function getDashboard(config = {}) {
  return request.get('/admin/dashboard', config)
}

export function getUsers(params, config = {}) {
  return request.get('/admin/users', { ...config, params })
}

export function freezeUser(userId, data) {
  return request.post(`/admin/users/${userId}/freeze`, data)
}

export function unfreezeUser(userId) {
  return request.post(`/admin/users/${userId}/unfreeze`)
}

export function resetUserPassword(userId, data) {
  return request.post(`/admin/users/${userId}/reset_password`, data)
}

export function createAdmin(data) {
  return request.post('/admin/admins', data)
}

export function getAnnouncements(params, config = {}) {
  return request.get('/admin/announcements', { ...config, params })
}

export function createAnnouncement(data) {
  return request.post('/admin/announcements', data)
}

export function offlineAnnouncement(id) {
  return request.put(`/admin/announcements/${id}/offline`)
}

export function getConfig(config = {}) {
  return request.get('/admin/config', config)
}

export function updateConfig(data) {
  return request.put('/admin/config', data)
}

export function getOperationLogs(params, config = {}) {
  return request.get('/admin/logs/operation', { ...config, params })
}

export function getExceptionLogs(params, config = {}) {
  return request.get('/admin/logs/exception', { ...config, params })
}

export function cleanLogs(params) {
  return request.delete('/admin/logs/clean', { params })
}
