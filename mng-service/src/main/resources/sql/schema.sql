-- ============================================================
-- Skada 排行榜 SaaS 平台 - 数据库建表脚本
-- 数据库：skada (所有后端服务共用)
-- 字符集：utf8mb4_general_ci
-- ============================================================

CREATE DATABASE IF NOT EXISTS skada
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_general_ci;

USE skada;

-- ------------------------------------------------------------
-- 管理员用户表
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS admin_user (
    id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    username     VARCHAR(64)  NOT NULL COMMENT '登录用户名',
    password_hash VARCHAR(256) NOT NULL COMMENT '密码哈希(BCrypt)',
    display_id   VARCHAR(20)  NOT NULL COMMENT '显示ID，格式: ad_xxxxxxxx(8位以上)',
    role         VARCHAR(16)  NOT NULL DEFAULT 'viewer' COMMENT '角色: admin/viewer',
    status       TINYINT      NOT NULL DEFAULT 1 COMMENT '状态: 1=启用, 0=停用',
    create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by    VARCHAR(64)  NOT NULL DEFAULT 'system' COMMENT '创建人',
    update_by    VARCHAR(64)  NOT NULL DEFAULT 'system' COMMENT '更新人',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username),
    UNIQUE KEY uk_display_id (display_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='管理员用户';

-- ------------------------------------------------------------
-- 租户表
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS tenant (
    id                    BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id             VARCHAR(32)  NOT NULL COMMENT '租户唯一标识(生成)',
    name                  VARCHAR(128) NOT NULL COMMENT '租户名称',
    secret_key            VARCHAR(64)  NOT NULL COMMENT '租户密钥(生成)',
    allow_anonymous_query TINYINT      NOT NULL DEFAULT 0 COMMENT '是否允许匿名查询: 1=允许, 0=禁止',
    status                TINYINT      NOT NULL DEFAULT 1 COMMENT '状态: 1=启用, 0=停用',
    create_time           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by             VARCHAR(64)  NOT NULL DEFAULT 'system' COMMENT '创建人',
    update_by             VARCHAR(64)  NOT NULL DEFAULT 'system' COMMENT '更新人',
    PRIMARY KEY (id),
    UNIQUE KEY uk_tenant_id (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='租户';

-- ------------------------------------------------------------
-- 指标表 (Metric)
-- 租户定义的上报维度，如"击杀数"、"得分"、"等级"等
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS metric (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id   VARCHAR(32)  NOT NULL COMMENT '所属租户ID',
    name        VARCHAR(128) NOT NULL COMMENT '指标名称',
    code        VARCHAR(64)  NOT NULL COMMENT '指标编码(租户内唯一)',
    description VARCHAR(256) NULL     COMMENT '指标描述',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by   VARCHAR(64)  NOT NULL DEFAULT 'system' COMMENT '创建人',
    update_by   VARCHAR(64)  NOT NULL DEFAULT 'system' COMMENT '更新人',
    PRIMARY KEY (id),
    UNIQUE KEY uk_tenant_code (tenant_id, code),
    KEY idx_tenant_id (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='指标';

-- ------------------------------------------------------------
-- 排行榜计划表 (Leaderboard Plan)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS leaderboard_plan (
    id                    BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id             VARCHAR(32)  NOT NULL COMMENT '所属租户ID',
    name                  VARCHAR(128) NOT NULL COMMENT '排行榜名称',
    start_time            BIGINT       NOT NULL COMMENT '开始时间(毫秒时间戳)',
    end_time              BIGINT       NULL     COMMENT '结束时间(毫秒时间戳，空=永不结束)',
    max_query_users       INT          NOT NULL DEFAULT 1000 COMMENT '最大可查询用户数',
    allow_duplicate_report TINYINT     NOT NULL DEFAULT 0 COMMENT '是否允许同一用户重复上报: 1=允许, 0=禁止',
    allow_history_query   TINYINT      NOT NULL DEFAULT 1 COMMENT '是否支持查询历史榜单: 1=允许, 0=禁止',
    roll_strategy         VARCHAR(16)  NOT NULL DEFAULT 'none' COMMENT '滚动策略: none=不滚动, periodic=周期性, user_count=按用户数',
    roll_interval_value   INT          NULL     COMMENT '周期性滚动-间隔值',
    roll_interval_unit    VARCHAR(8)   NULL     COMMENT '周期性滚动-时间单位: minute/hour/day',
    roll_user_count       INT          NULL     COMMENT '用户数滚动-触发阈值',
    status                VARCHAR(16)  NOT NULL DEFAULT 'active' COMMENT '状态: active=进行中, stopped=已终止',
    current_instance_id   BIGINT       NULL     COMMENT '当前活跃实例ID',
    create_time           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by             VARCHAR(64)  NOT NULL DEFAULT 'system' COMMENT '创建人',
    update_by             VARCHAR(64)  NOT NULL DEFAULT 'system' COMMENT '更新人',
    PRIMARY KEY (id),
    KEY idx_tenant_id (tenant_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='排行榜计划';

-- ------------------------------------------------------------
-- 排行榜关联指标表
-- 每个排行榜计划关联一个或多个指标，按优先级排序
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS leaderboard_metric (
    id              BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    leaderboard_id  BIGINT      NOT NULL COMMENT '排行榜计划ID',
    metric_id       BIGINT      NOT NULL COMMENT '指标ID',
    priority        INT         NOT NULL DEFAULT 1 COMMENT '优先级(越小越优先)',
    sort_order      VARCHAR(8)  NOT NULL DEFAULT 'desc' COMMENT '排序方向: asc=升序, desc=降序',
    create_time     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_leaderboard_metric (leaderboard_id, metric_id),
    KEY idx_leaderboard_id (leaderboard_id),
    KEY idx_metric_id (metric_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='排行榜关联指标';

-- ------------------------------------------------------------
-- 排行榜实例表 (Leaderboard Instance)
-- 排行榜计划的具体执行实例，每次滚动产生一个新实例
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS leaderboard_instance (
    id                BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    leaderboard_id    BIGINT       NOT NULL COMMENT '所属排行榜计划ID',
    instance_seq      INT          NOT NULL COMMENT '实例序号(从1递增)',
    start_time        BIGINT       NOT NULL COMMENT '实例开始时间(毫秒时间戳)',
    end_time          BIGINT       NULL     COMMENT '实例结束时间(毫秒时间戳)',
    status            VARCHAR(16)  NOT NULL DEFAULT 'active' COMMENT '状态: active=进行中, closed=已关闭',
    create_time       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    create_by         VARCHAR(64)  NOT NULL DEFAULT 'system' COMMENT '创建人',
    update_by         VARCHAR(64)  NOT NULL DEFAULT 'system' COMMENT '更新人',
    PRIMARY KEY (id),
    KEY idx_leaderboard_id (leaderboard_id),
    KEY idx_leaderboard_status (leaderboard_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='排行榜实例';

-- ------------------------------------------------------------
-- 分数记录表（玩家指标数据）
-- 记录用户在某个排行榜实例中某个指标的值
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS score_record (
    id             BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    tenant_id      VARCHAR(32)   NOT NULL COMMENT '租户ID',
    leaderboard_id BIGINT        NOT NULL COMMENT '排行榜计划ID',
    instance_id    BIGINT        NOT NULL COMMENT '排行榜实例ID',
    metric_id      BIGINT        NOT NULL COMMENT '指标ID',
    user_id        VARCHAR(128)  NOT NULL COMMENT '用户ID(由租户定义)',
    score          DECIMAL(20,4) NOT NULL COMMENT '指标值',
    payload        TEXT          NULL     COMMENT '透传数据(JSON格式)',
    create_time    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_leaderboard_instance (leaderboard_id, instance_id),
    KEY idx_leaderboard_instance_metric (leaderboard_id, instance_id, metric_id),
    KEY idx_leaderboard_instance_metric_score (leaderboard_id, instance_id, metric_id, score),
    KEY idx_tenant_id (tenant_id),
    KEY idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='分数记录';

-- ------------------------------------------------------------
-- 初始数据：创建默认管理员
-- 密码: admin123 (BCrypt hash)
-- ------------------------------------------------------------
INSERT INTO admin_user (username, password_hash, display_id, role, create_by, update_by)
VALUES ('admin', '$2a$10$.S6A4B45AcyZ5PVVEJOILu02rcDLeUc5Tb1efvrUB4mdeWDjFRfia',
        'ad_00000001', 'admin', 'system', 'system')
ON DUPLICATE KEY UPDATE username = username;
