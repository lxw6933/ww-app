package com.ww.mall.common.annotation;

import java.lang.annotation.*;

/**
 * @description:
 * @author: ww
 * @create: 2023/7/21 22:37
 **/
@Documented
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface MqFallBack {

}
