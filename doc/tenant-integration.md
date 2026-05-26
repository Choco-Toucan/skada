# Skada 租户接入文档

> **版本**: v1.3  
> **最后更新**: 2026-05-20  
> **服务域名**:
> - `local` 环境: `http://localhost:8801`
> - `test` 环境: `https://toucan-api-test.invooooke.cn/skada`

---

## 目录

1. [产品概述](#1-产品概述)
2. [核心概念](#2-核心概念)
3. [快速接入](#3-快速接入)
4. [API 参考](#4-api-参考)
   - [4.1 分数上报（单条）](#41-分数上报单条)
   - [4.2 分数上报（批量）](#42-分数上报批量)
   - [4.3 排行榜排名查询](#43-排行榜排名查询)
   - [4.4 排行榜实例列表查询](#44-排行榜实例列表查询)
   - [4.5 手动触发排行榜滚动](#45-手动触发排行榜滚动)
5. [统一响应格式](#5-统一响应格式)
6. [排行榜滚动机制](#6-排行榜滚动机制)
7. [最佳实践](#7-最佳实践)
8. [常见问题](#8-常见问题)

---

## 1. 产品概述

Skada 是一个**排行榜即服务（Leaderboard as a Service）**平台，为游戏、社区、营销活动等场景提供开箱即用的排行榜能力。

**典型场景**：

- 游戏内击杀榜、竞速榜、战力榜
- 营销活动积分排行榜
- 社区贡献排行榜

**核心能力**：

- 多租户隔离，每个接入方可独立管理自己的排行榜
- 支持多指标排序（如先按`积分`排，积分相同再按`等级`排）
- 支持榜单自动滚动（周期滚动 / 达到N人滚动 / 不滚动）
- 支付自定义透传数据（payload），查询时原样下发
- 支持匿名查询或鉴权查询

---

## 2. 核心概念

```
┌──────────────┐     ┌─────────────────┐     ┌──────────────────────┐
│   Metric     │     │ Leaderboard     │     │ LeaderboardInstance  │
│   (指标)     │────▶│ Plan (排行榜计划) │────▶│ (排行榜实例)           │
│              │     │                 │     │                      │
│ mt_xxxxxxxx │     │ lb_xxxxxxxx     │     │ li_xxxxxxxx          │
└──────────────┘     └─────────────────┘     └──────────────────────┘
```

### 2.1 指标（Metric）

指标是租户上报的**数据维度**，例如"击杀数"、"得分"、"等级"。每个指标由 Skada 管理后台创建，生成唯一的 `metricId`（格式: `mt_xxxxxxxx`）。

### 2.2 排行榜计划（Leaderboard Plan）

排行榜计划定义了一个排行榜的**配置规则**：

- 关联哪些指标（可多个，按优先级排序）
- 每个指标是升序还是降序
- 开始/结束时间
- 滚动策略（不滚动 / 周期性 / 达到N人）
- 最大可查询人数
- 是否允许同一用户重复上报
- 是否允许查询历史榜单

创建后生成唯一的 `planId`（格式: `lb_xxxxxxxx`）。

### 2.3 排行榜实例（Leaderboard Instance）

排行榜计划的具体**执行载体**。每个计划在生命周期内会有一个或多个实例：

- 计划创建时自动产生**第一个实例**
- 每次滚动（手动/自动）产生**新实例**
- 实例有独立的 `instanceId`（格式: `li_xxxxxxxx`）

**查询时如果不指定实例，默认查询当前活跃实例。**

### 2.4 租户凭证

每个租户拥有两组凭据：

| 字段 | 格式 | 用途 |
|------|------|------|
| `tenantId` | `tn_xxxxxxxx` | 租户唯一标识 |
| `secretKey` | `sk_xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx` | 租户密钥，用于上报鉴权 |

> 凭据由 Skada 管理后台创建租户时自动生成，请联系运营方获取。

---

## 3. 快速接入

### 第一步：获取凭据

从 Skada 运营方获取你的 `tenantId` 和 `secretKey`。

### 第二步：创建指标和排行榜

联系运营方在管理后台为你创建**指标**和**排行榜计划**，你将获得：

- `metricId` — 上报数据时使用
- `planId` — 查询排名时使用

### 第三步：上报数据

```bash
# 计算签名：SHA256(timestamp + secretKey + "skada")
TIMESTAMP=$(date +%s%3N)
SIGN=$(echo -n "${TIMESTAMP}sk_550e8400-e29b-41d4-a716-446655440000skada" | shasum -a 256 | cut -d' ' -f1)

curl -X POST {API_BASE_URL}/api/v1/score/submit \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: tn_xxxxxxxx" \
  -H "X-Timestamp: ${TIMESTAMP}" \
  -H "X-Sign: ${SIGN}" \
  -d '{
    "userId": "player_001",
    "metrics": [
      { "metricId": "mt_killcount", "value": 1500 },
      { "metricId": "mt_level",     "value": 42, "payload": "{\"guild\":\"DragonSlayer\"}" }
    ]
  }'
```

### 第四步：查询排名

```bash
# 匿名查询（需租户开启"允许匿名查询"）
curl "{API_BASE_URL}/api/v1/leaderboard/ranking?planId=lb_xxxxxxxx&from=0&to=9"

# 鉴权查询（需携带签名 Header）
TIMESTAMP=$(date +%s%3N)
SIGN=$(echo -n "${TIMESTAMP}sk_550e8400-e29b-41d4-a716-446655440000skada" | shasum -a 256 | cut -d' ' -f1)

curl "{API_BASE_URL}/api/v1/leaderboard/ranking?planId=lb_xxxxxxxx&from=0&to=9" \
  -H "X-Tenant-Id: tn_xxxxxxxx" \
  -H "X-Timestamp: ${TIMESTAMP}" \
  -H "X-Sign: ${SIGN}"
```

---

## 4. API 参考

### 4.1 分数上报（单条）

```
POST /api/v1/score/submit
Content-Type: application/json
```

> **鉴权方式**：签名鉴权（Header 传递凭据）。详见下方说明。

**请求头**：

| Header | 必填 | 说明 |
|--------|------|------|
| `X-Tenant-Id` | 是 | 租户ID，格式 `tn_xxxxxxxx` |
| `X-Timestamp` | 是 | 当前毫秒时间戳，偏差超过5分钟将被拒绝 |
| `X-Sign` | 是 | 签名，算法：`SHA256(timestamp + secretKey + "skada")` |
| `Content-Type` | 是 | `application/json` |

> **签名计算示例**：
> ```
> sign = SHA256("1747334400000" + "sk_550e8400-e29b-41d4-a716-446655440000" + "skada")
> ```

**请求体**：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `userId` | string | 是 | 用户ID，由租户自行定义（如玩家ID），最长128字符 |
| `metrics` | array | 是 | 指标值数组，至少1项 |

`metrics` 数组元素：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `metricId` | string | 是 | 指标外部ID，格式 `mt_xxxxxxxx` |
| `value` | number | 是 | 指标值，支持小数（最多4位） |
| `payload` | string | 否 | 透传数据，JSON字符串，查询时原样下发 |

**示例**：

```bash
TIMESTAMP=$(date +%s%3N)
SIGN=$(echo -n "${TIMESTAMP}sk_550e8400-e29b-41d4-a716-446655440000skada" | shasum -a 256 | cut -d' ' -f1)

curl -X POST {API_BASE_URL}/api/v1/score/submit \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: tn_a1b2c3d4" \
  -H "X-Timestamp: ${TIMESTAMP}" \
  -H "X-Sign: ${SIGN}" \
  -d '{
    "userId": "player_001",
    "metrics": [
      {
        "metricId": "mt_killcount",
        "value": 1500,
        "payload": "{\"server\":\"asia-1\",\"clan\":\"DragonSlayer\"}"
      },
      {
        "metricId": "mt_level",
        "value": 42
      }
    ]
  }'
```

**成功响应**：

```json
{
  "code": 200,
  "message": "success",
  "data": null,
  "timestamp": 1747334400000
}
```

**业务规则**：

- 上报的指标集合必须精确匹配某个排行榜计划关联的指标集合，否则上报失败
- 如果排行榜计划设置了"不允许重复上报"，同一用户重复上报会返回错误
- 排行榜计划尚未开始或已终止时，上报会被拒绝

---

### 4.2 分数上报（批量）

```
POST /api/v1/score/batch-submit
Content-Type: application/json
```

> **鉴权方式**：签名鉴权（Header 传递凭据），与单条上报相同。

**请求头**：同 [4.1 分数上报（单条）](#41-分数上报单条)。

**请求体**：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `scores` | array | 是 | 批量数据，**最多1000条** |

`scores` 数组元素：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `userId` | string | 是 | 用户ID |
| `metrics` | array | 是 | 指标值数组，**批量中每条数据的指标集合必须一致** |

**示例**：

```bash
TIMESTAMP=$(date +%s%3N)
SIGN=$(echo -n "${TIMESTAMP}sk_550e8400-e29b-41d4-a716-446655440000skada" | shasum -a 256 | cut -d' ' -f1)

curl -X POST {API_BASE_URL}/api/v1/score/batch-submit \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: tn_a1b2c3d4" \
  -H "X-Timestamp: ${TIMESTAMP}" \
  -H "X-Sign: ${SIGN}" \
  -d '{
    "scores": [
      {
        "userId": "player_001",
        "metrics": [
          { "metricId": "mt_killcount", "value": 1500 },
          { "metricId": "mt_level", "value": 42 }
        ]
      },
      {
        "userId": "player_002",
        "metrics": [
          { "metricId": "mt_killcount", "value": 2300 },
          { "metricId": "mt_level", "value": 55 }
        ]
      }
    ]
  }'
```

---

### 4.3 排行榜排名查询

```
GET /api/v1/leaderboard/ranking
```

**查询参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `planId` | string | 是 | 排行榜计划外部ID，格式 `lb_xxxxxxxx` |
| `from` | int | 是 | 起始位置，0-based（含） |
| `to` | int | 是 | 结束位置，0-based（含），`from ≤ to` |
| `instanceId` | string | 否 | 实例外部ID，格式 `li_xxxxxxxx`。不传则查询当前活跃实例 |
> **分页规则**：`from` 和 `to` 均为 0-based 且包含边界。例如 `from=0&to=9` 返回前10名。  
> 实际返回条数受排行榜的 `maxQueryUsers` 配置约束。如果 `from` 超出可查询范围，返回空列表。

**鉴权说明**：

- **匿名查询**：租户开启"允许匿名查询"时，无需携带鉴权 Header 即可查询
- **鉴权查询**：租户不允许匿名查询时，需携带签名鉴权 Header（`X-Tenant-Id`、`X-Timestamp`、`X-Sign`），与上报接口鉴权方式相同

**示例请求**：

```bash
# 匿名查询（当前活跃实例前10名）
curl "{API_BASE_URL}/api/v1/leaderboard/ranking?planId=lb_xxxxxxxx&from=0&to=9"

# 查询指定历史实例
curl "{API_BASE_URL}/api/v1/leaderboard/ranking?planId=lb_xxxxxxxx&instanceId=li_xxxxxxxx&from=20&to=29"

# 鉴权查询
TIMESTAMP=$(date +%s%3N)
SIGN=$(echo -n "${TIMESTAMP}sk_550e8400-e29b-41d4-a716-446655440000skada" | shasum -a 256 | cut -d' ' -f1)

curl "{API_BASE_URL}/api/v1/leaderboard/ranking?planId=lb_xxxxxxxx&from=0&to=9" \
  -H "X-Tenant-Id: tn_xxxxxxxx" \
  -H "X-Timestamp: ${TIMESTAMP}" \
  -H "X-Sign: ${SIGN}"
```

**成功响应**：

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "rank": 1,
      "userId": "player_005",
      "metricValues": [
        {
          "metricId": "mt_killcount",
          "metricName": "击杀数",
          "value": 9800,
          "payload": "{\"server\":\"asia-1\"}"
        },
        {
          "metricId": "mt_level",
          "metricName": "等级",
          "value": 99,
          "payload": null
        }
      ]
    },
    {
      "rank": 2,
      "userId": "player_012",
      "metricValues": [
        {
          "metricId": "mt_killcount",
          "metricName": "击杀数",
          "value": 8700,
          "payload": null
        },
        {
          "metricId": "mt_level",
          "metricName": "等级",
          "value": 87,
          "payload": null
        }
      ]
    }
  ],
  "timestamp": 1747334400000
}
```

**响应字段**：

| 字段 | 类型 | 说明 |
|------|------|------|
| `rank` | int | 排名，从1开始 |
| `userId` | string | 用户ID |
| `metricValues` | array | 指标值列表，按排行榜配置的优先级排序 |
| `metricValues[].metricId` | string | 指标外部ID |
| `metricValues[].metricName` | string | 指标名称 |
| `metricValues[].value` | number | 指标值 |
| `metricValues[].payload` | string\|null | 上报时传入的透传数据 |

---

### 4.4 排行榜实例列表查询

```
GET /api/v1/leaderboard/instances
```

**查询参数**：

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `planId` | string | 是 | 排行榜计划外部ID |

**示例**：

```bash
curl "{API_BASE_URL}/api/v1/leaderboard/instances?planId=lb_xxxxxxxx"
```

**成功响应**：

```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 42,
      "instanceId": "li_a1b2c3d4",
      "leaderboardId": 10,
      "instanceSeq": 3,
      "startTime": 1747000000000,
      "endTime": 1747100000000,
      "status": "closed"
    },
    {
      "id": 45,
      "instanceId": "li_e5f6g7h8",
      "leaderboardId": 10,
      "instanceSeq": 4,
      "startTime": 1747100000000,
      "endTime": null,
      "status": "active"
    }
  ],
  "timestamp": 1747334400000
}
```

---

### 4.5 手动触发排行榜滚动

```
POST /api/v1/leaderboard/roll
Content-Type: application/json
```

> **鉴权方式**：签名鉴权（Header 传递凭据）。详见 [签名鉴权说明](#45-鉴权说明)。

**请求头**：

| Header | 必填 | 说明 |
|--------|------|------|
| `X-Tenant-Id` | 是 | 租户ID，格式 `tn_xxxxxxxx` |
| `X-Timestamp` | 是 | 当前毫秒时间戳，偏差超过5分钟将被拒绝 |
| `X-Sign` | 是 | 签名，算法：`SHA256(timestamp + secretKey + "skada")` |
| `Content-Type` | 是 | `application/json` |

**请求体**：

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `planId` | string | 是 | 排行榜计划外部ID，格式 `lb_xxxxxxxx` |
| `instanceId` | string | 是 | 当前活跃实例外部ID，格式 `li_xxxxxxxx`（用于并发冲突校验） |

**示例**：

```bash
TIMESTAMP=$(date +%s%3N)
SIGN=$(echo -n "${TIMESTAMP}sk_550e8400-e29b-41d4-a716-446655440000skada" | shasum -a 256 | cut -d' ' -f1)

curl -X POST {API_BASE_URL}/api/v1/leaderboard/roll \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: tn_a1b2c3d4" \
  -H "X-Timestamp: ${TIMESTAMP}" \
  -H "X-Sign: ${SIGN}" \
  -d '{
    "planId": "lb_xxxxxxxx",
    "instanceId": "li_xxxxxxxx"
  }'
```

**成功响应**：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "planId": "lb_xxxxxxxx",
    "instanceId": "li_newinst1",
    "instanceSeq": 5
  },
  "timestamp": 1747334400000
}
```

**响应字段**：

| 字段 | 类型 | 说明 |
|------|------|------|
| `data.planId` | string | 排行榜计划外部ID |
| `data.instanceId` | string | 新创建的实例外部ID |
| `data.instanceSeq` | int | 新实例的序号 |

**业务规则**：

- 只有状态为"进行中"的排行榜计划才能手动滚动
- `instanceId` 必须与当前活跃实例一致，不一致时返回错误（防止并发冲突）
- 服务端使用分布式锁保证同一排行榜同一时间只能触发一次滚动
- 旧的活跃实例将被关闭，其数据保留可查询

**常见错误码**：

| code | message | 说明 |
|------|---------|------|
| 10001 | `planId 不能为空` | 未传 planId |
| 10001 | `instanceId 不能为null` | 未传 instanceId |
| 20002 | `缺少 X-Sign 签名` | 鉴权 Header 不完整 |
| 20003 | `签名校验失败` | secretKey 或签名算法错误 |
| 20004 | `缺少 X-Timestamp 时间戳` | 未传 X-Timestamp |
| 20005 | `请求时间戳已过期` | 时间戳偏差超过5分钟 |
| 20006 | `租户不存在或已停用` | tenantId 无效 |
| 20007 | `缺少租户鉴权信息` | 写接口必须携带完整鉴权 Header |
| 30001 | `排行榜不存在或无权操作` | planId 错误或无权限 |
| 30002 | `排行榜已终止，无法滚动` | 排行榜不在进行中状态 |
| 30003 | `排行榜实例已变更，请重新查询后重试` | instanceId 与当前活跃实例不匹配，可能已有并发滚动 |
| 30004 | `当前无活跃实例` | 实例状态异常 |
| 30006 | `排行榜正在滚动中，请稍后重试` | 并发冲突，需重试 |

---

## 5. 统一响应格式

所有 API 返回统一的 JSON 结构，**HTTP 状态码始终为 200**，业务结果通过 `code` 字段区分：

```json
{
  "code": 200,
  "message": "success",
  "data": { ... },
  "timestamp": 1747334400000
}
```

### 5.1 错误码体系

采用三级 BizCode，所有错误码定义在 `BizCode` 常量类中：

| 范围 | 级别 | 说明 |
|------|------|------|
| 200 | 成功 | 请求成功 |
| 10000–19999 | 系统错误 | 参数缺失、格式错误、系统内部错误 |
| 20000–29999 | 接入层错误 | 签名校验失败、时间戳过期、租户不存在/停用 |
| 30000–39999 | 业务错误 | 排行榜不存在、已终止、实例不匹配等 |

### 5.2 全量错误码枚举（OpenAPI）

#### 系统错误 (10000–19999)

| code | message | 触发场景 | 接口 |
|------|---------|----------|------|
| 10000 | `(具体信息)` | 通用系统错误 | 全部 |
| 10001 | `userId 不能为空` | 单条/批量上报未传 userId | 全部 |
| 10001 | `metrics 不能为空` | 上报未传 metrics 数组 | 全部 |
| 10001 | `metrics 中每条数据必须包含 metricId` | 指标数据缺少 metricId | 全部 |
| 10001 | `metrics 中每条数据必须包含 value` | 指标数据缺少 value | 全部 |
| 10001 | `scores 不能为空` | 批量上报未传 scores | 批量上报 |
| 10001 | `单次批量上报不能超过1000条` | 批量超过1000条上限 | 批量上报 |
| 10001 | `scores 中每条数据必须包含 userId` | 批量数据缺少 userId | 批量上报 |
| 10001 | `scores 中每条数据必须包含 metrics` | 批量数据缺少 metrics | 批量上报 |
| 10001 | `planId 不能为空` | 手动滚动未传 planId | 手动滚动 |
| 10001 | `instanceId 不能为null` | 手动滚动未传 instanceId | 手动滚动 |
| 10002 | `请求体格式错误，请使用合法的JSON` | Body 不是合法 JSON | 全部 |
| 10003 | `服务器内部错误` | 未预期的服务端异常 | 全部 |

#### 接入层错误 (20000–29999)

| code | message | 触发场景 |
|------|---------|----------|
| 20000 | `(具体信息)` | 通用 SAAS 接入层错误 |
| 20001 | — | 缺少 X-Tenant-Id（当前未使用，匿名请求直接放行） |
| 20002 | `缺少 X-Sign 签名` | 携带 X-Tenant-Id 但未传 X-Sign |
| 20003 | `签名校验失败` | secretKey 不匹配或签名算法错误 |
| 20004 | `缺少 X-Timestamp 时间戳` | 携带 X-Tenant-Id 但未传 X-Timestamp |
| 20005 | `X-Timestamp 格式无效` | 时间戳非合法数字 |
| 20005 | `请求时间戳已过期` | 偏差超过 ±5 分钟 |
| 20006 | `租户不存在或已停用` | tenantId 无效或租户被停用 |
| 20007 | `缺少租户鉴权信息` | 写接口（上报/滚动）未携带完整 Header |
| 20007 | `租户凭证无效或无权查询` | 查排行榜时鉴权失败 |

#### 业务错误 (30000–39999)

| code | message | 触发场景 | 接口 |
|------|---------|----------|------|
| 30000 | `分页参数无效: from 必须 ≤ to 且都为非负数` | from/to 参数不合法 | 排名查询 |
| 30000 | `排行榜计划不存在` | planId 无效 | 排名查询/实例查询 |
| 30000 | `租户不存在或已停用` | 匿名查询时租户无效 | 排名查询 |
| 30000 | `实例不存在` | instanceId 无效 | 排名查询 |
| 30000 | `当前没有活跃实例` | 计划无活跃实例 | 排名查询 |
| 30000 | `排行榜计划未关联指标` | 计划配置异常 | 排名查询 |
| 30000 | `该用户已上报过分数，不允许重复上报` | 计划禁止重复上报 | 单条/批量上报 |
| 30000 | `批量上报中每条数据的指标集合必须一致` | 批量内指标集合不一致 | 批量上报 |
| 30000 | `指标不存在: {metricId}` | metricId 不存在 | 上报 |
| 30000 | `上报的指标集合未关联到任何活跃的排行榜计划` | 指标组合不匹配任何计划 | 上报 |
| 30000 | `上报的指标集合关联了多个排行榜计划` | 指标组合模糊匹配多个计划 | 上报 |
| 30000 | `排行榜计划不存在或已终止` | 计划无效 | 上报 |
| 30000 | `排行榜计划不属于该租户` | 租户无权操作该计划 | 上报 |
| 30000 | `排行榜计划尚未开始` | 还未到达 startTime | 上报 |
| 30000 | `排行榜已结束` | 已过 endTime | 上报 |
| 30000 | `当前没有活跃的排行榜实例` | 计划无活跃实例 | 上报 |
| 30000 | `指标 {metricId} 不属于该排行榜计划` | 指标未关联到计划 | 上报 |
| 30001 | `排行榜不存在或无权操作` | 租户无权操作该计划 | 手动滚动 |
| 30002 | `排行榜已终止，无法滚动` | 计划已 stopped | 手动滚动 |
| 30003 | `排行榜实例已变更，请重新查询后重试` | instanceId 与当前活跃实例不匹配 | 手动滚动 |
| 30004 | `当前无活跃实例` | 计划无活跃实例 | 手动滚动 |
| 30005 | `请提供当前活跃实例ID` | 未传 instanceId（service 层） | 手动滚动 |
| 30006 | `排行榜正在滚动中，请稍后重试` | 分布式锁冲突 | 手动滚动 |
| 30007 | — | 资源不存在（预留） | — |

### 5.3 错误响应示例

```json
{
  "code": 30000,
  "message": "上报的指标集合未关联到任何活跃的排行榜计划",
  "data": null,
  "timestamp": 1747334400000
}
```

```json
{
  "code": 20003,
  "message": "签名校验失败",
  "data": null,
  "timestamp": 1747334400000
}
```

```json
{
  "code": 10001,
  "message": "userId 不能为空",
  "data": null,
  "timestamp": 1747334400000
}
```

---

## 6. 排行榜滚动机制

排行榜计划支持三种滚动策略，创建时由运营方在管理后台配置：

### 6.1 不滚动（`none`）

整个排行榜生命周期只有一个实例，所有数据都在同一榜单中排名。

### 6.2 周期性滚动（`periodic`）

每隔 N 个时间单位自动创建一个新实例。例如：每 1 天滚动一次，产生"日榜"效果。

- 支持单位：`minute`（分钟）、`hour`（小时）、`day`（天）
- 旧实例数据保留，可通过 `instanceId` 查询历史榜单

### 6.3 用户数滚动（`user_count`）

当 N 个不同用户上报数据后，自动创建新实例。

- 使用 HyperLogLog 近似计数 + MySQL 精确计数双重验证
- 触发滚动的用户数据写入新实例

### 6.4 手动操作

管理员可在管理后台**手动滚动**或**手动终止**排行榜。租户也可通过 OpenAPI 的 [`POST /api/v1/leaderboard/roll`](#45-手动触发排行榜滚动) 接口自行触发滚动。

---

## 7. 最佳实践

### 7.1 上报时机

- **新用户首次活动时**：立即上报初始值，确保用户出现在榜单中
- **数值变化时**：如果允许重复上报，每次变化都应上报最新值
- **活动结束时**：确保所有用户最终数据已上报

### 7.2 userId 设计

- `userId` 最大长度 128 字符
- 建议使用你系统内的稳定用户标识，避免使用昵称（昵称可能变更）
- 不同排行榜计划中，同一 `userId` 被认为是不同榜单中的独立数据

### 7.3 payload 使用

- `payload` 用于携带自定义数据，如服务器ID、公会名称、头像URL等
- 内容为 JSON 字符串，服务端不做解析，查询时原样返回
- 不建议放入大量数据（字段类型为 `TEXT`，但建议控制在 1KB 以内）
- 每个指标的 payload 独立，可以为不同指标携带不同的自定义数据

### 7.4 批量上报

- 当需要一次性上报大量用户数据（如每日结算）时，使用批量接口
- 单次最多 1000 条
- 批量接口有更高吞吐量，减少网络往返

### 7.5 错误处理

- 始终检查响应的 `code` 字段，不要仅依赖 HTTP 状态码
- 鉴权失败（401）时检查凭据是否正确
- 参数错误（400）时检查请求格式
- 建议实现带退避的重试策略，处理偶发的服务端错误（500）

### 7.6 缓存建议

- 排行榜查询接口有服务端缓存（Redis，10分钟TTL），重复查询无需额外缓存
- 实例列表变化频率低，客户端可缓存更长时间

### 7.7 全球部署注意事项

- 所有时间字段均为**毫秒级 Unix 时间戳**
- 客户端在不同时区无需转换，传原始时间戳即可

---

## 8. 常见问题

### Q: 上报时报"指标集合未关联到任何活跃的排行榜计划"？

A: 你上报的 `metricId` 组合必须在某个活跃排行榜计划中精确匹配。请联系运营方确认：
1. 这些指标是否已创建
2. 对应的排行榜计划是否已创建且状态为"进行中"
3. 排行榜计划的开始时间是否已到

### Q: 上报时报"该用户已上报过分数"？

A: 该排行榜计划设置了"不允许重复上报"。如需更新用户分数，请联系运营方修改配置。

### Q: 如何查询历史榜单？

A: 先调用 `/api/v1/leaderboard/instances` 获取实例列表，找到目标实例的 `instanceId`，然后在排名查询中传入该 ID。

### Q: 查询排名返回的数据比请求的少？

A: 排行榜的 `maxQueryUsers` 限制了最大可查询范围。如果请求 `from=0&to=999` 而 `maxQueryUsers=100`，实际只返回前 100 条。

### Q: 不提供 tenantId 和 secretKey 能查排名吗？

A: 取决于租户的配置。如果租户开启了"允许匿名查询"，则不传凭据也可查询；否则必须传凭据。

### Q: payload 支持二进制数据吗？

A: 不支持。`payload` 为字符串字段，如需传输二进制数据，请 Base64 编码后放入 JSON 字符串。

### Q: 上报的 value 可以是负数吗？

A: 可以。`value` 支持正数、负数和小数（最多4位小数），如 `-10.5`。

---

> 如有其他问题，请联系 Skada 运营团队。
