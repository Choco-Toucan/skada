import http from './http'
import type { ApiResponse, PageResult, Tenant, TenantCreateRequest, TenantUpdateRequest } from '@/types'

export function listTenants(page: number = 1, pageSize: number = 20) {
  return http.get<ApiResponse<PageResult<Tenant>>>('/tenant/list', {
    params: { page, pageSize },
  })
}

export function listAllTenants() {
  return http.get<ApiResponse<PageResult<Tenant>>>('/tenant/list', {
    params: { page: 1, pageSize: 1000 },
  })
}

export function getTenant(id: number) {
  return http.get<ApiResponse<Tenant>>('/tenant/get', { params: { id } })
}

export function createTenant(data: TenantCreateRequest) {
  return http.post<ApiResponse<Tenant>>('/tenant/create', data)
}

export function updateTenant(data: TenantUpdateRequest) {
  return http.post<ApiResponse<Tenant>>('/tenant/update', data)
}
