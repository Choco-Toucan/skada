import http from './http'
import type { ApiResponse, PageResult, Leaderboard, LeaderboardCreateRequest, LeaderboardUpdateRequest, LeaderboardInstance } from '@/types'

export function listLeaderboards(page: number = 1, pageSize: number = 20, tenantId?: string) {
  return http.get<ApiResponse<PageResult<Leaderboard>>>('/leaderboard/list', {
    params: { page, pageSize, ...(tenantId ? { tenantId } : {}) },
  })
}

export function getLeaderboard(id: number) {
  return http.get<ApiResponse<Leaderboard>>('/leaderboard/get', { params: { id } })
}

export function createLeaderboard(data: LeaderboardCreateRequest) {
  return http.post<ApiResponse<Leaderboard>>('/leaderboard/create', data)
}

export function updateLeaderboard(data: LeaderboardUpdateRequest) {
  return http.post<ApiResponse<Leaderboard>>('/leaderboard/update', data)
}

export function rollLeaderboard(leaderboardId: number) {
  return http.post<ApiResponse<Leaderboard>>('/leaderboard/roll', { leaderboardId })
}

export function stopLeaderboard(leaderboardId: number) {
  return http.post<ApiResponse<Leaderboard>>('/leaderboard/stop', { leaderboardId })
}

export function getInstances(leaderboardId: number) {
  return http.get<ApiResponse<LeaderboardInstance[]>>('/leaderboard/instances', {
    params: { leaderboardId },
  })
}
