import request from './request'

export function getWeeklyReport(config = {}) {
  return request.get('/report/weekly', config)
}

export function getWeeklyReportHistory(config = {}) {
  return request.get('/report/weekly/history', config)
}
