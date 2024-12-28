package com.ww.mall.security.handler;

import com.ww.mall.common.common.BaseUser;
import com.ww.mall.common.common.Result;
import com.ww.mall.common.utils.HttpContextUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.AccessDeniedHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static com.ww.mall.common.enums.GlobalResCodeConstants.FORBIDDEN;

/**
 * @author ww
 * @create 2024-09-20- 17:18
 * @description: 授权失败处理器
 */
@Slf4j
public class AppAccessDeniedHandler implements AccessDeniedHandler {
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) {
        BaseUser user = (BaseUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        log.error("访问 URL【{}】，[{}]用户[{}]没有权限", request.getRequestURI(), user.getUserType(), user.getId());
        HttpContextUtils.write(response, Result.error(FORBIDDEN));
    }
}
