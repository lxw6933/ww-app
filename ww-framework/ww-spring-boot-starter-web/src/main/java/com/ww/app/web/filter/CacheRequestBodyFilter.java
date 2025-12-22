package com.ww.app.web.filter;

import cn.hutool.core.util.StrUtil;
import com.ww.app.common.enums.WebFilterOrderEnum;
import com.ww.app.common.utils.HttpContextUtils;
import com.ww.app.web.config.request.CachedBodyHttpServletRequest;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author ww
 * @create 2025-12-22 13:46
 * @description: Request Body 缓存 Filter，实现它的可重复读取
 */
@Component
@Order(WebFilterOrderEnum.REQUEST_BODY_CACHE_FILTER)
public class CacheRequestBodyFilter extends OncePerRequestFilter {

    /**
     * 需要排除的 URI
     */
    private static final String[] IGNORE_URIS = {"/admin/", "/actuator/"};

    @Override
    protected void doFilterInternal(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        filterChain.doFilter(new CachedBodyHttpServletRequest(request), response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // 1. 校验是否为排除的 URL
        String requestURI = request.getRequestURI();
        if (StrUtil.startWithAny(requestURI, IGNORE_URIS)) {
            return true;
        }

        // 2. 只处理 json 请求内容
        return !HttpContextUtils.isJsonRequest(request);
    }

}
