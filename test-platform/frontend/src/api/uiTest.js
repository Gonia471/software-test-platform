import request from './request'

/** 获取用例列表 */
export function getCases() {
  return request.get('/ui-test/test-cases')
}

/** 获取指定组织下的用例列表 */
export function getCasesByOrganization(orgId) {
  return request.get(`/ui-test/test-cases/organization/${orgId}`)
}

/** 创建用例 */
export function createCase(data) {
  return request.post('/ui-test/test-cases', data)
}

/** 更新用例 */
export function updateCase(id, data) {
  return request.put(`/ui-test/test-cases/${id}`, data)
}

/** 获取用例详情 */
export function getCase(id) {
  return request.get(`/ui-test/test-cases/${id}`)
}

/** 删除用例 */
export function deleteCase(id) {
  return request.delete(`/ui-test/test-cases/${id}`)
}

/** 获取执行实例列表 */
export function getInstances() {
  return request.get('/ui-test/instances')
}

/** XPath 快速预览 */
export function previewXpathFast(xpath) {
  return request.get('/ui-test/xpath/preview', {
    params: { xpath },
  })
}

/** XPath 上下文预览 */
export async function previewXpathWithContext(data) {
  try {
    return await request.post('/ui-test/xpath/preview', data)
  } catch (error) {
    if (data?.xpath) {
      return previewXpathFast(data.xpath)
    }
    throw error
  }
}

/** 启动执行 */
export function startExecution(data) {
  return request.post('/ui-test/executions', data)
}

/** UI 自动化执行记录列表（测试报告） */
export function listExecutions() {
  return request.get('/ui-test/executions')
}

/** 获取执行详情 */
export function getExecutionDetail(id) {
  return request.get(`/ui-test/executions/${id}`)
}

/** 停止执行 */
export function stopExecution(id) {
  return request.post(`/ui-test/executions/${id}/stop`)
}
