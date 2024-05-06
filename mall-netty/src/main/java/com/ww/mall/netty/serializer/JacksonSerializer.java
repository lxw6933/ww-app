package com.ww.mall.netty.serializer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ww.mall.common.exception.ApiException;
import com.ww.mall.netty.enums.SerializerTypeEnum;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * @author ww
 * @create 2024-05-06 22:40
 * @description:
 */
@Component
public class JacksonSerializer implements MallSerializer {
    ObjectMapper mapper = new ObjectMapper();

    @Override
    public SerializerTypeEnum type() {
        return SerializerTypeEnum.JACKSON;
    }

    @Override
    public <T> byte[] serialize(T object) {
        try {
            return mapper.writeValueAsBytes(object);
        } catch (JsonProcessingException e) {
            throw new ApiException(e);
        }
    }

    @Override
    public <T> T deserialize(Class<T> clazz, byte[] bytes) {
        try {
            return mapper.readValue(bytes, clazz);
        } catch (IOException e) {
            throw new ApiException(e);
        }
    }
}
