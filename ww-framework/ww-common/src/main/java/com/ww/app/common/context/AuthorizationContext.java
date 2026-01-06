package com.ww.app.common.context;

import com.alibaba.fastjson.JSON;
import com.alibaba.ttl.TransmittableThreadLocal;
import com.ww.app.common.common.AdminUser;
import com.ww.app.common.common.ClientUser;
import com.ww.app.common.constant.Constant;
import com.ww.app.common.enums.GlobalResCodeConstants;
import com.ww.app.common.exception.ApiException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

/**
 * @author ww
 * @create 2023-07-19- 15:58
 * @description:
 */
@Slf4j
public class AuthorizationContext {

    private AuthorizationContext() {}

    private static final TransmittableThreadLocal<ClientUser> CLIENT_USER_THREAD_LOCAL = new TransmittableThreadLocal<>();
    private static final TransmittableThreadLocal<AdminUser> ADMIN_USER_THREAD_LOCAL = new TransmittableThreadLocal<>();
    private static final TransmittableThreadLocal<List<String>> ADMIN_USER_SENSITIVE_PERMS_THREAD_LOCAL = TransmittableThreadLocal.withInitial(ArrayList::new);

    public static void setClientUser(ClientUser clientUser) {
        CLIENT_USER_THREAD_LOCAL.set(clientUser);
    }

    public static void setAdminUser(AdminUser adminUser) {
        ADMIN_USER_THREAD_LOCAL.set(adminUser);
    }

    public static void setAdminUserSensitivePermissions(List<String> permissions) {
        ADMIN_USER_SENSITIVE_PERMS_THREAD_LOCAL.set(permissions);
    }

    public static ClientUser getClientUser() {
        return getClientUser(true);
    }

    public static ClientUser getClientUser(boolean ex) {
        ClientUser clientUser = CLIENT_USER_THREAD_LOCAL.get();
        // 获取当前线程是否有用户信息
        if (clientUser != null) {
            return clientUser;
        }
        
        // 从 Header 解析用户信息
        clientUser = getUserTokenInfo(ex, ClientUser.class);
        
        // 解析成功后缓存到 ThreadLocal
        if (clientUser != null) {
            setClientUser(clientUser);
            log.debug("ClientUser 已缓存到 ThreadLocal: userId={}", clientUser.getId());
        }
        
        return clientUser;
    }

    public static AdminUser getAdminUser() {
        return getAdminUser(true);
    }

    public static AdminUser getAdminUser(boolean ex) {
        AdminUser adminUser = ADMIN_USER_THREAD_LOCAL.get();
        // 获取当前线程是否有用户信息
        if (adminUser != null) {
            return adminUser;
        }
        
        // 从 Header 解析用户信息
        adminUser = getUserTokenInfo(ex, AdminUser.class);
        
        // 解析成功后缓存到 ThreadLocal
        if (adminUser != null) {
            setAdminUser(adminUser);
            log.debug("AdminUser 已缓存到 ThreadLocal: userId={}", adminUser.getId());
        }
        
        return adminUser;
    }

    private static <T> T getUserTokenInfo(boolean ex, Class<T> tClass) {
        // 获取当前线程是否带有token
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder
                .getRequestAttributes();
        if (attributes == null) {
            if (ex) {
                throw new ApiException(GlobalResCodeConstants.UNAUTHORIZED);
            } else {
                return null;
            }
        }
        HttpServletRequest request = attributes.getRequest();
        String tokenInfo = request.getHeader(Constant.USER_TOKEN_INFO);
        if (StringUtils.isEmpty(tokenInfo)) {
            if (ex) {
                throw new ApiException(GlobalResCodeConstants.UNAUTHORIZED);
            } else {
                return null;
            }
        }
        
        try {
            T user = JSON.parseObject(tokenInfo, tClass);
            log.debug("从 Header 解析用户信息成功: type={}", tClass.getSimpleName());
            return user;
        } catch (Exception e) {
            log.error("解析用户信息失败: tokenInfo={}, type={}, error={}", 
                    tokenInfo, tClass.getSimpleName(), e.getMessage());
            if (ex) {
                throw new ApiException(GlobalResCodeConstants.UNAUTHORIZED);
            }
            return null;
        }
    }

    public static List<String> getAdminUserSensitivePerms() {
        return ADMIN_USER_SENSITIVE_PERMS_THREAD_LOCAL.get();
    }

    public static void clear() {
        CLIENT_USER_THREAD_LOCAL.remove();
        ADMIN_USER_THREAD_LOCAL.remove();
        ADMIN_USER_SENSITIVE_PERMS_THREAD_LOCAL.remove();
    }

}
