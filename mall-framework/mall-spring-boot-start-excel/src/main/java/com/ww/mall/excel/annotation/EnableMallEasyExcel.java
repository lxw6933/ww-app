package com.ww.mall.excel.annotation;

import com.ww.mall.excel.MallEasyExcelAutoConfiguration;
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
@Import({MallEasyExcelAutoConfiguration.class})
public @interface EnableMallEasyExcel {
}
