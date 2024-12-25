package com.ww.mall.im.serialize;

import com.ww.mall.common.exception.ApiException;
import com.ww.mall.im.enums.ImMsgSerializerEnum;
import org.springframework.stereotype.Component;

/**
 * @author ww
 * @create 2024-05-06 22:40
 * @description:
 */
@Component
public class ProtobufSerializer implements ImMsgSerializer {

    @Override
    public ImMsgSerializerEnum type() {
        return ImMsgSerializerEnum.PROTOBUF;
    }

    @Override
    public <T> byte[] serialize(T object) {
        throw new ApiException("暂不支持当前消息序列化类型");
    }

    @Override
    public <T> T deserialize(Class<T> clazz, byte[] bytes) {
        throw new ApiException("暂不支持当前消息序列化类型");
    }
}
