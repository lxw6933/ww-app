package com.ww.mall.web.interceptor;

import cn.hutool.core.util.IdUtil;
import com.ww.mall.common.constant.Constant;
import org.slf4j.MDC;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @description: 日志拦截器
 * @author: ww
 * @create: 2023/7/8 10:51
 **/
public class LogInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 如果有上层调用就用上层的ID
        String traceId = request.getHeader(Constant.TRACE_ID);
        if (traceId == null) {
            traceId = IdUtil.objectId();
        }
        MDC.put(Constant.TRACE_ID, traceId);
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView)
            throws Exception {
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex)
            throws Exception {
        // 调用结束后删除
        MDC.remove(Constant.TRACE_ID);
    }
}
