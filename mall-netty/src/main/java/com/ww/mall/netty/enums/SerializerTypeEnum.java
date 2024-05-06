package com.ww.mall.netty.enums;

import lombok.Getter;

/**
 * @author ww
 * @create 2024-05-06 22:37
 * @description:
 */
@Getter
public enum SerializerTypeEnum {

    JAVA(1),
    JSON(2),
    JACKSON(3),
    GSON(4);

    public final Integer type;

    SerializerTypeEnum(Integer type) {
        this.type = type;
    }

}
