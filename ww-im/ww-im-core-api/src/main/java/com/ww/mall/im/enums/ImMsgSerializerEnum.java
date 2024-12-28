package com.ww.mall.im.enums;

import lombok.Getter;

/**
 * @author ww
 * @create 2024-11-09 21:13
 * @description: 消息序列化类型
 */
@Getter
public enum ImMsgSerializerEnum {

    JSON(1),
    JACKSON(2),
    PROTOBUF(3);

    public final int type;

    ImMsgSerializerEnum(int type) {
        this.type = type;
    }

}
