# skada-common

Skada 公共库模块。

## 功能

### 拦截器
所有拦截器在 `com.skada.common.interceptor` 包中：

- **LoginInterceptor** — 登录态校验。标注 `@SkipLoginCheck` 的方法跳过校验。Token 从 `Authorization` 请求头获取，自动续期 2 小时。
- **PermissionInterceptor** — 权限校验。标注 `@RequirePermission` 的方法需要对应权限。
- **LogInterceptor** — 接口日志。记录请求方法、URI、状态码、耗时，输出到 `logs/api.log`。

### 模型
- **BaseResponse** — 统一 API 响应格式 `{code, message, data, timestamp}`。
- **BusinessException** — 业务异常，可携带错误码。

### 注解
- `@SkipLoginCheck` — 跳过登录校验
- `@RequirePermission` — 权限要求

### 工具
- **SnowflakeIdGenerator** — Snowflake 分布式 ID 生成器，支持最多 1024 个节点，每毫秒 4096 个 ID。
- **DistributedLock** — 基于 Redis SET NX EX + Lua 脚本的分布式锁。

### 异常处理
- **GlobalExceptionHandler** — `@RestControllerAdvice`，统一异常捕获并返回 `BaseResponse`。

## 使用

其他模块在 `pom.xml` 中添加依赖：

```xml
<dependency>
    <groupId>com.skada</groupId>
    <artifactId>skada-common</artifactId>
</dependency>
```

在 `@SpringBootApplication` 上配置包扫描：

```java
@ComponentScan(basePackages = {"com.skada.common", "com.skada.api"})
```
