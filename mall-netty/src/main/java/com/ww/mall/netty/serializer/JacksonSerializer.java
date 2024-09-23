package com.ww.mall.netty.serializer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ww.mall.common.enums.GlobalResCodeConstants;
import com.ww.mall.common.exception.ApiException;
import com.ww.mall.netty.enums.SerializerTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * @author ww
 * @create 2024-05-06 22:40
 * @description:
 */
@Slf4j
@Component
public class JacksonSerializer implements MallSerializer {
    private static final ObjectMapper mapper = new ObjectMapper();

    @Override
    public SerializerTypeEnum type() {
        return SerializerTypeEnum.JACKSON;
    }

    @Override
    public <T> byte[] serialize(T object) {
        try {
            return mapper.writeValueAsBytes(object);
        } catch (JsonProcessingException e) {
            log.error("{} 序列化异常：", object, e);
            throw new ApiException(GlobalResCodeConstants.SYSTEM_ERROR);
        }
    }

    @Override
    public <T> T deserialize(Class<T> clazz, byte[] bytes) {
        try {
            return mapper.readValue(bytes, clazz);
        } catch (IOException e) {
            log.error("[{}][{}]反序列化异常：", clazz, bytes, e);
            throw new ApiException(GlobalResCodeConstants.SYSTEM_ERROR);
        }
    }
}
