package com.ww.mall.coupon.config;

import com.ww.mall.coupon.fallback.SmsCouponApiFallBack;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author ww
 * @create 2025-03-06- 17:37
 * @description:
 */
@Configuration
public class FeignFallBackConfiguration {

    @Bean
    public SmsCouponApiFallBack smsCouponApiFallBack() {
        return new SmsCouponApiFallBack();
    }

}
