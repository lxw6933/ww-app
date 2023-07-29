CREATE TABLE `t_category`
(
    `id`            bigint NOT NULL AUTO_INCREMENT COMMENT '物理主键',
    `category_name` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL COMMENT '分类名称',
    `categoryLevel` varchar(20)                                            DEFAULT NULL COMMENT '类目层级',
    `pid`           bigint                                                 DEFAULT NULL COMMENT '父分类id',
    `state`         tinyint(1)                                             DEFAULT '1' COMMENT '是否显示[0-不显示，1显示]',
    `sort`          int                                                    DEFAULT '0' COMMENT '排序',
    `icon`          varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL COMMENT '图标地址',
    `product_unit`  varchar(10) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL COMMENT '计量单位',
    `product_count` int                                                    DEFAULT NULL COMMENT '商品数量',
    `version`       bigint                                                 DEFAULT NULL,
    `creator_id`    bigint                                                 DEFAULT NULL,
    `updater_id`    bigint                                                 DEFAULT NULL,
    `create_time`   datetime                                               DEFAULT NULL,
    `update_time`   datetime                                               DEFAULT NULL,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb3;
























