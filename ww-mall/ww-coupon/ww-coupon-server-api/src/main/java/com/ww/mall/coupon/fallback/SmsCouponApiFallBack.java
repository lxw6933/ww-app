package com.ww.mall.coupon.fallback;

import com.ww.app.common.common.Result;
import com.ww.app.common.enums.GlobalResCodeConstants;
import com.ww.mall.coupon.rpc.SmsCouponApi;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;

/**
 * @author ww
 * @create 2024-11-15- 14:40
 * @description:
 */
@Slf4j
public class SmsCouponApiFallBack implements FallbackFactory<SmsCouponApi> {
    @Override
    public SmsCouponApi create(Throwable cause) {
        log.error("优惠券服务【SmsCouponServerFeignService】调用异常：{}", cause.getMessage());
        return new SmsCouponApi() {

            @Override
            public Result<Boolean> receiveCoupon(String activityCode) {
                return Result.error(GlobalResCodeConstants.LIMIT_REQUEST);
            }

            @Override
            public Result<Boolean> convertCoupon(String couponCode) {
                return Result.error(GlobalResCodeConstants.LIMIT_REQUEST);
            }

        };
    }
}
