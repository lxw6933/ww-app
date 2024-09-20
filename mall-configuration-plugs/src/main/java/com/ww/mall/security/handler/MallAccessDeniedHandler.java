package com.ww.mall.security.handler;

import com.ww.mall.common.common.Result;
import com.ww.mall.utils.HttpContextUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static com.ww.mall.common.enums.GlobalResCodeConstants.UNAUTHORIZED;

/**
 * @author ww
 * @create 2024-09-20- 17:18
 * @description: 授权失败处理器
 */
@Slf4j
@Component
public class MallAccessDeniedHandler implements AccessDeniedHandler {
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException, ServletException {
        HttpContextUtils.write(response, Result.error(UNAUTHORIZED));
    }
}
