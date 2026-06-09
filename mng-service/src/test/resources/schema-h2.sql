DROP ALL OBJECTS;

-- ------------------------------------------------------------
-- 管理员用户表
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS admin_user (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    username     VARCHAR(64)  NOT NULL,
    password_hash VARCHAR(256) NOT NULL,
    display_id   VARCHAR(20)  NOT NULL,
    role         VARCHAR(16)  NOT NULL DEFAULT 'viewer',
    status       TINYINT      NOT NULL DEFAULT 1,
    create_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_by    VARCHAR(64)  NOT NULL DEFAULT 'system',
    update_by    VARCHAR(64)  NOT NULL DEFAULT 'system',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username),
    UNIQUE KEY uk_display_id (display_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ------------------------------------------------------------
-- 租户表
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS tenant (
    id                    BIGINT       NOT NULL AUTO_INCREMENT,
    tenant_id             VARCHAR(32)  NOT NULL,
    name                  VARCHAR(128) NOT NULL,
    secret_key            VARCHAR(64)  NOT NULL,
    allow_anonymous_query TINYINT      NOT NULL DEFAULT 0,
    status                TINYINT      NOT NULL DEFAULT 1,
    create_time           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_by             VARCHAR(64)  NOT NULL DEFAULT 'system',
    update_by             VARCHAR(64)  NOT NULL DEFAULT 'system',
    PRIMARY KEY (id),
    UNIQUE KEY uk_tenant_id (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ------------------------------------------------------------
-- 指标表 (Metric)
-- 租户定义的上报维度，如"击杀数"、"得分"、"等级"等
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS metric (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    metric_id   VARCHAR(32)  NOT NULL,
    tenant_id   VARCHAR(32)  NOT NULL,
    name        VARCHAR(128) NOT NULL,
    description VARCHAR(256) NULL    ,
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_by   VARCHAR(64)  NOT NULL DEFAULT 'system',
    update_by   VARCHAR(64)  NOT NULL DEFAULT 'system',
    PRIMARY KEY (id),
    UNIQUE KEY uk_metric_id (metric_id),
    KEY idx_metric_tenant_id (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ------------------------------------------------------------
-- 排行榜计划表 (Leaderboard Plan)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS leaderboard_plan (
    id                    BIGINT       NOT NULL AUTO_INCREMENT,
    plan_id               VARCHAR(32)  NOT NULL,
    tenant_id             VARCHAR(32)  NOT NULL,
    name                  VARCHAR(128) NOT NULL,
    start_time            BIGINT       NOT NULL,
    end_time              BIGINT       NULL    ,
    max_query_users       INT          NOT NULL DEFAULT 1000,
    allow_duplicate_report TINYINT     NOT NULL DEFAULT 0,
    allow_history_query   TINYINT      NOT NULL DEFAULT 1,
    roll_strategy         VARCHAR(16)  NOT NULL DEFAULT 'none',
    roll_interval_value   INT          NULL    ,
    roll_interval_unit    VARCHAR(8)   NULL    ,
    roll_user_count       INT          NULL    ,
    status                VARCHAR(16)  NOT NULL DEFAULT 'active',
    current_instance_id   BIGINT       NULL    ,
    create_time           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time           DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_by             VARCHAR(64)  NOT NULL DEFAULT 'system',
    update_by             VARCHAR(64)  NOT NULL DEFAULT 'system',
    PRIMARY KEY (id),
    UNIQUE KEY uk_plan_id (plan_id),
    KEY idx_plan_tenant_id (tenant_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ------------------------------------------------------------
-- 排行榜关联指标映射表(leaderboard_metric_mapping)
-- 每个排行榜计划关联一个或多个指标，按优先级排序
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS leaderboard_metric_mapping (
    id              BIGINT      NOT NULL AUTO_INCREMENT,
    leaderboard_id  BIGINT      NOT NULL,
    metric_id       BIGINT      NOT NULL,
    priority        INT         NOT NULL DEFAULT 1,
    sort_order      VARCHAR(8)  NOT NULL DEFAULT 'desc',
    create_time     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_by       VARCHAR(64) NOT NULL DEFAULT 'system',
    update_by       VARCHAR(64) NOT NULL DEFAULT 'system',
    PRIMARY KEY (id),
    UNIQUE KEY uk_leaderboard_metric_mapping (leaderboard_id, metric_id),
    KEY idx_metric_id (metric_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ------------------------------------------------------------
-- 排行榜实例表 (Leaderboard Instance)
-- 排行榜计划的具体执行实例，每次滚动产生一个新实例
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS leaderboard_instance (
    id                BIGINT       NOT NULL AUTO_INCREMENT,
    instance_id       VARCHAR(32)  NOT NULL,
    leaderboard_id    BIGINT       NOT NULL,
    instance_seq      INT          NOT NULL,
    start_time        BIGINT       NOT NULL,
    end_time          BIGINT       NULL    ,
    status            VARCHAR(16)  NOT NULL DEFAULT 'active',
    create_time       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    create_by         VARCHAR(64)  NOT NULL DEFAULT 'system',
    update_by         VARCHAR(64)  NOT NULL DEFAULT 'system',
    PRIMARY KEY (id),
    UNIQUE KEY uk_instance_id (instance_id),
    UNIQUE KEY uk_instance_seq (leaderboard_id, instance_seq),
    KEY idx_leaderboard_status (leaderboard_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ------------------------------------------------------------
-- 分数记录表（玩家指标数据）
-- 记录用户在某个排行榜实例中某个指标的值
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS score_record (
    id             BIGINT        NOT NULL AUTO_INCREMENT,
    tenant_id      VARCHAR(32)   NOT NULL,
    leaderboard_id BIGINT        NOT NULL,
    instance_id    BIGINT        NOT NULL,
    metric_id      BIGINT        NOT NULL,
    user_id        VARCHAR(128)  NOT NULL,
    score          DECIMAL(12,2) NOT NULL,
    payload        TEXT          NULL    ,
    create_time    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_instance_metric (leaderboard_id, instance_id, metric_id, user_id),
    KEY idx_leaderboard_instance_metric_score (leaderboard_id, instance_id, metric_id, score),
    KEY idx_score_tenant_id (tenant_id),
    KEY idx_tenant_user (tenant_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- ------------------------------------------------------------
-- 初始数据：创建默认管理员
-- 密码: admin123 (BCrypt hash)
-- ------------------------------------------------------------
INSERT INTO admin_user (username, password_hash, display_id, role, create_by, update_by)
VALUES ('admin', '$2a$10$.S6A4B45AcyZ5PVVEJOILu02rcDLeUc5Tb1efvrUB4mdeWDjFRfia',
        'ad_00000001', 'admin', 'system', 'system')
ON DUPLICATE KEY UPDATE username = username;
