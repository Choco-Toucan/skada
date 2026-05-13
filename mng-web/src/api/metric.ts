import http from './http'
import type { ApiResponse, PageResult, Metric, MetricCreateRequest, MetricUpdateRequest } from '@/types'

export function listMetrics(tenantId: string) {
  return http.get<ApiResponse<Metric[]>>('/metric/list', { params: { tenantId } })
}

export function pageMetrics(page: number = 1, pageSize: number = 20) {
  return http.get<ApiResponse<PageResult<Metric>>>('/metric/page', {
    params: { page, pageSize },
  })
}

export function getMetric(id: number) {
  return http.get<ApiResponse<Metric>>('/metric/detail', { params: { id } })
}

export function createMetric(data: MetricCreateRequest) {
  return http.post<ApiResponse<Metric>>('/metric/create', data)
}

export function updateMetric(data: MetricUpdateRequest) {
  return http.post<ApiResponse<Metric>>('/metric/update', data)
}

export function deleteMetric(id: number) {
  return http.post<ApiResponse<null>>('/metric/delete', { id })
}
