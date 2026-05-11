import http from './http'
import type { ApiResponse, Leaderboard, LeaderboardCreateRequest } from '@/types'

export function listLeaderboards(tenantId?: string) {
  return http.get<ApiResponse<Leaderboard[]>>('/leaderboard/list', {
    params: tenantId ? { tenantId } : {},
  })
}

export function getLeaderboard(id: number) {
  return http.get<ApiResponse<Leaderboard>>('/leaderboard/get', { params: { id } })
}

export function createLeaderboard(data: LeaderboardCreateRequest) {
  return http.post<ApiResponse<Leaderboard>>('/leaderboard/create', data)
}

export function updateLeaderboard(data: any) {
  return http.post<ApiResponse<Leaderboard>>('/leaderboard/update', data)
}

export function rollLeaderboard(leaderboardId: number) {
  return http.post<ApiResponse<Leaderboard>>('/leaderboard/roll', { leaderboardId })
}

export function stopLeaderboard(leaderboardId: number) {
  return http.post<ApiResponse<Leaderboard>>('/leaderboard/stop', { leaderboardId })
}
