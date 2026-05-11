import http from './http'
import type { ApiResponse, LoginRequest, LoginResponse } from '@/types'

export function login(data: LoginRequest) {
  return http.post<ApiResponse<LoginResponse>>('/auth/login', data)
}

export function logout() {
  return http.post<ApiResponse<void>>('/auth/logout')
}

export function health() {
  return http.get<ApiResponse<string>>('/auth/health')
}
