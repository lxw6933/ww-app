package com.ww.app.coupon.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author ww
 * @create 2023-07-14- 11:16
 * @description:
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "coupon")
public class CouponProperties {

    private String couponId;

}
