CREATE TABLE `t_coupon`
(
    `id`                          bigint                                                  NOT NULL AUTO_INCREMENT COMMENT '物理主键',
    `activity_code`               varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '活动唯一编码',
    `channel_id`                  bigint         DEFAULT NULL COMMENT '渠道id',
    `merchant_id`                 bigint         DEFAULT NULL COMMENT '商家id',
    `title`                       varchar(255)   DEFAULT NULL COMMENT '优惠券名称',
    `achieve_amount`              decimal(18, 2) DEFAULT NULL COMMENT '优惠券需满X金额',
    `deduction_amount`            decimal(18, 2) DEFAULT NULL COMMENT '优惠券扣减金额【折扣】',
    `receive_start_time`          datetime       DEFAULT NULL COMMENT '领取开始时间',
    `receive_end_time`            datetime       DEFAULT NULL COMMENT '领取结束时间',
    `use_start_time`              datetime       DEFAULT NULL COMMENT '优惠券允许使用开始时间【CouponUseTimeType】',
    `use_end_time`                datetime       DEFAULT NULL COMMENT '优惠券允许使用结束时间【CouponUseTimeType】',
    `receive_after_day_effect`    int            DEFAULT NULL COMMENT '指定领取后多少天数后可用【CouponUseTimeType】',
    `receive_after_effect_day`    int            DEFAULT NULL COMMENT '指定领取后达到使用时间后，多少天过期【CouponUseTimeType】',
    `init_total_coupon_number`    int            DEFAULT NULL COMMENT '初始化多少张优惠券',
    `init_success`                tinyint(1)     DEFAULT NULL COMMENT '是否初始化完成',
    `coupon_limit_receive_number` int            DEFAULT NULL COMMENT '优惠券在限制时间范围内允许领取的数量',
    `state`                       tinyint(1)     DEFAULT NULL COMMENT '上下架状态',
    `remark`                      varchar(255)   DEFAULT NULL COMMENT '详细说明',
    `version`                     bigint         DEFAULT NULL,
    `creator_id`                  bigint         DEFAULT NULL,
    `updater_id`                  bigint         DEFAULT NULL,
    `create_time`                 datetime       DEFAULT NULL,
    `update_time`                 datetime       DEFAULT NULL,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb3;