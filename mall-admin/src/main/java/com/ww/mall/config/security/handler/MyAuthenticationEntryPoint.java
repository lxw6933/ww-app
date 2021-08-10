package com.ww.mall.config.security.handler;

import com.ww.mall.common.common.R;
import com.ww.mall.utils.HttpContextUtils;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @description: 认证异常处理器
 * @author: ww
 * @create: 2021/6/26 上午9:07
 **/
@Component
public class MyAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
        R r;
        if (authException instanceof InsufficientAuthenticationException) {
            r = R.error("请先登录");
        } else if (authException instanceof BadCredentialsException) {
            r = R.error("密码错误");
        } else if (authException instanceof InternalAuthenticationServiceException) {
            r = R.error("账号错误");
        } else {
            authException.printStackTrace();
            r = R.error(authException.getMessage());
        }
        HttpContextUtils.backJson(response, r);
    }
}
