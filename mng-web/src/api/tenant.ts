import http from './http'
import type { ApiResponse, Tenant, TenantCreateRequest, TenantUpdateRequest } from '@/types'

export function listTenants() {
  return http.get<ApiResponse<Tenant[]>>('/tenant/list')
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
