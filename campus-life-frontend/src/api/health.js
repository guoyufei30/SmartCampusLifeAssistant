import request from './request'

export function getSleepRecords(params, config = {}) {
  return request.get('/health/sleep', { ...config, params })
}

export function createSleepRecord(data) {
  return request.post('/health/sleep', data)
}

export function deleteSleepRecord(recordId) {
  return request.delete(`/health/sleep/${recordId}`)
}

export function getExerciseRecords(params, config = {}) {
  return request.get('/health/exercise', { ...config, params })
}

export function createExerciseRecord(data) {
  return request.post('/health/exercise', data)
}

export function getWeightRecords(params, config = {}) {
  return request.get('/health/weight', { ...config, params })
}

export function createWeightRecord(data) {
  return request.post('/health/weight', data)
}

export function getHealthChart(params, config = {}) {
  return request.get('/health/chart', { ...config, params })
}
