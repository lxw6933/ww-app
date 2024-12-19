package com.ww.mall.common.serializer;

import cn.hutool.core.collection.CollectionUtil;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;
import com.ww.mall.common.annotation.Sensitive;
import com.ww.mall.common.enums.SensitiveDataType;
import com.ww.mall.common.utils.AuthorizationContext;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

/**
 * @author ww
 * @create 2024-04-26- 10:50
 * @description:
 */
@Getter
@Setter
public class SensitiveJsonSerializer extends JsonSerializer<String> implements ContextualSerializer {

    private SensitiveDataType sensitiveDataType;

    private String requiredPermission;

    @Override
    public JsonSerializer<?> createContextual(SerializerProvider prov, BeanProperty property) {
        // 获取bean属性上的@Sensitive注解
        Sensitive annotation = property.getAnnotation(Sensitive.class);
        if (Objects.nonNull(annotation) && Objects.equals(String.class, property.getType().getRawClass())) {
            SensitiveJsonSerializer serializer = new SensitiveJsonSerializer();
            serializer.setSensitiveDataType(annotation.type());
            serializer.setRequiredPermission(annotation.permission());
            return serializer;
        }
        return this;
    }

    @Override
    public void serialize(String value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        // 判断用户是否有权限【没有权限，敏感序列化，有权限正常序列化】
        if (hasPermission(requiredPermission)) {
            gen.writeObject(value);
        } else {
            // 序列化敏感数据字段
            gen.writeString(sensitiveDataType.desensitizer.apply(value));
        }
    }

    private boolean hasPermission(String requiredPermission) {
        if (StringUtils.isBlank(requiredPermission)) {
            return false;
        }
        List<String> adminUserSensitivePerms = AuthorizationContext.getAdminUserSensitivePerms();
        // 判断用户是否拥有权限
        return CollectionUtil.isNotEmpty(adminUserSensitivePerms) && adminUserSensitivePerms.contains(requiredPermission);
    }

}
