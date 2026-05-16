import request from './request'

export function getCurrentUser() {
  return request.get('/auth/me')
}

export function updateCurrentUser(data) {
  return request.put('/users/me', data)
}

export function getUserOrganizations() {
  return request.get('/organizations')
}
