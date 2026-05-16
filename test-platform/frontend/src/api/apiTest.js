import request from './request'

/** 获取接口集合列表 */
export function getCollections() {
  return request.get('/api-test/collections')
}

/** 创建/更新集合 */
export function saveCollection(data) {
  return data.id
    ? request.put(`/api-test/collections/${data.id}`, data)
    : request.post('/api-test/collections', data)
}

/** 删除集合 */
export function deleteCollection(id) {
  return request.delete(`/api-test/collections/${id}`)
}

/** 获取环境列表 */
export function getEnvironments() {
  return request.get('/api-test/environments')
}

/** 保存环境 */
export function saveEnvironment(data) {
  return data.id
    ? request.put(`/api-test/environments/${data.id}`, data)
    : request.post('/api-test/environments', data)
}

/** 删除环境 */
export function deleteEnvironment(id) {
  return request.delete(`/api-test/environments/${id}`)
}

/** 发送 HTTP 请求（通过后端代理，避免 CORS） */
export function sendRequest(config) {
  return request.post('/api-test/send', config)
}

/** 获取 API 测试执行记录列表 */
export function listApiExecutions(limit = 50) {
  return request.get('/api-test/executions', { params: { limit } })
}

/** 获取 API 测试执行详情 */
export function getApiExecutionDetail(id) {
  return request.get(`/api-test/executions/${id}`)
}

/** 保存 API 测试执行记录 */
export function saveApiExecution(data) {
  return request.post('/api-test/executions', data)
}

/** 获取 API 测试执行统计 */
export function getApiExecutionStatistics() {
  return request.get('/api-test/executions/statistics')
}
