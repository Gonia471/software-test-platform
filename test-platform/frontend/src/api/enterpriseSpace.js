import request from './request'

export function getCurrentEnterpriseSpace() {
  return request.get('/enterprise-space/current')
}

export function getEnterpriseSpaceMembers() {
  return request.get('/enterprise-space/members')
}
