import request from './request'

export function listAssertions(collectionId) {
  return request.get(`/api-test/assertions/collection/${collectionId}`)
}

export function saveAssertions(collectionId, assertions) {
  return request.post(`/api-test/assertions/collection/${collectionId}/batch`, assertions)
}

export function createAssertion(assertion) {
  return request.post('/api-test/assertions', assertion)
}

export function updateAssertion(id, assertion) {
  return request.put(`/api-test/assertions/${id}`, assertion)
}

export function deleteAssertion(id) {
  return request.delete(`/api-test/assertions/${id}`)
}

export function listPrescripts(collectionId) {
  return request.get(`/api-test/prescripts/collection/${collectionId}`)
}

export function savePrescripts(collectionId, prescripts) {
  return request.post(`/api-test/prescripts/collection/${collectionId}/batch`, prescripts)
}

export function createPrescript(prescript) {
  return request.post('/api-test/prescripts', prescript)
}

export function updatePrescript(id, prescript) {
  return request.put(`/api-test/prescripts/${id}`, prescript)
}

export function deletePrescript(id) {
  return request.delete(`/api-test/prescripts/${id}`)
}

export function listScripts() {
  return request.get('/api-test/scripts')
}

export function getScript(id) {
  return request.get(`/api-test/scripts/${id}`)
}

export function getScriptByFunctionName(functionName) {
  return request.get(`/api-test/scripts/function/${functionName}`)
}

export function compileScript(content) {
  return request.post('/api-test/scripts/compile', { content })
}

export function testScript(content, params) {
  return request.post('/api-test/scripts/test', { content, params })
}

export function createScript(script) {
  return request.post('/api-test/scripts', script)
}

export function updateScript(id, script) {
  return request.put(`/api-test/scripts/${id}`, script)
}

export function deleteScript(id) {
  return request.delete(`/api-test/scripts/${id}`)
}

export function executeApiTest(collectionId) {
  return request.post(`/api-test/execute/${collectionId}`)
}