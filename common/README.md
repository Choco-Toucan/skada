# skada-common

Skada 公共库模块，被 api-service 和 mng-service 共同依赖。

## 过滤器

- **ContentCachingFilter** — 请求/响应体缓存。包装为 `ContentCachingRequestWrapper` / `ContentCachingResponseWrapper`，供日志拦截器读取。

## 拦截器

- **ValidationInterceptor** — 校验 POST/PUT 请求的 Content-Type 和请求体大小（上限 1MB）
- **LoginInterceptor** — 登录态校验。`@SkipLoginCheck` 跳过。Token 从 `Authorization: Bearer xxx` 获取，Redis 校验并自动续期
- **PermissionInterceptor** — 权限校验。`@RequirePermission("admin")` 要求对应角色
- **LogInterceptor** — 接口日志。记录请求体、响应体、状态码、耗时，输出到 `logs/api.log`

## 模型

- **BaseResponse\<T\>** — 统一响应 `{code, message, data, timestamp}`
- **PageResult\<T\>** — 分页结果 `{records, total, page, pageSize, totalPages}`
- **BusinessException** — 业务异常，携带错误码

## 注解

- `@SkipLoginCheck` — 跳过登录校验
- `@RequirePermission` — 权限要求

## 异常处理

- **GlobalExceptionHandler** — `@RestControllerAdvice`，统一捕获并返回 `BaseResponse`

## 工具

- **SnowflakeIdGenerator** — 分布式ID生成器
- **DistributedLock** — 基于 Redis SET NX EX + Lua 的分布式锁

## 使用

```xml
<dependency>
    <groupId>com.skada</groupId>
    <artifactId>skada-common</artifactId>
</dependency>
```

```java
@ComponentScan(basePackages = {"com.skada.common", "com.skada.api"})
```
