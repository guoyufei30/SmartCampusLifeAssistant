import request from './request'

export function getSemesters(config = {}) {
  return request.get('/semester/list', config)
}

export function setCurrentSemester(data) {
  return request.put('/semester/current', data)
}

export function downloadScheduleTemplate() {
  return request.get('/course/template', { responseType: 'blob' })
}

export function importSchedule(file) {
  const formData = new FormData()
  formData.append('file', file)

  return request.post('/course/import', formData)
}

export function getCourses(config = {}) {
  return request.get('/course/list', config)
}

export function createCourse(data) {
  return request.post('/course', data)
}

export function updateCourse(courseId, data) {
  return request.put(`/course/${courseId}`, data)
}

export function deleteCourse(courseId) {
  return request.delete(`/course/${courseId}`)
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
  return request.post('/schedule/events/check_conflict', data)
}

export function getVisibleAnnouncements(config = {}) {
  return request.get('/announcements', config)
}

export function dismissAnnouncement(id) {
  return request.post(`/announcements/${id}/dismiss`)
}
