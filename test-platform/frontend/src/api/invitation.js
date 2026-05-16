import request from './request'

export function getOrgInvitations(orgId) {
  return request.get(`/invitations/orgs/${orgId}`)
}

export function createInvitation(orgId, data) {
  return request.post(`/invitations/orgs/${orgId}`, data)
}

export function useInvitation(code) {
  return request.post(`/invitations/use/${code}`)
}