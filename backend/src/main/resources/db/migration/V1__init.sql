-- =====================================================
-- V1: 初始表结构（用户 / 客户 / 跟进记录 / 刷新令牌）
-- =====================================================

CREATE TABLE IF NOT EXISTS sys_user (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    username    VARCHAR(32)  NOT NULL COMMENT '登录名',
    password    VARCHAR(100) NOT NULL COMMENT 'BCrypt 密码哈希',
    real_name   VARCHAR(32)  NULL COMMENT '姓名',
    phone       VARCHAR(20)  NULL COMMENT '手机号',
    role        VARCHAR(10)  NOT NULL DEFAULT 'MEMBER' COMMENT '角色：ADMIN=主账号 MEMBER=子账号',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1启用 0禁用',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username, deleted)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='系统用户表';

CREATE TABLE IF NOT EXISTS customer (
    id                 BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    name               VARCHAR(64)   NOT NULL COMMENT '客户姓名',
    phone              VARCHAR(20)   NULL COMMENT '联系电话',
    company            VARCHAR(128)  NULL COMMENT '公司名称',
    position           VARCHAR(64)   NULL COMMENT '职位',
    source             VARCHAR(32)   NULL COMMENT '客户来源（转介绍/广告/陌拜/展会/线上咨询等）',
    industry           VARCHAR(64)   NULL COMMENT '所属行业',
    intent_level       VARCHAR(20)   NOT NULL DEFAULT 'PENDING' COMMENT '意向等级：PENDING待跟进 INTERESTED有意向 NOT_INTERESTED无意向 COOPERATED已合作 REFUSED不合作',
    address            VARCHAR(255) NULL COMMENT '联系地址',
    remark             VARCHAR(1024) NULL COMMENT '备注',
    owner_id           BIGINT        NOT NULL COMMENT '客户归属人（sys_user.id）',
    next_followup_at   DATETIME      NULL COMMENT '下次跟进时间',
    last_contacted_at  DATETIME      NULL COMMENT '最近联系时间',
    created_by         BIGINT        NULL COMMENT '创建人',
    created_at         DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted           TINYINT        NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    KEY idx_owner (owner_id),
    KEY idx_intent (intent_level),
    KEY idx_phone (phone)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='客户表';

CREATE TABLE IF NOT EXISTS follow_up_record (
    id                BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    customer_id       BIGINT        NOT NULL COMMENT '关联客户',
    content           VARCHAR(2048) NOT NULL COMMENT '跟进内容',
    result_level      VARCHAR(20)   NULL COMMENT '本次跟进得出的意向结论',
    next_followup_at  DATETIME      NULL COMMENT '下次跟进时间',
    created_by        BIGINT        NULL COMMENT '记录人',
    created_at        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted           TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    KEY idx_customer (customer_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='客户跟进记录表';

CREATE TABLE IF NOT EXISTS sys_refresh_token (
    id          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id     BIGINT      NOT NULL COMMENT '所属用户',
    token       VARCHAR(64) NOT NULL COMMENT '刷新令牌（随机串）',
    expires_at  DATETIME    NOT NULL COMMENT '过期时间',
    created_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_token (token),
    KEY idx_user (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='服务端刷新令牌表';
