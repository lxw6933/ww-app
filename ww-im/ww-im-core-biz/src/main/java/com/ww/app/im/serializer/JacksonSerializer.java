package com.ww.app.im.serializer;

import com.ww.app.common.utils.json.JacksonUtils;
import com.ww.app.im.core.api.enums.ImMsgSerializerEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author ww
 * @create 2024-05-06 22:40
 * @description:
 */
@Slf4j
@Component
public class JacksonSerializer implements ImMsgSerializer {

    @Override
    public ImMsgSerializerEnum type() {
        return ImMsgSerializerEnum.JACKSON;
    }

    @Override
    public <T> byte[] serialize(T object) {
        return JacksonUtils.toJsonByte(object);
    }

    @Override
    public <T> T deserialize(Class<T> clazz, byte[] bytes) {
        return JacksonUtils.parseObject(bytes, clazz);
    }
}
