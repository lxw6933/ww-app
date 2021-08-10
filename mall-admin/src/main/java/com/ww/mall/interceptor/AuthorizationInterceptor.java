package com.ww.mall.interceptor;

import com.ww.mall.common.constant.TokenKeyConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @description: 权限(Token)验证拦截
 * 在拦截器哪里直接根据token查找用户信息，弊端很大，如果遇到网络IO就知道了，特别是高并发场景下（不推荐）
 * @author: ww
 * @create: 2021-05-18 14:37
 */
@Slf4j
@Component
@Deprecated
public class AuthorizationInterceptor extends HandlerInterceptorAdapter {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        log.info("权限(Token)验证拦截");
        String token = request.getHeader(TokenKeyConstant.APP_KEY);
        // TODO: 2021/5/18  解析token 获取用户信息
        // TODO: 2021/5/18 将用户信息放入request
        return true;
    }

}
