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

export function getAnnouncements(config = {}) {
  return request.get('/admin/announcements', config)
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
