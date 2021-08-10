package com.ww.mall.config.security.handler;

import com.ww.mall.common.common.R;
import com.ww.mall.utils.HttpContextUtils;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @description: 无权限处理器
 * @author: ww
 * @create: 2021/6/26 上午9:12
 **/
@Component
public class MyAccessDeniedHandler implements AccessDeniedHandler {
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException e) throws IOException, ServletException {
        HttpContextUtils.backJson(response, R.error("暂无无权限！"));
    }
}
