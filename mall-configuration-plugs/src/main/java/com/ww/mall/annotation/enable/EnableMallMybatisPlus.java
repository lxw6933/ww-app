package com.ww.mall.annotation.enable;

import com.ww.mall.mybatisplus.MallMybatisPlusAutoConfiguration;
import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author ww
 * @create 2024-06-01 10:45
 * @description:
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Import({MallMybatisPlusAutoConfiguration.class})
public @interface EnableMallMybatisPlus {
}
