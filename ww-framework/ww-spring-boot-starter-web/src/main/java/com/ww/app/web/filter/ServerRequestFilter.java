package com.ww.app.web.filter;

import com.ww.app.common.context.AuthorizationContext;
import com.ww.app.web.holder.ServerIpContextHolder;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
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

            filterChain.doFilter(request, response);
        } finally {
            AuthorizationContext.clear();
            ServerIpContextHolder.clear();
        }
    }
}
