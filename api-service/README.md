# skada-api-service

用户平面 API 服务 — 对外提供排行榜查询和数据录入接口。

## 核心接口

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/v1/score/submit` | 单条分数上报（需租户鉴权，校验开始时间） |
| POST | `/api/v1/score/batch-submit` | 批量分数上报（同一排行榜，上限1000条） |
| GET | `/api/v1/leaderboard/ranking` | 查询排行榜排名（支持匿名/加密查询） |
| GET | `/api/v1/leaderboard/cycles` | 查询榜单周期列表（含历史） |

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
- 排名查询基于 Redis ZSET，MySQL 作为兜底，缓存 TTL 10 分钟
- 写接口同步写 Redis ZSET 并持久化到 MySQL
- 租户鉴权通过 tenantId + secretKey 校验
- 查询鉴权支持匿名查询和加密查询，由租户配置控制
- HyperLogLog 统计去重用户数，触发按用户数滚动
- 分布式锁保障滚动操作的幂等性

## 启动
```bash
mvn spring-boot:run -pl api-service
# 默认端口 8080
```
