package com.ww.mall.common.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;
import com.ww.mall.common.annotation.Sensitive;
import com.ww.mall.common.enums.SensitiveDataType;

import java.io.IOException;
import java.util.Objects;

/**
 * @author ww
 * @create 2024-04-26- 10:50
 * @description:
 */
public class SensitiveJsonSerializer extends JsonSerializer<String> implements ContextualSerializer {

    private SensitiveDataType sensitiveDataType;

    @Override
    public JsonSerializer<?> createContextual(SerializerProvider prov, BeanProperty property) throws JsonMappingException {
        // 获取bean属性上的@Sensitive注解
        Sensitive annotation = property.getAnnotation(Sensitive.class);
        if (Objects.nonNull(annotation) && Objects.equals(String.class, property.getType().getRawClass())) {
            // 获取数据敏感类型
            this.sensitiveDataType = annotation.type();
            // 返回敏感数据序列化器
            return this;
        }
        return prov.findValueSerializer(property.getType(), property);
    }

    @Override
    public void serialize(String value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        // 序列化敏感数据字段
        gen.writeString(sensitiveDataType.desensitizer.apply(value));
    }

}
