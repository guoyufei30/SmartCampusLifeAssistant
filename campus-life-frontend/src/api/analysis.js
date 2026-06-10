import request from './request'

export function getSleepAnalysis(config = {}) {
  return request.get('/analysis/sleep', config)
}

export function getExerciseAnalysis(config = {}) {
  return request.get('/analysis/exercise', config)
}

export function getPressureAnalysis(config = {}) {
  return request.get('/analysis/pressure', config)
}

export function getProcrastinationAnalysis(config = {}) {
  return request.get('/analysis/procrastination', config)
}
