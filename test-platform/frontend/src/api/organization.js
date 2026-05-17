import request from './request'

export function getUserOrganizations() {
  return request.get('/organizations')
}

export function createOrganization(data) {
  return request.post('/organizations', data)
}

export function getOrganization(id) {
  return request.get(`/organizations/${id}`)
}

export function updateOrganization(id, data) {
  return request.put(`/organizations/${id}`, data)
}

export function deleteOrganization(id) {
  return request.delete(`/organizations/${id}`)
}

export function getOrganizationProjects(orgId) {
  return request.get(`/organizations/${orgId}/projects`)
}

export function getOrgMembers(orgId) {
  return request.get(`/organizations/${orgId}/members`)
}

export function updateMemberRole(orgId, memberId, data) {
  return request.put(`/organizations/${orgId}/members/${memberId}`, data)
}

export function removeMember(orgId, memberId) {
  return request.delete(`/organizations/${orgId}/members/${memberId}`)
}

export function getAvailableMembers(orgId) {
  return request.get(`/organizations/${orgId}/available-members`)
}

export function addMember(orgId, data) {
  return request.post(`/organizations/${orgId}/members`, data)
}
