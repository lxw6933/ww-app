package com.ww.mall.utils;

import com.alibaba.fastjson.JSON;
import com.ww.mall.common.common.MallAdminUser;
import com.ww.mall.common.common.MallClientUser;
import com.ww.mall.common.constant.Constant;
import com.ww.mall.common.enums.GlobalResCodeConstants;
import com.ww.mall.common.exception.ApiException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

/**
 * @author ww
 * @create 2023-07-19- 15:58
 * @description:
 */
@Slf4j
public class AuthorizationContext {

    private AuthorizationContext() {}

    private static final ThreadLocal<MallClientUser> CLIENT_USER_THREAD_LOCAL = new ThreadLocal<>();
    private static final ThreadLocal<MallAdminUser> ADMIN_USER_THREAD_LOCAL = new ThreadLocal<>();

    public static MallClientUser getClientUser() {
        return getClientUser(true);
    }

    public static MallClientUser getClientUser(boolean ex) {
        MallClientUser mallClientUser = CLIENT_USER_THREAD_LOCAL.get();
        // 获取当前线程是否有用户信息
        if (mallClientUser != null) {
            return mallClientUser;
        }
        return getUserTokenInfo(ex, MallClientUser.class);
    }

    public static MallAdminUser getAdminUser() {
        return getAdminUser(true);
    }

    public static MallAdminUser getAdminUser(boolean ex) {
        MallAdminUser mallAdminUser = ADMIN_USER_THREAD_LOCAL.get();
        // 获取当前线程是否有用户信息
        if (mallAdminUser != null) {
            return mallAdminUser;
        }
        return getUserTokenInfo(ex, MallAdminUser.class);
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
        return JSON.parseObject(tokenInfo, tClass);
    }

    public static void remove() {
        CLIENT_USER_THREAD_LOCAL.remove();
        ADMIN_USER_THREAD_LOCAL.remove();
    }

}
