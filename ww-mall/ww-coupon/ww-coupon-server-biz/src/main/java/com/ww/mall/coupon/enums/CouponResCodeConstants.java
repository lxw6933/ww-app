package com.ww.mall.coupon.enums;

import com.ww.app.common.common.ResCode;

/**
 * @author ww
 * @create 2025-03-17- 09:43
 * @description: 优惠券异常汇总
 */
public interface CouponResCodeConstants {

    // TODO 命名、code、msg 临时想的，后续维护规范
    ResCode DATA_ERROR = new ResCode(500, "数据异常");
    ResCode COUPON_ERROR = new ResCode(500, "券码生成异常");
    ResCode UN_FOUND_ACTIVITY = new ResCode(500, "未能找到有效的优惠券活动");
    ResCode INVALID_CODE = new ResCode(9201, "券码无效");
    ResCode CODE_USED = new ResCode(9202, "券码已使用，请勿重复兑换");
    ResCode COUPON_ACTIVITY_DOWN = new ResCode(9203, "优惠券已下架");
    ResCode COUPON_ACTIVITY_CANT_GET = new ResCode(9204, "优惠券还未到领取时间");
    ResCode EXCEED_RECEIVE_LIMIT = new ResCode(9205, "超出优惠券领取限制");
    ResCode EXCEED_BATCH_MAX_NUMBER = new ResCode(9207, "超出批次券码最大数量");
    ResCode COUPON_SALE_OUT = new ResCode(9206, "优惠券已被抢空");
    ResCode COUPON_STOCK_LESS = new ResCode(9207, "优惠券数量不足");
    ResCode COUPON_STOCK_SUCCESS_BUT_SHOW_DATA_EXCEPTION = new ResCode(9208, "优惠券数量已成功生成，优惠券数量展示数据维护失败");
    ResCode COUPON_USED_EXCEPTION = new ResCode(9209, "优惠券已被使用");

}
