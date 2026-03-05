package com.ww.app.ssh.config;

import com.ww.app.common.utils.IpUtil;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * 日志流 WebSocket 握手日志拦截器。
 * <p>
 * 在握手阶段统一提取客户端 IP，并输出结构化握手行为日志。
 * </p>
 */
public class LogStreamHandshakeInterceptor implements HandshakeInterceptor {

    /**
     * 日志组件。
     */
    private static final Logger log = LoggerFactory.getLogger(LogStreamHandshakeInterceptor.class);

    /**
     * 日志事件名称。
     */
    private static final String EVENT_WS_HANDSHAKE = "ws-handshake";

    /**
     * 握手日志阶段：开始。
     */
    private static final String STAGE_START = "start";

    /**
     * 握手日志阶段：完成。
     */
    private static final String STAGE_COMPLETED = "completed";

    /**
     * 握手日志阶段：失败。
     */
    private static final String STAGE_FAILED = "failed";

    /**
     * WebSocket 会话属性中的客户端 IP 键。
     */
    public static final String ATTR_CLIENT_IP = "wwSsh.ws.clientIp";

    @Override
    public boolean beforeHandshake(@NonNull ServerHttpRequest request,
                                   @NonNull ServerHttpResponse response,
                                   @NonNull WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) {
        String clientIp = resolveClientIp(request);
        attributes.put(ATTR_CLIENT_IP, clientIp);
        log.info("event={} stage={} ip={} path={}",
                EVENT_WS_HANDSHAKE, STAGE_START, clientIp, request.getURI().getPath());
        return true;
    }

    @Override
    public void afterHandshake(@NonNull ServerHttpRequest request,
                               @NonNull ServerHttpResponse response,
                               @NonNull WebSocketHandler wsHandler,
                               Exception exception) {
        String clientIp = resolveClientIp(request);
        int status = resolveStatus(response);
        if (exception == null) {
            log.info("event={} stage={} ip={} path={} status={}",
                    EVENT_WS_HANDSHAKE, STAGE_COMPLETED, clientIp, request.getURI().getPath(), status);
            return;
        }
        log.warn("event={} stage={} ip={} path={} status={} error={}",
                EVENT_WS_HANDSHAKE, STAGE_FAILED, clientIp, request.getURI().getPath(), status, exception.getMessage());
    }

    /**
     * 解析握手响应状态码。
     *
     * @param response 握手响应
     * @return HTTP 状态码；无法获取时返回 0
     */
    private int resolveStatus(ServerHttpResponse response) {
        if (response instanceof ServletServerHttpResponse) {
            return ((ServletServerHttpResponse) response).getServletResponse().getStatus();
        }
        return 0;
    }

    /**
     * 解析握手请求来源 IP。
     * <p>
     * 优先复用 ww-common 的 {@link IpUtil}，在非 Servlet 场景回退到 remoteAddress。
     * </p>
     *
     * @param request 握手请求
     * @return 客户端 IP
     */
    private String resolveClientIp(ServerHttpRequest request) {
        if (request instanceof ServletServerHttpRequest) {
            return IpUtil.getRealIp(((ServletServerHttpRequest) request).getServletRequest());
        }
        if (request == null) {
            return IpUtil.UNKNOWN;
        } else {
            request.getRemoteAddress();
        }
        if (request.getRemoteAddress().getAddress() == null) {
            String host = request.getRemoteAddress().getHostString();
            return host == null || host.trim().isEmpty() ? IpUtil.UNKNOWN : host;
        }
        String ip = request.getRemoteAddress().getAddress().getHostAddress();
        return ip == null || ip.trim().isEmpty() ? IpUtil.UNKNOWN : ip;
    }
}
