package com.skada.common.enums;

/**
 * 业务错误码常量
 * <p>
 * 三级分层：
 *   10000-19999 — 系统错误（参数缺失、格式错误、内部异常）
 *   20000-29999 — SAAS接入层（签名校验、租户鉴权、时间戳过期）
 *   30000-39999 — 业务错误（排行榜、实例、并发冲突等）
 * </p>
 */
public final class BizCode {

    private BizCode() {}

    // ==================== 系统错误 (10000-19999) ====================

    /** 通用系统错误 */
    public static final int SYSTEM_ERROR = 10000;
    /** 必传参数缺失 */
    public static final int PARAM_MISSING = 10001;
    /** 请求体格式错误 */
    public static final int REQUEST_FORMAT_ERROR = 10002;
    /** 服务器内部错误 */
    public static final int INTERNAL_ERROR = 10003;

    // ==================== SAAS接入层 (20000-29999) ====================

    /** 通用SAAS错误 */
    public static final int SAAS_ERROR = 20000;
    /** 缺少 X-Tenant-Id */
    public static final int TENANT_ID_MISSING = 20001;
    /** 缺少 X-Sign */
    public static final int SIGN_MISSING = 20002;
    /** 签名校验失败 */
    public static final int SIGN_INVALID = 20003;
    /** 缺少 X-Timestamp */
    public static final int TIMESTAMP_MISSING = 20004;
    /** 时间戳过期（偏差超过5分钟） */
    public static final int TIMESTAMP_EXPIRED = 20005;
    /** 租户不存在或已停用 */
    public static final int TENANT_NOT_FOUND_OR_DISABLED = 20006;
    /** 租户鉴权失败 */
    public static final int TENANT_AUTH_FAILED = 20007;

    // ==================== 业务错误 (30000-39999) ====================

    /** 通用业务错误 */
    public static final int BIZ_ERROR = 30000;
    /** 排行榜不存在或无权操作 */
    public static final int LEADERBOARD_NOT_FOUND_OR_DENIED = 30001;
    /** 排行榜已终止，无法操作 */
    public static final int LEADERBOARD_STOPPED = 30002;
    /** 排行榜实例已变更 */
    public static final int INSTANCE_CHANGED = 30003;
    /** 当前无活跃实例 */
    public static final int NO_ACTIVE_INSTANCE = 30004;
    /** 缺少活跃实例ID */
    public static final int INSTANCE_ID_REQUIRED = 30005;
    /** 并发操作冲突 */
    public static final int CONCURRENT_CONFLICT = 30006;
    /** 资源不存在 */
    public static final int RESOURCE_NOT_FOUND = 30007;
}
