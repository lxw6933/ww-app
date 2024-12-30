package com.ww.app.im.serializer;

import com.alibaba.fastjson.JSON;
import com.ww.app.im.enums.ImMsgSerializerEnum;
import org.springframework.stereotype.Component;

/**
 * @author ww
 * @create 2024-05-06 22:40
 * @description:
 */
@Component
public class JsonSerializer implements ImMsgSerializer {
    @Override
    public ImMsgSerializerEnum type() {
        return ImMsgSerializerEnum.JSON;
    }

    @Override
    public <T> byte[] serialize(T object) {
        return JSON.toJSONBytes(object);
    }

    @Override
    public <T> T deserialize(Class<T> clazz, byte[] bytes) {
        return JSON.parseObject(bytes, clazz);
    }
}
