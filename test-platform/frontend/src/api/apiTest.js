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
