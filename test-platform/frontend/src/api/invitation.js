import request from './request'

export function getInvitations() {
  return request.get('/invitations')
}

export function createInvitation(data) {
  return request.post('/invitations', data)
}

export function acceptInvitation(invitationId) {
  return request.post(`/invitations/${invitationId}/accept`)
}

export function checkInvitationByPhone(phone) {
  return request.get('/invitations/check', { params: { phone } })
}
