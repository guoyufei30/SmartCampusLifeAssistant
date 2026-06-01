import request from './request'

export function getWeeklyReport(config = {}) {
  return request.get('/report/weekly', config)
}

export function getWeeklyReportHistory(config = {}) {
  return request.get('/report/weekly/history', config)
}

export function regenerateWeeklyReport(data) {
  return request.post('/report/weekly/regenerate', data)
}

export function getWeeklyReportDetail(reportId, config = {}) {
  return request.get(`/report/weekly/${reportId}`, config)
}
