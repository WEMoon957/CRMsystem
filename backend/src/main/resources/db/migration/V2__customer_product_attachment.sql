-- =====================================================
-- V2: 客户产品资料 + 图片/附件
-- =====================================================

CREATE TABLE IF NOT EXISTS customer_product (
    id            BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
    customer_id   BIGINT        NOT NULL COMMENT '关联客户',
    product_name  VARCHAR(128)  NOT NULL COMMENT '产品名称',
    spec          VARCHAR(128)  NULL COMMENT '规格/型号',
    quantity      INT           NULL COMMENT '数量',
    unit_price    DECIMAL(12,2) NULL COMMENT '单价',
    remark        VARCHAR(512)  NULL COMMENT '备注',
    created_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted       TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    KEY idx_customer (customer_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='客户产品资料表';

CREATE TABLE IF NOT EXISTS customer_attachment (
    id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
    customer_id  BIGINT       NOT NULL COMMENT '关联客户',
    file_name    VARCHAR(255) NOT NULL COMMENT '原始文件名',
    file_type    VARCHAR(20)  NOT NULL DEFAULT 'image' COMMENT '类型：image/doc/other',
    file_path    VARCHAR(255) NOT NULL COMMENT '存储相对路径',
    file_size    BIGINT       NULL COMMENT '字节数',
    created_by   BIGINT       NULL COMMENT '上传人',
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted      TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (id),
    KEY idx_customer (customer_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='客户附件表';
