package com.ww.mall.web.utils;

import com.alibaba.fastjson.JSON;
import com.ww.mall.common.common.MallClientUser;
import com.ww.mall.common.constant.Constant;
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

    private static final ThreadLocal<MallClientUser> CLIENT_USER_THREAD_LOCAL = new ThreadLocal<>();

    public static MallClientUser getClientUser() {
        MallClientUser mallClientUser = CLIENT_USER_THREAD_LOCAL.get();
        // 获取当前线程是否有用户信息
        if (mallClientUser != null) {
            return mallClientUser;
        }
        // 获取当前线程是否带有token
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder
                .getRequestAttributes();
        if (attributes == null) {
            return null;
        }
        HttpServletRequest request = attributes.getRequest();
        String tokenInfo = request.getHeader(Constant.USER_TOKEN_INFO);
        if (StringUtils.isEmpty(tokenInfo)) {
            return null;
        }
        return JSON.parseObject(tokenInfo, MallClientUser.class);
    }

    public void remove() {
        CLIENT_USER_THREAD_LOCAL.remove();
    }

}
