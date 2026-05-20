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
| POST | `/api/v1/score/submit` | 单条分数上报（多指标，需签名鉴权） |
| POST | `/api/v1/score/batch-submit` | 批量分数上报（上限1000条） |
| GET | `/api/v1/leaderboard/ranking` | 查询排名（多指标值，支持匿名/鉴权查询） |
| GET | `/api/v1/leaderboard/instances` | 查询实例列表（含历史） |
| POST | `/api/v1/leaderboard/roll` | 手动触发排行榜滚动（需签名鉴权） |

## 鉴权方式

所有写操作和需要鉴权的读操作采用 **Header 签名鉴权**：

| Header | 说明 |
|--------|------|
| `X-Tenant-Id` | 租户ID，格式 `tn_xxxxxxxx` |
| `X-Timestamp` | 当前毫秒时间戳，偏差超过5分钟将被拒绝 |
| `X-Sign` | 签名：`SHA256(timestamp + secretKey + "skada")` |

未携带 `X-Tenant-Id` 的请求视为匿名访问，仅允许查询开启了匿名查询的排行榜。

## 分数上报

### 单条上报（多指标）
```bash
curl -X POST {API_BASE_URL}/api/v1/score/submit \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: tn_xxxx" \
  -H "X-Timestamp: $(date +%s%3N)" \
  -H "X-Sign: <SHA256(timestamp+secretKey+skada)>" \
  -d '{
    "userId": "player_001",
    "metrics": [
        { "metricId": "mt_xxxx", "value": 100 },
        { "metricId": "mt_yyyy", "value": 5000 }
    ],
    "payload": "{\"nickname\": \"Alice\"}"
  }'
```

### 批量上报
```bash
curl -X POST {API_BASE_URL}/api/v1/score/batch-submit \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: tn_xxxx" \
  -H "X-Timestamp: $(date +%s%3N)" \
  -H "X-Sign: <SHA256(timestamp+secretKey+skada)>" \
  -d '{
    "scores": [
        { "userId": "player_001", "metrics": [{"metricId": "mt_xxxx", "value": 100}] },
        { "userId": "player_002", "metrics": [{"metricId": "mt_xxxx", "value": 2300}] }
    ]
  }'
```

## 排名查询

返回多指标排名数据，使用 `from`/`to` 分页（0-based，含边界）：

```bash
curl "{API_BASE_URL}/api/v1/leaderboard/ranking?planId=lb_xxxx&from=0&to=9"
```

```json
{
    "code": 200,
    "data": [
        {
            "rank": 1,
            "userId": "player_001",
            "metricValues": [
                { "metricId": "mt_xxxx", "metricName": "击杀数", "value": 100.0 },
                { "metricId": "mt_yyyy", "metricName": "得分", "value": 5000.0 }
            ]
        }
    ]
}
```

## 手动触发滚动

租户可通过 OpenAPI 手动触发排行榜滚动，当前实例关闭后产生新实例：

```bash
curl -X POST {API_BASE_URL}/api/v1/leaderboard/roll \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: tn_xxxx" \
  -H "X-Timestamp: $(date +%s%3N)" \
  -H "X-Sign: <SHA256(timestamp+secretKey+skada)>" \
  -d '{
    "planId": "lb_xxxx",
    "instanceId": "li_xxxx"
  }'
```

响应返回新实例的 `instanceId` 和 `instanceSeq`。历史实例数据保留，可通过 `instanceId` 查询。

## 技术说明

- 每个指标独立 Redis ZSET 缓存（`skada:metric:{planId}:{instanceId}:{metricId}`），TTL 10 分钟
- 排序按指标优先级依次比较，主指标决定排序方向
- MySQL 作为兜底，缓存 miss 时回填 Redis
- 租户鉴权通过 Header 签名校验（`X-Tenant-Id` + `X-Timestamp` + `X-Sign`）
- 分布式锁保障滚动幂等性

## 启动

```bash
mvn spring-boot:run -pl api-service
# 端口 8801
```
