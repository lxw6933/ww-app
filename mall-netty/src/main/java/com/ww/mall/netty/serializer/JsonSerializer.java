package com.ww.mall.netty.serializer;

import com.alibaba.fastjson.JSON;
import com.ww.mall.netty.enums.SerializerTypeEnum;
import org.springframework.stereotype.Component;

/**
 * @author ww
 * @create 2024-05-06 22:40
 * @description:
 */
@Component
public class JsonSerializer implements MallSerializer {
    @Override
    public SerializerTypeEnum type() {
        return SerializerTypeEnum.JSON;
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
