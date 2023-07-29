CREATE TABLE `t_category`
(
    `id`             bigint NOT NULL AUTO_INCREMENT COMMENT '物理主键',
    `category_name`  varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL COMMENT '分类名称',
    `category_level` varchar(20)                                            DEFAULT NULL COMMENT '类目层级',
    `pid`            bigint                                                 DEFAULT NULL COMMENT '父分类id',
    `state`          tinyint(1)                                             DEFAULT '1' COMMENT '是否显示[0-不显示，1显示]',
    `sort`           int                                                    DEFAULT '0' COMMENT '排序',
    `icon`           varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL COMMENT '图标地址',
    `product_unit`   varchar(10) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL COMMENT '计量单位',
    `product_count`  int                                                    DEFAULT NULL COMMENT '商品数量',
    `version`        bigint                                                 DEFAULT NULL,
    `creator_id`     bigint                                                 DEFAULT NULL,
    `updater_id`     bigint                                                 DEFAULT NULL,
    `create_time`    datetime                                               DEFAULT NULL,
    `update_time`    datetime                                               DEFAULT NULL,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb3;

CREATE TABLE `t_brand`
(
    `id`          bigint NOT NULL AUTO_INCREMENT COMMENT '物理主键',
    `brand_name`  varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL COMMENT '品牌名',
    `logo`        varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL COMMENT '品牌logo地址',
    `desc_info`   varchar(255)                                           DEFAULT NULL COMMENT '介绍',
    `state`       tinyint(1)                                             DEFAULT '1' COMMENT '显示状态[0-不显示；1-显示]',
    `sort`        int                                                    DEFAULT '0' COMMENT '排序',
    `version`     bigint                                                 DEFAULT NULL,
    `creator_id`  bigint                                                 DEFAULT NULL,
    `updater_id`  bigint                                                 DEFAULT NULL,
    `create_time` datetime                                               DEFAULT NULL,
    `update_time` datetime                                               DEFAULT NULL,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb3;

CREATE TABLE `t_category_brand_relation`
(
    `id`          bigint NOT NULL AUTO_INCREMENT COMMENT '物理主键',
    `brand_id`    bigint NOT NULL COMMENT '品牌id',
    `category_id` bigint NOT NULL COMMENT '分类id',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb3;

CREATE TABLE `t_spu`
(
    `id`          bigint NOT NULL AUTO_INCREMENT COMMENT '物理主键',
    `merchant_id` bigint                                                 DEFAULT NULL COMMENT '商家id',
    `spu_name`    varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL COMMENT '商品名称',
    `catalog_id`  bigint                                                 DEFAULT NULL COMMENT '所属分类id',
    `brand_id`    bigint                                                 DEFAULT NULL COMMENT '品牌id',
    `spu_code`    varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL COMMENT '商品编码',
    `unit`        bigint                                                 DEFAULT '1' COMMENT '商品单位【默认：1 件】',
    `fare_tmp_id` bigint                                                 DEFAULT '1' COMMENT '运费模板【默认：1 包邮】',
    `valid`       tinyint(1)                                             DEFAULT '1' COMMENT '是否有效',
    `spu_type`    varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL COMMENT '商品类型【虚拟、实物】',
    `spu_status`  varchar(20) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL COMMENT '商品状态【上下架、冻结】',
    `channel_ids` varchar(255)                                           DEFAULT NULL COMMENT '分发渠道id集合',
    `version`     bigint                                                 DEFAULT NULL,
    `creator_id`  bigint                                                 DEFAULT NULL,
    `updater_id`  bigint                                                 DEFAULT NULL,
    `create_time` datetime                                               DEFAULT NULL,
    `update_time` datetime                                               DEFAULT NULL,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb3;

CREATE TABLE `t_spu_info_desc`
(
    `id`        bigint NOT NULL AUTO_INCREMENT COMMENT '物理主键',
    `spu_id`    bigint NOT NULL COMMENT '商品id',
    `desc_info` text CHARACTER SET utf8 COLLATE utf8_general_ci COMMENT '商品html介绍',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb3;

CREATE TABLE `t_spu_img`
(
    `id`          bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
    `spu_id`      bigint                                                  DEFAULT NULL COMMENT 'spu_id',
    `img_name`    varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci  DEFAULT NULL COMMENT '图片名',
    `img_url`     varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL COMMENT '图片地址',
    `img_sort`    int                                                     DEFAULT '0' COMMENT '顺序',
    `default_img` tinyint(1)                                              DEFAULT NULL COMMENT '是否默认图',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb3;

CREATE TABLE `t_spu_attr_value`
(
    `id`         bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
    `spu_id`     bigint                                                 DEFAULT NULL COMMENT '商品id',
    `attr_id`    bigint                                                 DEFAULT NULL COMMENT '属性id',
    `attr_name`  varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL COMMENT '属性名',
    `attr_value` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL COMMENT '属性值',
    `sort`       int                                                    DEFAULT '0' COMMENT '顺序',
    `valid`      tinyint(1)                                             DEFAULT '1' COMMENT '是否有效',
    `deleted`    tinyint(1)                                             DEFAULT '0' COMMENT '是否删除',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb3;

CREATE TABLE `t_sku`
(
    `id`          bigint NOT NULL AUTO_INCREMENT COMMENT '物理主键',
    `spu_id`      bigint                                                 DEFAULT NULL COMMENT 'spuId',
    `sku_code`    varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL COMMENT 'sku编码',
    `sku_name`    varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL COMMENT 'sku名称',
    `sku_img`     varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL COMMENT 'sku图片',
    `price`       decimal(18, 2)                                         DEFAULT NULL COMMENT '价格',
    `state`       tinyint(1)                                             DEFAULT '1' COMMENT '是否启用',
    `valid`       tinyint(1)                                             DEFAULT '1' COMMENT '是否有效',
    `version`     bigint                                                 DEFAULT NULL,
    `creator_id`  bigint                                                 DEFAULT NULL,
    `updater_id`  bigint                                                 DEFAULT NULL,
    `create_time` datetime                                               DEFAULT NULL,
    `update_time` datetime                                               DEFAULT NULL,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb3;

CREATE TABLE `t_sku_img`
(
    `id`          bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
    `sku_id`      bigint       DEFAULT NULL COMMENT 'sku_id',
    `img_url`     varchar(255) DEFAULT NULL COMMENT '图片地址',
    `img_sort`    int          DEFAULT '0' COMMENT '排序',
    `default_img` tinyint(1)   DEFAULT NULL COMMENT '默认图[0 - 不是默认图，1 - 是默认图]',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb3;

CREATE TABLE `t_sku_attr_value`
(
    `id`         bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
    `sku_id`     bigint                                                 DEFAULT NULL COMMENT 'sku_id',
    `spu_id`     bigint                                                 DEFAULT NULL COMMENT 'spu_id',
    `attr_id`    bigint                                                 DEFAULT NULL COMMENT 'attr_id',
    `attr_name`  varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL COMMENT '销售属性名',
    `attr_value` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL COMMENT '销售属性值',
    `sort`       int                                                    DEFAULT '0' COMMENT '顺序',
    `valid`      tinyint(1)                                             DEFAULT '1' COMMENT '是否有效',
    `deleted`    tinyint(1)                                             DEFAULT '0' COMMENT '是否删除',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb3;

CREATE TABLE `t_attr`
(
    `id`           bigint NOT NULL AUTO_INCREMENT COMMENT '属性id',
    `attr_name`    varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL COMMENT '属性名',
    `icon`         varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL COMMENT '属性图标',
    `attr_type`    varchar(20)                                            DEFAULT NULL COMMENT '属性类型',
    `value_select` varchar(255)                                           DEFAULT NULL COMMENT '可选值列表[用逗号分隔]',
    `state`        tinyint(1)                                             DEFAULT '1' COMMENT '启用状态[0 - 禁用，1 - 启用]',
    `category_id`  bigint                                                 DEFAULT NULL COMMENT '所属分类',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb3;

CREATE TABLE `t_attr_group`
(
    `id`              bigint NOT NULL AUTO_INCREMENT COMMENT '分组id',
    `attr_group_name` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL COMMENT '组名',
    `sort`            int                                                    DEFAULT '0' COMMENT '排序',
    `desc_info`       varchar(255)                                           DEFAULT NULL COMMENT '描述',
    `icon`            varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL COMMENT '组图标',
    `category_id`     bigint                                                 DEFAULT NULL COMMENT '所属分类id',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb3;

CREATE TABLE `t_attr_group_relation`
(
    `id`            bigint NOT NULL AUTO_INCREMENT COMMENT 'id',
    `attr_id`       bigint DEFAULT NULL COMMENT '属性id',
    `attr_group_id` bigint DEFAULT NULL COMMENT '属性分组id',
    `attr_sort`     int    DEFAULT '0' COMMENT '属性组内排序',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb3;





















