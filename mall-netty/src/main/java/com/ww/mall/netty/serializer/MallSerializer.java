package com.ww.mall.netty.serializer;

import com.ww.mall.netty.enums.SerializerTypeEnum;

/**
 * @author ww
 * @create 2024-05-06 22:31
 * @description:
 */
public interface MallSerializer {

    SerializerTypeEnum type();

    <T> byte[] serialize(T object);

    <T> T deserialize(Class<T> clazz, byte[] bytes);

}
