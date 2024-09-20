package com.ww.mall.security.handler;

import com.ww.mall.common.common.Result;
import com.ww.mall.utils.HttpContextUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static com.ww.mall.common.enums.GlobalResCodeConstants.FORBIDDEN;

/**
 * @author ww
 * @create 2024-09-20- 17:19
 * @description: 认证失败处理器
 */
@Slf4j
@Component
public class MallAuthenticationEntryPoint implements AuthenticationEntryPoint {
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
        HttpContextUtils.write(response, Result.error(FORBIDDEN));
    }
}
