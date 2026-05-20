# skada-mng-service

B端管理服务 — 租户管理、指标管理、排行榜计划配置、调度滚动。端口 **8811**。

## 核心功能

- **管理员认证** — Token 认证（Redis），2 小时过期自动续期
- **租户管理** — 创建、编辑、分页查询，自动生成 tenantId 和 secretKey
- **指标管理** — 创建、编辑、删除指标，按租户隔离
- **排行榜计划** — 创建时关联多个指标（优先级+排序方向），配置滚动策略
- **实例管理** — 查询排行榜计划的实例列表（含历史）
- **滚动控制** — 手动滚动、手动终止、周期性自动滚动、按用户数自动滚动；租户也可通过 OpenAPI 自行触发滚动

## 接口

| 方法 | 路径 | 说明 | 权限 |
|---|---|---|---|
| POST | `/api/v1/auth/login` | 管理员登录 | 无需登录 |
| POST | `/api/v1/auth/logout` | 管理员登出 | — |
| POST | `/api/v1/tenant/create` | 创建租户 | admin |
| POST | `/api/v1/tenant/update` | 编辑租户 | admin |
| GET | `/api/v1/tenant/list` | 租户列表（分页） | — |
| GET | `/api/v1/tenant/get` | 租户详情 | — |
| POST | `/api/v1/metric/create` | 创建指标 | admin |
| POST | `/api/v1/metric/update` | 编辑指标 | admin |
| POST | `/api/v1/metric/delete` | 删除指标 | admin |
| GET | `/api/v1/metric/list` | 按租户查询指标 | — |
| GET | `/api/v1/metric/page` | 指标分页 | — |
| POST | `/api/v1/leaderboard/create` | 创建排行榜计划（含指标关联） | admin |
| POST | `/api/v1/leaderboard/update` | 编辑排行榜计划（可更新指标关联） | admin |
| GET | `/api/v1/leaderboard/list` | 排行榜计划列表（分页，可按租户筛选） | — |
| GET | `/api/v1/leaderboard/get` | 排行榜计划详情 | — |
| GET | `/api/v1/leaderboard/instances` | 实例列表（含历史） | — |
| POST | `/api/v1/leaderboard/roll` | 手动触发滚动 | admin |
| POST | `/api/v1/leaderboard/stop` | 手动终止 | admin |

## 滚动策略

| 策略 | 说明 |
|---|---|
| `none` | 不滚动，一次性榜单 |
| `periodic` | 周期性滚动：每 N 分钟/小时/天 |
| `user_count` | 按用户数滚动：每 N 个不同用户上报 |
| 手动 | 管理后台手动触发，或租户通过 OpenAPI 调用 |

## 数据库

所有后端服务共用 `skada` 数据库，建表 SQL 位于 `src/main/resources/sql/schema.sql`。

### 表结构

| 表名 | 说明 |
|------|------|
| `admin_user` | 管理员用户 |
| `tenant` | 租户 |
| `metric` | 指标定义 |
| `leaderboard_plan` | 排行榜计划 |
| `leaderboard_metric` | 排行榜关联指标（优先级+排序） |
| `leaderboard_instance` | 排行榜实例 |
| `score_record` | 分数记录（多指标值） |

## 启动

```bash
# 默认 local 环境
mvn spring-boot:run -pl mng-service
# 端口 8811

# test 环境
mvn spring-boot:run -pl mng-service -Dspring-boot.run.arguments="--spring.profiles.active=test"
```

### 环境

| 环境 | profile | MySQL | Redis |
|------|---------|-------|-------|
| local | `local` (默认) | localhost:3306 | localhost:6379 |
| test | `test` | 阿里云 RDS | 待定 |
