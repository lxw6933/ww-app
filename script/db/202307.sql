CREATE TABLE `t_member`
(
    `id`                 bigint NOT NULL AUTO_INCREMENT COMMENT '物理主键',
    `open_id`            varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL COMMENT 'OpenId',
    `channel_id`         bigint                                                 DEFAULT NULL COMMENT '渠道ID',
    `password`           varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL COMMENT '密码',
    `nick_name`          varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL COMMENT '昵称',
    `mobile`             varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL COMMENT '手机号',
    `head_img`           varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL COMMENT '头像',
    `occupy_integral`    int                                                    DEFAULT '0' COMMENT '锁定积分',
    `available_integral` int                                                    DEFAULT '0' COMMENT '可用积分',
    `gender`             int                                                    DEFAULT '-1' COMMENT '性别(-1：未知 0：女 1：男)',
    `birthday`           datetime                                               DEFAULT NULL COMMENT '出生日期',
    `email`              varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci DEFAULT NULL COMMENT '邮箱',
    `blacklist`          tinyint(1) DEFAULT '0' COMMENT '是否黑名单用户',
    `version`            bigint                                                 DEFAULT '1',
    `creator_id`         bigint                                                 DEFAULT NULL,
    `updater_id`         bigint                                                 DEFAULT NULL,
    `create_time`        datetime                                               DEFAULT NULL,
    `update_time`        datetime                                               DEFAULT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=22 DEFAULT CHARSET=utf8mb3;