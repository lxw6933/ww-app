package com.ww.mall.im.serialize;

import com.ww.mall.im.enums.ImMsgSerializerEnum;

/**
 * @author ww
 * @create 2024-11-09 21:12
 * @description:
 */
public interface ImMsgSerializer {

    ImMsgSerializerEnum type();

    <T> byte[] serialize(T object);

    <T> T deserialize(Class<T> clazz, byte[] bytes);

}
