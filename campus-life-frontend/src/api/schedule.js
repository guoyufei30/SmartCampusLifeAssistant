import request from './request'

export function getSemesters(config = {}) {
  return request.get('/semester/list', config)
}

export function setCurrentSemester(data) {
  return request.put('/semester/current', data)
}

export function downloadScheduleTemplate() {
  return request.get('/schedule/template', { responseType: 'blob' })
}

export function importSchedule(file) {
  const formData = new FormData()
  formData.append('file', file)

  return request.post('/schedule/import', formData)
}

export function getEvents(params, config = {}) {
  return request.get('/schedule/events', { ...config, params })
}

export function createEvent(data) {
  return request.post('/schedule/events', data)
}

export function updateEvent(eventId, data) {
  return request.put(`/schedule/events/${eventId}`, data)
}

export function deleteEvent(eventId) {
  return request.delete(`/schedule/events/${eventId}`)
}

export function updateEventStatus(eventId, data) {
  return request.patch(`/schedule/events/${eventId}/status`, data)
}

export function checkEventConflict(data) {
  return request.post('/schedule/events/check-conflict', data)
}

export function getVisibleAnnouncements(config = {}) {
  return request.get('/announcements', config)
}
