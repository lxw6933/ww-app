package com.ww.app.common.utils;

import com.ww.app.common.annotation.Sensitive;
import com.ww.app.common.context.AuthorizationContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Field;
import java.util.List;

/**
 * @author ww
 * @create 2024-12-27- 10:04
 * @description:
 */
@Slf4j
public class SensitiveUtils {

    private SensitiveUtils() {}

    public static boolean hasPermission(String requiredPermission) {
        if (StringUtils.isBlank(requiredPermission)) {
            return false;
        }
        List<String> adminUserSensitivePerms = AuthorizationContext.getAdminUserSensitivePerms();
        // 判断用户是否拥有权限
        return adminUserSensitivePerms.contains(requiredPermission);
    }

    public static void handleSensitiveData(Object obj) {
        for (Field field : obj.getClass().getDeclaredFields()) {
            if (field.isAnnotationPresent(Sensitive.class)) {
                field.setAccessible(true);
                try {
                    Object sensitiveFieldValue = field.get(obj);
                    if (sensitiveFieldValue != null) {
                        Sensitive sensitive = field.getAnnotation(Sensitive.class);
                        if (!hasPermission(sensitive.permission())) {
                            field.set(obj, sensitive.type().desensitizer.apply(sensitiveFieldValue.toString()));
                        }
                    }
                } catch (IllegalAccessException e) {
                    log.error("数据访问异常", e);
                }
            }
        }
    }

}
