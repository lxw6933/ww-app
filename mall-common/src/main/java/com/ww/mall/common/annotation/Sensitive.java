package com.ww.mall.common.annotation;

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.ww.mall.common.enums.SensitiveDataType;
import com.ww.mall.common.serializer.SensitiveJsonSerializer;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author ww
 * @create 2024-04-26- 10:46
 * @description:
 */
@JacksonAnnotationsInside
@JsonSerialize(using = SensitiveJsonSerializer.class)
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Sensitive {

    SensitiveDataType sensitiveDataType();
}
