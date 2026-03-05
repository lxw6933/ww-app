package com.ww.app.ssh.config;

import com.ww.app.common.utils.IpUtil;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * ww-ssh HTTP 接口行为日志拦截器。
 * <p>
 * 以“请求 IP + 接口路径 + 执行结果”记录访问轨迹，
 * 用于快速定位谁在何时调用了哪些运维/日志查询接口。
 * </p>
 */
public class ApiAccessLogInterceptor implements HandlerInterceptor {

    /**
     * 日志组件。
     */
    private static final Logger log = LoggerFactory.getLogger(ApiAccessLogInterceptor.class);

    /**
     * 日志事件名称。
     */
    private static final String EVENT_API_ACCESS = "api-access";

    /**
     * 日志阶段：请求完成。
     */
    private static final String STAGE_COMPLETED = "completed";

    /**
     * 日志阶段：请求异常。
     */
    private static final String STAGE_FAILED = "failed";

    /**
     * 请求起始时间属性键。
     */
    private static final String ATTR_START_AT = "wwSsh.access.startAt";

    /**
     * 请求来源 IP 属性键。
     */
    private static final String ATTR_CLIENT_IP = "wwSsh.access.clientIp";

    /**
     * URL 查询串最大日志长度。
     */
    private static final int QUERY_MAX_LEN = 256;

    /**
     * User-Agent 最大日志长度。
     */
    private static final int UA_MAX_LEN = 160;

    /**
     * 错误消息最大日志长度。
     */
    private static final int ERROR_MAX_LEN = 180;

    @Override
    public boolean preHandle(HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler) {
        request.setAttribute(ATTR_START_AT, System.currentTimeMillis());
        request.setAttribute(ATTR_CLIENT_IP, IpUtil.getRealIp(request));
        return true;
    }

    @Override
    public void afterCompletion(@NonNull HttpServletRequest request,
                                HttpServletResponse response,
                                @NonNull Object handler,
                                Exception ex) {
        long startAt = readStartAt(request);
        long duration = Math.max(0L, System.currentTimeMillis() - startAt);
        String clientIp = readClientIp(request);
        String method = safe(request.getMethod());
        String uri = safe(request.getRequestURI());
        String query = cutToMax(safe(request.getQueryString()), QUERY_MAX_LEN);
        int status = response.getStatus();
        String ua = cutToMax(safe(request.getHeader("User-Agent")), UA_MAX_LEN);
        if (ex == null) {
            log.info("event={} stage={} ip={} method={} uri={} status={} costMs={} query={} ua={}",
                    EVENT_API_ACCESS, STAGE_COMPLETED, clientIp, method, uri, status, duration, query, ua);
        } else {
            log.warn("event={} stage={} ip={} method={} uri={} status={} costMs={} query={} error={}",
                    EVENT_API_ACCESS, STAGE_FAILED, clientIp, method, uri, status, duration, query,
                    cutToMax(safe(ex.getMessage()), ERROR_MAX_LEN));
        }
    }

    /**
     * 读取请求起始时间。
     *
     * @param request HTTP 请求
     * @return 起始时间戳
     */
    private long readStartAt(HttpServletRequest request) {
        Object value = request.getAttribute(ATTR_START_AT);
        if (value instanceof Long) {
            return (Long) value;
        }
        return System.currentTimeMillis();
    }

    /**
     * 读取来源 IP。
     *
     * @param request HTTP 请求
     * @return 客户端 IP
     */
    private String readClientIp(HttpServletRequest request) {
        Object value = request.getAttribute(ATTR_CLIENT_IP);
        if (value instanceof String && !((String) value).trim().isEmpty()) {
            return (String) value;
        }
        return IpUtil.getRealIp(request);
    }

    /**
     * 空值兜底。
     *
     * @param value 原始文本
     * @return 非 null 文本
     */
    private String safe(String value) {
        return value == null ? "" : value;
    }

    /**
     * 日志字段最大长度裁剪。
     *
     * @param text      原始文本
     * @param maxLength 最大长度
     * @return 裁剪结果
     */
    private String cutToMax(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text == null ? "" : text;
        }
        return text.substring(0, maxLength);
    }
}
