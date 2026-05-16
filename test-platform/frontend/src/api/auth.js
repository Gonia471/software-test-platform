import request from './request'

export function registerWithOrg(phone, orgName, description) {
  return request.post('/auth/register-with-org', { phone, orgName, description })
}

export function loginWithCode(phone) {
  return request.post('/auth/login-with-code', { phone })
}

export function getMe() {
  return request.get('/auth/me')
}
