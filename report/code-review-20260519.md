# Skada 项目第二轮代码审查报告

> **审查日期**: 2026-05-19  
> **审查范围**: api-service / mng-service / common / mng-web 全部源码  
> **审查方式**: 四路并行 agent 逐文件审查

---

## 总体统计

| 级别 | 数量 |
|------|------|
| CRITICAL | 2 |
| HIGH | 5 |
| MEDIUM | 6 |
| LOW | 11 |

---

## CRITICAL

### 1. mng-service `LeaderboardMapper.update()` 缺少 `status` 和 `current_instance_id` 列

**文件**: `mng-service/src/main/resources/mapper/LeaderboardMapper.xml:39-53`

UPDATE 语句没有包含这两个字段，但以下方法依赖它们：

| 调用方 | 设置的值 | 后果 |
|--------|---------|------|
| `LeaderboardConfigService.stop()` | `lb.setStatus("stopped")` | 终止操作**静默失败**，排行榜保持 active |
| `LeaderboardConfigService.roll()` | `lb.setCurrentInstanceId(...)` | 滚动后排行榜**仍指向旧实例** |
| `LeaderboardRollScheduler.stopLeaderboard()` | `lb.setStatus("stopped")` | 调度器自动终止失效 |
| `LeaderboardRollScheduler.rollLeaderboard()` | `lb.setCurrentInstanceId(...)` | 调度器自动滚动后引用丢失 |

**当前 UPDATE 列**: `name`, `start_time`, `end_time`, `max_query_users`, `allow_duplicate_report`, `allow_history_query`, `roll_strategy`, `roll_interval_value`, `roll_interval_unit`, `roll_user_count`, `update_by`

**缺失列**: `status`, `current_instance_id`

---

### 2. api-service `ScoreRecordMapper.findRanking()` 未按主指标过滤，多指标排行榜 DB 兜底查询排名错误

**文件**: `api-service/src/main/resources/mapper/ScoreRecordMapper.xml:32-41`

```sql
WHERE leaderboard_id = #{leaderboardId} AND instance_id = #{instanceId}
ORDER BY score
```

该查询返回**所有指标**的行，然后按 `score` 排序。对于多指标排行榜，用户在每个指标下各有一行记录，第二指标的行会混入主指标的排序结果中。

**场景**: 排行榜关联了"击杀数"（主指标，降序）和"等级"（次指标）。甲用户击杀 100、等级 50；乙用户击杀 80、等级 99。DB 查询返回 4 行（每人 2 行），ORDER BY score 降序时，等级=99 的行可能排在击杀=80 的行前面，破坏主指标排序。

**调用链**: `LeaderboardQueryService.getRanking()` → Redis 无数据 → `scoreRecordMapper.findRanking()` → `buildRankEntriesFromDb()`

---

## HIGH

### 3. 单条上报未写入 HyperLogLog，按用户数滚动永不触发

**文件**: `api-service/src/main/java/com/skada/api/service/ScoreService.java`

- `submit()`（第 97-98 行）: 调用 `writeMetrics()` + `checkUserCountRoll()`，但**没有** HLL 的 `add` 操作
- `batchSubmit()`（第 147 行）: 有 `redisTemplate.opsForHyperLogLog().add(userCountKey, item.getUserId())`

`checkUserCountRoll()`（第 270 行）依赖 `approxCount >= lb.getRollUserCount()` 作为第一层判断，单条路径下该计数永远不增长，滚动条件永不满足。

---

### 4. 上报接口未校验排行榜结束时间

**文件**: `api-service/src/main/java/com/skada/api/service/ScoreService.java:228-240`

`validateAndGetLeaderboard()` 检查了：
- `active` 状态 ✓
- `startTime` 是否已到 ✓
- 所属租户 ✓

但**没有检查** `endTime`。结束时间已过的排行榜仍然接受分数上报。

---

### 5. LogInterceptor 将 secretKey 和密码写入明文日志

**文件**: `common/src/main/java/com/skada/common/interceptor/LogInterceptor.java:58`

```java
API_LOG.info("{} {} -> {} ({}ms) params={} response={}", method, uri, status, cost, params, responseBody);
```

所有请求的 body 和 response 原样记录到 `api.log`，包括：
- `ScoreSubmitRequest.secretKey`（租户密钥）
- `BatchScoreSubmitRequest.secretKey`
- `LoginRequest.password`（管理员明文密码）
- 登录响应中的 token

这些敏感数据以明文形式持久化在磁盘上。

---

### 6. `lb.getMaxQueryUsers()` 和 `lb.getAllowDuplicateReport()` 可能 NPE

**文件**:
- `api-service/.../LeaderboardQueryService.java:113` — `lb.getMaxQueryUsers() - from`
- `api-service/.../ScoreService.java:89,134` — `lb.getAllowDuplicateReport() == 0`

这两个字段在模型中声明为 `Integer`（可空）。如果数据库中对应列存在 NULL 值（schema 有 DEFAULT 但历史数据可能缺失），自动拆箱会抛出 `NullPointerException`。

---

### 7. mng-service 手动滚动与调度器滚动之间无互斥

**文件**:
- `mng-service/.../LeaderboardConfigService.java:194-226` (`roll()`)
- `mng-service/.../LeaderboardRollScheduler.java:114-136` (`rollLeaderboard()`)

两个入口执行相同的步骤：
1. 读取活跃实例 → 2. 关闭它 → 3. 获取最大序列号 → 4. 创建新实例

没有任何锁保护。并发调用可能：
- 读取相同的 `MAX(instance_seq)` 创建重复序列号
- 创建冗余实例

---

## MEDIUM

### 8. `@RequirePermission` 类级别注解被静默忽略

**文件**: `common/src/main/java/com/skada/common/interceptor/PermissionInterceptor.java:30-33`

```java
RequirePermission annotation = hm.getMethodAnnotation(RequirePermission.class);
```

注解声明了 `@Target({ElementType.METHOD, ElementType.TYPE})`，但拦截器只检查方法级注解。如果在 Controller **类**上标注 `@RequirePermission("admin")`，该类所有方法都绕过权限检查。

---

### 9. `ValidationInterceptor` Content-Type 检查过于宽松

**文件**: `common/src/main/java/com/skada/common/interceptor/ValidationInterceptor.java:38`

```java
if (contentType == null || !contentType.toLowerCase().contains(MediaType.APPLICATION_JSON_VALUE))
```

`MediaType.APPLICATION_JSON_VALUE = "application/json"`，使用 `contains()` 匹配意味着 `application/json-patch+json`、`application/geo+json` 等都能通过校验。

---

### 10. 重复上报检查存在 TOCTOU 竞态条件

**文件**: `api-service/src/main/java/com/skada/api/service/ScoreService.java:89-95`

`@Transactional` 的默认 READ_COMMITTED 隔离级别下，两个并发请求可以同时：
1. 执行 `findByUserAndInstance()`（都返回 null）
2. 都通过 `if (existing != null)` 检查
3. 都执行 `insertBatch()`

数据库层面没有唯一约束阻止重复插入。

---

### 11. MetricService 和 TenantService 缺少 `@Transactional`

**文件**:
- `mng-service/.../MetricService.java:35,60,80` (create/update/delete)
- `mng-service/.../TenantService.java:32,54` (create/update)

同一项目中 `LeaderboardConfigService` 的所有写方法都有 `@Transactional`，而这两个 Service 没有，缺乏一致性。未来扩展时容易引入部分写入 bug。

---

### 12. 前端 HTTP 拦截器双重错误提示

**文件**: `mng-web/src/api/http.ts:34-52`

当后端返回 `code != 200`（非 401）时：
1. 成功拦截器（第 39 行）: `message.error(body.message)` + `Promise.reject`  
2. 错误拦截器（第 49-52 行）: 捕获到 reject 的 Error（无 `response` 属性），回退显示 `message.error('网络错误')`

用户会看到**两条**错误消息，第二条"网络错误"是误导性的。

---

### 13. 前端非空断言可能发送 undefined

**文件**: `mng-web/src/views/LeaderboardCreate.vue:139`

```ts
metrics: form.metrics.map((m, i) => ({
  metricId: m.metricId!,  // 非空断言
```

表单中 `metricId` 定义为 `number | undefined`（第 104 行）。如果选择器未设置值，`m.metricId!` 将 `undefined` 静默发送到 API。

---

## LOW

### 14. 分页参数未校验 page ≤ 0 或 pageSize ≤ 0

**文件**: `mng-service/.../LeaderboardConfigService.java:162`, `MetricService.java:105`, `TenantService.java:86`

```java
int offset = (page - 1) * pageSize;
```

传入 `page=0` 导致 offset 为负数，MySQL OFFSET 为负会报错。虽有 Controller 默认值保护，但客户端可显式传参。

---

### 15. 排行榜创建表单缺少关键配置项

**文件**: `mng-web/src/views/LeaderboardCreate.vue`

`LeaderboardCreateRequest` 类型定义了 `allowDuplicateReport`、`allowHistoryQuery`、`maxQueryUsers`，但创建表单上**没有这些字段的 UI 控件**，管理员无法配置。

---

### 16. 已登录用户仍可访问 /login 页面

**文件**: `mng-web/src/router/index.ts:49-54`

导航守卫只保护需要认证的路由，不阻止已登录用户访问 `/login`。

---

### 17. `LoginResponse` 解构未做防御性检查

**文件**: `mng-web/src/store/auth.ts:15`

```ts
const data = res.data.data  // 类型: LoginResponse
token.value = data.token     // data 为 null 时崩溃
```

如果后端返回 `code: 200, data: null`，此行抛出 `TypeError`。

---

### 18. Token 存储在 localStorage 而非 httpOnly cookie

**文件**: `mng-web/src/store/auth.ts:19` 和 `mng-web/src/api/http.ts:12`

存在 XSS 漏洞时，恶意脚本可通过 `window.localStorage` 窃取 token。

---

### 19. 前端存在未使用的导入和死代码

| 文件 | 未使用项 |
|------|---------|
| `views/TenantList.vue:50` | `getTenant` |
| `api/metric.ts:8-12` | `pageMetrics()` |
| `api/auth.ts:12-14` | `health()` |

---

### 20. `MetricAssociation.metricId` 命名混淆

**文件**: `mng-web/src/types/index.ts:103`

```ts
export interface MetricAssociation {
  metricId: number  // 实际存储的是 Metric.id（数字主键），而非 Metric.metricId（字符串外部标识）
```

字段名为 `metricId` 但类型是 `number`，容易与 `Metric.metricId: string` 混淆。

---

### 21. 排行榜创建表单缺少开始/结束时间互斥校验

**文件**: `mng-web/src/views/LeaderboardCreate.vue:14-19`

用户可以选一个早于开始时间的结束时间，前端未做校验。

---

### 22. 密码输入框无最小长度限制

**文件**: `mng-web/src/views/Login.vue:9`

密码字段仅校验 `required: true`，无最小长度规则。

---

### 23. 所有表格无加载失败/空状态 UI

**文件**: 所有视图（TenantList.vue, MetricList.vue, LeaderboardList.vue）

API 调用失败时，`loading` 重置为 `false`，表格静默变为空白，没有错误提示、重试按钮或空状态说明。

---

### 24. 排行榜操作按钮无加载状态

**文件**: `mng-web/src/views/LeaderboardList.vue:45-46`

"滚动"和"终止"按钮在 API 调用期间无 loading/disabled 状态，用户可重复点击。

---

## 修复优先级建议

| 优先级 | 编号 | 说明 |
|--------|------|------|
| P0 | #1 | mng-service update 缺字段，管理操作完全失效 |
| P0 | #2 | 多指标排行榜 DB 回退路径排名错误 |
| P1 | #3 | 按用户数滚动永不触发 |
| P1 | #4 | 排行榜结束后仍接受上报 |
| P1 | #5 | 密钥和密码泄露到日志 |
| P1 | #6 | NPE 风险 |
| P1 | #7 | 并发滚动无保护 |
| P2 | #8-#13 | 中等影响，建议合并处理 |
| P3 | #14-#24 | 可择机批量修复 |
