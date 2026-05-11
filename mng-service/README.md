# skada-mng-service

管理平面后端服务 — 租户管理、排行榜配置、数据查询。

## 核心功能

- **管理员登录** — Token 认证，2 小时过期自动续期
- **租户管理** — 创建、编辑、查询租户（分页），自动生成 tenantId 和 secretKey
- **排行榜配置** — 创建排行榜，配置滚动策略、排序规则等
- **滚动控制** — 手动触发滚动、手动终止排行榜
- **自动调度** — 周期性滚动自动触发、排行榜到期自动终止
- **数据查询** — 查询排行榜的周期列表、历史数据

## 接口概览

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/v1/auth/login` | 管理员登录（@SkipLoginCheck） |
| POST | `/api/v1/auth/logout` | 管理员登出 |
| POST | `/api/v1/tenant/create` | 创建租户（需admin权限） |
| POST | `/api/v1/tenant/update` | 编辑租户（需admin权限） |
| GET | `/api/v1/tenant/list` | 租户列表（分页） |
| GET | `/api/v1/tenant/get` | 租户详情 |
| POST | `/api/v1/leaderboard/create` | 创建排行榜（需admin权限） |
| POST | `/api/v1/leaderboard/update` | 编辑排行榜（需admin权限） |
| GET | `/api/v1/leaderboard/list` | 排行榜列表（分页，可按租户筛选） |
| GET | `/api/v1/leaderboard/get` | 排行榜详情 |
| GET | `/api/v1/leaderboard/cycles` | 周期列表（含历史周期） |
| POST | `/api/v1/leaderboard/roll` | 手动滚动（需admin权限） |
| POST | `/api/v1/leaderboard/stop` | 手动终止（需admin权限） |

## 滚动策略

| 策略 | 说明 |
|---|---|
| 不滚动 | 一次性榜单 |
| 周期性滚动 | 每 N 分钟/小时/天滚动一次 |
| 按用户数滚动 | 每 N 个不同用户上报后滚动 |
| 手动滚动 | 管理后台手动触发 |

## 数据库

建表 SQL 位于 `src/main/resources/sql/schema.sql`，包含全部表的 DDL。

所有后端服务共用 `skada` 数据库。

## 启动
```bash
mvn spring-boot:run -pl mng-service
# 默认端口 8081
```
