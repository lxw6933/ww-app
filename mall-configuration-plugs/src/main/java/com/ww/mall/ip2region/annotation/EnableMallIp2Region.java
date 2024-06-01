package com.ww.mall.ip2region.annotation;

import com.ww.mall.ip2region.Ip2RegionConfiguration;
import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author ww
 * @create 2024-06-01 19:26
 * @description:
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Import({Ip2RegionConfiguration.class})
public @interface EnableMallIp2Region {
}
