# skada-api-service

用户平面 API 服务 — 对外提供排行榜查询和数据录入接口。

## 核心接口

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/v1/score/submit` | 单条分数上报（需租户鉴权） |
| POST | `/api/v1/score/batch-submit` | 批量分数上报（同一排行榜） |
| GET | `/api/v1/leaderboard/ranking` | 查询排行榜排名 |
| GET | `/api/v1/leaderboard/history` | 查询历史榜单数据 |
| GET | `/api/v1/leaderboard/cycles` | 查询榜单周期列表 |

## 分数上报

### 单条上报
```json
POST /api/v1/score/submit
{
    "tenantId": "tn_xxxx",
    "secretKey": "sk_xxxx",
    "leaderboardId": 1,
    "userId": "player_001",
    "score": 1500,
    "payload": "{\"nickname\": \"Alice\"}"
}
```

### 批量上报
```json
POST /api/v1/score/batch-submit
{
    "tenantId": "tn_xxxx",
    "secretKey": "sk_xxxx",
    "leaderboardId": 1,
    "scores": [
        {"userId": "player_001", "score": 1500, "payload": "..."},
        {"userId": "player_002", "score": 2300}
    ]
}
```

## 排行榜查询
排行榜排名基于 Redis ZSET 实现，毫秒级响应，TTL 10 分钟。

## 技术说明
- 读接口带 Redis 缓存，布隆过滤器防缓存穿透
- 写接口先写 Redis ZSET，异步落库到 MySQL
- 租户鉴权通过 tenantId + secretKey 校验
- 分布式锁保障滚动操作的幂等性

## 启动
```bash
mvn spring-boot:run -pl api-service
# 默认端口 8080
```
