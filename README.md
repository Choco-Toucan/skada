# Skada

**排行榜 SaaS 平台** — 多租户、高性能、全球部署。

Skada 为游戏和应用提供托管的排行榜服务。租户可以通过 API 上报用户分数，查询实时排名，并灵活配置排行榜的滚动策略。

## 模块结构

| 路径 | 模块 | 说明 |
|---|---|---|
| `common/` | skada-common | 公共库 — 拦截器、基础模型、分布式工具 |
| `api-service/` | skada-api-service | 用户平面 API — 分数上报、排行榜查询 |
| `mng-service/` | skada-mng-service | 管理平面后端 — 租户/排行榜配置 |
| `mng-web/` | 管理平面前端 | Vue3 + Ant Design Vue + TypeScript |

## 核心流程

```
租户注册 → 创建排行榜 → 玩家上报分数 → 排行榜排序 → 查询排名
  ↑                      ↑                    ↑
管理后台                API鉴权              缓存加速
```

## 技术栈

- **后端**: JDK 21, Spring Boot 4.0.6, Spring Cloud 2025.1.1
- **持久层**: MySQL + MyBatis
- **缓存**: Redis (排行榜 ZSET + 分布式锁)
- **序列化**: Gson
- **日志**: Log4j2
- **前端**: Vue3 + Ant Design Vue + TypeScript

## 构建

```bash
# 全量编译
mvn clean package

# 编译单个模块
mvn clean package -pl api-service -am
```

## 快速开始

1. 创建 MySQL 数据库 `skada`，执行 `mng-service/src/main/resources/sql/schema.sql`
2. 启动 Redis
3. 配置 `application.yml` 中的数据库和 Redis 连接
4. 启动服务：

```bash
mvn spring-boot:run -pl api-service   # 用户平面 API，默认端口 8080
mvn spring-boot:run -pl mng-service   # 管理平面 API，默认端口 8081
```
