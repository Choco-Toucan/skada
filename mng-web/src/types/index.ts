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

/** 指标 */
export interface Metric {
  id: number
  metricId: string
  tenantId: string
  name: string
  description: string | null
  createTime: string
  updateTime: string
}

/** 创建指标请求 */
export interface MetricCreateRequest {
  tenantId: string
  name: string
  description?: string
}

/** 更新指标请求 */
export interface MetricUpdateRequest {
  id: number
  name?: string
  description?: string
}

/** 排行榜计划 */
export interface Leaderboard {
  id: number
  tenantId: string
  name: string
  startTime: number
  endTime: number | null
  maxQueryUsers: number
  allowDuplicateReport: number
  allowHistoryQuery: number
  rollStrategy: string
  rollIntervalValue: number | null
  rollIntervalUnit: string | null
  rollUserCount: number | null
  status: string
  currentInstanceId: number | null
  createTime: string
}

/** 指标关联 */
export interface MetricAssociation {
  /** 指标的内部数字主键（Metric.id），注意与 Metric.metricId（字符串外部标识）区分 */
  metricId: number
  priority: number
  sortOrder: string
}

/** 创建排行榜计划请求 */
export interface LeaderboardCreateRequest {
  tenantId: string
  name: string
  startTime: number
  endTime?: number
  maxQueryUsers?: number
  allowDuplicateReport?: number
  allowHistoryQuery?: number
  rollStrategy?: string
  rollIntervalValue?: number
  rollIntervalUnit?: string
  rollUserCount?: number
  metrics: MetricAssociation[]
}

/** 更新排行榜计划请求 */
export interface LeaderboardUpdateRequest {
  id: number
  name?: string
  startTime?: number
  endTime?: number
  maxQueryUsers?: number
  allowDuplicateReport?: number
  allowHistoryQuery?: number
  rollStrategy?: string
  rollIntervalValue?: number
  rollIntervalUnit?: string
  rollUserCount?: number
  metrics?: MetricAssociation[]
}

/** 排行榜实例 */
export interface LeaderboardInstance {
  id: number
  leaderboardId: number
  instanceSeq: number
  startTime: number
  endTime: number | null
  status: string
}
