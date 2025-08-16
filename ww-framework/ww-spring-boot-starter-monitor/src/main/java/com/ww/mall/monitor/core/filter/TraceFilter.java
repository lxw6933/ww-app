package com.ww.mall.monitor.core.filter;

import com.ww.app.common.utils.TracerUtils;
import lombok.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author ww
 * @create 2025-08-14 22:44
 * @description: 将 TraceContext.traceId() 回写到响应头，便于前端/客服定位问题
 */
public class TraceFilter extends OncePerRequestFilter {

    private static final String HEADER_NAME_TRACE_ID = "X-Trace-Id";

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        // 设置响应 traceId
        response.addHeader(HEADER_NAME_TRACE_ID, TracerUtils.getTraceId());
        // 继续过滤
        chain.doFilter(request, response);
    }

}
