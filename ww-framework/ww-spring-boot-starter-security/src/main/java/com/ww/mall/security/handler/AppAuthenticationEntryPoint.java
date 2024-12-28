package com.ww.mall.security.handler;

import com.ww.mall.common.common.Result;
import com.ww.mall.common.utils.HttpContextUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static com.ww.mall.common.enums.GlobalResCodeConstants.UNAUTHORIZED;

/**
 * @author ww
 * @create 2024-09-20- 17:19
 * @description: 认证失败处理器
 */
@Slf4j
public class AppAuthenticationEntryPoint implements AuthenticationEntryPoint {
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) {
        log.error("访问 URL【{}】，没有登录认证", request.getRequestURI());
        HttpContextUtils.write(response, Result.error(UNAUTHORIZED));
    }
}
