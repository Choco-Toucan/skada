/** 统一API响应 */
export interface ApiResponse<T> {
  code: number
  message: string
  data: T
  timestamp: number
}

/** 分页结果 */
export interface PageResult<T> {
  records: T[]
  total: number
  page: number
  pageSize: number
  totalPages: number
}

/** 登录请求 */
export interface LoginRequest {
  username: string
  password: string
}

/** 登录响应 */
export interface LoginResponse {
  token: string
  displayId: string
  role: string
}

/** 租户 */
export interface Tenant {
  id: number
  tenantId: string
  name: string
  secretKey: string
  allowAnonymousQuery: number
  status: number
  createTime: string
  updateTime: string
}

/** 创建租户请求 */
export interface TenantCreateRequest {
  name: string
  allowAnonymousQuery?: number
}

/** 更新租户请求 */
export interface TenantUpdateRequest {
  id: number
  name?: string
  allowAnonymousQuery?: number
  status?: number
}

/** 排行榜 */
export interface Leaderboard {
  id: number
  tenantId: string
  name: string
  startTime: number
  endTime: number | null
  sortOrder: string
  maxQueryUsers: number
  allowDuplicateReport: number
  allowHistoryQuery: number
  rollStrategy: string
  rollIntervalValue: number | null
  rollIntervalUnit: string | null
  rollUserCount: number | null
  status: string
  currentCycleId: number | null
  createTime: string
}

/** 创建排行榜请求 */
export interface LeaderboardCreateRequest {
  tenantId: string
  name: string
  startTime: number
  endTime?: number
  sortOrder?: string
  maxQueryUsers?: number
  allowDuplicateReport?: number
  allowHistoryQuery?: number
  rollStrategy?: string
  rollIntervalValue?: number
  rollIntervalUnit?: string
  rollUserCount?: number
}
