package com.ww.mall.coupon.constant;

/**
 * @author ww
 * @create 2024-09-19- 14:01
 * @description:
 */
public interface LogRecordConstants {

    String SYSTEM_COUPON_TYPE = "SYSTEM 优惠券";
    String SYSTEM_COUPON_CREATE_SUB_TYPE = "创建优惠券";
    String SYSTEM_COUPON_CREATE_SUCCESS = "创建了优惠券【{{#smsCouponActivity.name}}】";
    String SYSTEM_MERCHANT_COUPON_CREATE_SUCCESS = "创建了商家优惠券【{{#merchantCouponActivity.name}}】";
    String SYSTEM_COUPON_UPDATE_SUB_TYPE = "更新优惠券";
    String SYSTEM_COUPON_UPDATE_SUCCESS = "更新了优惠券【{{#smsCouponActivityName}}】: {_DIFF{#smsCouponActivityEditBO}}";
    String SYSTEM_MERCHANT_COUPON_UPDATE_SUCCESS = "更新了优惠券【{{#merchantCouponActivityName}}】: {_DIFF{#merchantCouponActivityEditBO}}";
    String SYSTEM_COUPON_STATUS_SUB_TYPE = "更新优惠券状态";
    String SYSTEM_COUPON_STATUS_SUCCESS = "更新了优惠券【{{#activityCode}}】状态, 更新为【{{newStatus}}】";
    String SYSTEM_MERCHANT_COUPON_STATUS_SUCCESS = "更新了商家优惠券【{{#activityCode}}】状态, 更新为【{{newStatus}}】";
    String SYSTEM_COUPON_AUDIT_SUB_TYPE = "审核优惠券活动";
    String SYSTEM_MERCHANT_COUPON_AUDIT_SUCCESS = "审核了商家优惠券【{{#activityCode}}】活动, 审核状态【{{newStatus}}】";
    String SYSTEM_COUPON_ADD_CODE_SUB_TYPE = "生成优惠券券码";
    String SYSTEM_COUPON_ADD_CODE_SUCCESS = "生成优惠券【{{#activityCode}}】数量【{{num}}】";
    String SYSTEM_COUPON_EXPORT_SUB_TYPE = "导出优惠券";
    String SYSTEM_COUPON_EXPORT_SUCCESS = "导出优惠券参数【{{#export}}】";

}
