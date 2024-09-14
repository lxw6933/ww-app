CREATE TABLE `sys_user`
(
    `id`          bigint                                                       NOT NULL AUTO_INCREMENT COMMENT '物理主键',
    `username`    varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '用户名（账号名称）',
    `real_name`   varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci  DEFAULT NULL COMMENT '用户昵称（姓名）',
    `password`    varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '密码',
    `salt`        varchar(32) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci  DEFAULT NULL COMMENT '密码盐',
    `avatar`      varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '头像',
    `email`       varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci  DEFAULT NULL COMMENT '邮箱',
    `phone`       varchar(20) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci  DEFAULT NULL COMMENT '电话号码',
    `remark`      varchar(255)                                                  DEFAULT NULL COMMENT '备注',
    `status`      tinyint(1) DEFAULT NULL COMMENT '状态',
    `valid`       tinyint(1) DEFAULT NULL COMMENT '是否有效',
    `version`     bigint                                                        DEFAULT NULL,
    `creator_id`  bigint                                                        DEFAULT NULL,
    `updater_id`  bigint                                                        DEFAULT NULL,
    `create_time` datetime                                                      DEFAULT NULL,
    `update_time` datetime                                                      DEFAULT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE `sys_role`
(
    `id`          bigint                                                       NOT NULL AUTO_INCREMENT COMMENT '物理主键',
    `name`        varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '角色名称',
    `role_no`     varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci NOT NULL COMMENT '角色编号',
    `remark`      varchar(255) DEFAULT NULL COMMENT '备注',
    `valid`       tinyint(1) NOT NULL COMMENT '是否有效',
    `version`     bigint       DEFAULT NULL,
    `creator_id`  bigint       DEFAULT NULL,
    `updater_id`  bigint       DEFAULT NULL,
    `create_time` datetime     DEFAULT NULL,
    `update_time` datetime     DEFAULT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE `sys_menu`
(
    `id`          bigint                                                        NOT NULL AUTO_INCREMENT COMMENT '物理主键',
    `pid`         bigint                                                        DEFAULT NULL COMMENT '父级编号',
    `name`        varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci  NOT NULL COMMENT '菜单名称',
    `type`        varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci  NOT NULL COMMENT '类型',
    `url`         varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT 'URL地址',
    `icon`        varchar(50) CHARACTER SET utf8mb3 COLLATE utf8mb3_general_ci  DEFAULT NULL COMMENT '图标',
    `permission`  varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '权限标识',
    `sort`        int                                                           DEFAULT NULL COMMENT '排序',
    `valid`       tinyint(1) NOT NULL COMMENT '是否有效',
    `version`     bigint                                                        DEFAULT NULL,
    `creator_id`  bigint                                                        DEFAULT NULL,
    `updater_id`  bigint                                                        DEFAULT NULL,
    `create_time` datetime                                                      DEFAULT NULL,
    `update_time` datetime                                                      DEFAULT NULL,
    PRIMARY KEY (`id`)
) ENGINE=InnoDB CHARSET = utf8mb4;

CREATE TABLE `sys_role_menu`
(
    `role_id` bigint NOT NULL COMMENT '角色id',
    `menu_id` bigint NOT NULL COMMENT '菜单id'
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE `sys_role_user`
(
    `role_id` bigint NOT NULL COMMENT '角色id',
    `user_id` bigint NOT NULL COMMENT '用户id'
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
