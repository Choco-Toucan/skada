# skada-api-service

C端 API 服务 — 对外提供排行榜查询和分数上报接口。端口 **8801**。

## 核心概念

- **指标 (Metric)**：租户定义的上报维度
- **排行榜计划 (Leaderboard Plan)**：关联多个指标，配置滚动策略
- **排行榜实例 (Leaderboard Instance)**：计划的执行实例，每次滚动产生新实例
- **多指标上报**：一次上报可提交多个指标的值

## 接口

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/v1/score/submit` | 单条分数上报（多指标，需租户鉴权） |
| POST | `/api/v1/score/batch-submit` | 批量分数上报（上限1000条） |
| GET | `/api/v1/leaderboard/ranking` | 查询排名（多指标值，支持匿名/加密查询） |
| GET | `/api/v1/leaderboard/instances` | 查询实例列表（含历史） |

## 分数上报

### 单条上报（多指标）
```json
POST /api/v1/score/submit
{
    "tenantId": "tn_xxxx",
    "secretKey": "sk_xxxx",
    "leaderboardId": 1,
    "userId": "player_001",
    "metrics": [
        { "metricId": 1, "value": 100 },
        { "metricId": 2, "value": 5000 }
    ],
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
        { "userId": "player_001", "metrics": [{"metricId": 1, "value": 100}] },
        { "userId": "player_002", "metrics": [{"metricId": 1, "value": 2300}] }
    ]
}
```

## 排名查询

返回多指标排名数据：

```json
GET /api/v1/leaderboard/ranking?leaderboardId=1&limit=10
{
    "code": 200,
    "data": [
        {
            "rank": 1,
            "userId": "player_001",
            "metricValues": [
                { "metricId": 1, "metricName": "击杀数", "value": 100.0 },
                { "metricId": 2, "metricName": "得分", "value": 5000.0 }
            ]
        }
    ]
}
```

## 技术说明

- 每个指标独立 Redis ZSET 缓存（`skada:metric:{planId}:{instanceId}:{metricId}`），TTL 10 分钟
- 排序按指标优先级依次比较，主指标决定排序方向
- MySQL 作为兜底，缓存 miss 时回填 Redis
- 租户鉴权通过 tenantId + secretKey 校验
- HyperLogLog 统计去重用户数，分布式锁保障滚动幂等性

## 启动

```bash
mvn spring-boot:run -pl api-service
# 端口 8801
```
