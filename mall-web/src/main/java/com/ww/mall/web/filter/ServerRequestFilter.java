package com.ww.mall.web.filter;

import cn.hutool.core.util.IdUtil;
import com.ww.mall.common.constant.Constant;
import com.ww.mall.common.thread.ThreadMdcUtil;
import com.ww.mall.common.utils.AuthorizationContext;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author ww
 * @create 2023-07-26- 11:04
 * @description: 服务请求过滤器
 */
@Slf4j
@Component
public class ServerRequestFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain) throws ServletException, IOException {
        try {
            // 如果有上层调用就用上层的ID
            String traceId = request.getHeader(Constant.TRACE_ID);
            if (traceId == null) {
                traceId = IdUtil.objectId();
            }
            ThreadMdcUtil.setTraceId(traceId);
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(Constant.TRACE_ID);
            AuthorizationContext.remove();
        }
    }
}
