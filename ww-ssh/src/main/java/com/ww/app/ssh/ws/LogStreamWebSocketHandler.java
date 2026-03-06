package com.ww.app.ssh.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ww.app.common.utils.IpUtil;
import com.ww.app.ssh.config.LogStreamHandshakeInterceptor;
import com.ww.app.ssh.model.LogStreamRequest;
import com.ww.app.ssh.model.LogTarget;
import com.ww.app.ssh.service.LogPanelQueryService;
import com.ww.app.ssh.service.SshLogService;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 日志流 WebSocket 处理器。
 * <p>
 * 每个 WebSocket 连接可订阅一个“会话上下文”，上下文内可并发聚合多个环境/服务日志流。
 * </p>
 */
@Component
public class LogStreamWebSocketHandler extends TextWebSocketHandler {

    /**
     * 日志组件。
     */
    private static final Logger log = LoggerFactory.getLogger(LogStreamWebSocketHandler.class);

    /**
     * 日志事件名称。
     */
    private static final String EVENT_WS_STREAM = "ws-stream";

    /**
     * 日志阶段：连接建立。
     */
    private static final String STAGE_CONNECTED = "connected";

    /**
     * 日志阶段：接收消息。
     */
    private static final String STAGE_RECEIVE = "receive";

    /**
     * 日志阶段：消息解析失败。
     */
    private static final String STAGE_PAYLOAD_PARSE_FAILED = "payload-parse-failed";

    /**
     * 日志阶段：订阅开始。
     */
    private static final String STAGE_SUBSCRIBE_START = "subscribe-start";

    /**
     * 日志阶段：订阅成功。
     */
    private static final String STAGE_SUBSCRIBE_SUCCESS = "subscribe-success";

    /**
     * 日志阶段：订阅失败。
     */
    private static final String STAGE_SUBSCRIBE_FAILED = "subscribe-failed";

    /**
     * 日志阶段：传输异常。
     */
    private static final String STAGE_TRANSPORT_ERROR = "transport-error";

    /**
     * 日志阶段：连接关闭。
     */
    private static final String STAGE_CLOSED = "closed";

    /**
     * JSON 序列化组件。
     */
    private final ObjectMapper objectMapper;

    /**
     * 面板配置查询服务。
     */
    private final LogPanelQueryService logPanelQueryService;

    /**
     * SSH 日志服务。
     */
    private final SshLogService sshLogService;

    /**
     * WebSocket 会话上下文缓存。
     */
    private final Map<String, StreamContext> streamContexts = new ConcurrentHashMap<>();

    /**
     * 构造方法。
     *
     * @param objectMapper         JSON 序列化组件
     * @param logPanelQueryService 面板配置查询服务
     * @param sshLogService        SSH 日志服务
     */
    public LogStreamWebSocketHandler(ObjectMapper objectMapper,
                                     LogPanelQueryService logPanelQueryService,
                                     SshLogService sshLogService) {
        this.objectMapper = objectMapper;
        this.logPanelQueryService = logPanelQueryService;
        this.sshLogService = sshLogService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        log.info("event={} stage={} sessionId={} ip={}",
                EVENT_WS_STREAM, STAGE_CONNECTED, session.getId(), resolveClientIp(session));
        sendSystemMessage(session, "连接已建立，请发送订阅参数");
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        String payload = message.getPayload();
        log.info("event={} stage={} sessionId={} ip={} payloadLength={}",
                EVENT_WS_STREAM, STAGE_RECEIVE, session.getId(), resolveClientIp(session), payload.length());
        LogStreamRequest request;
        try {
            request = objectMapper.readValue(payload, LogStreamRequest.class);
        } catch (Exception ex) {
            log.warn("event={} stage={} sessionId={} ip={} error={}",
                    EVENT_WS_STREAM, STAGE_PAYLOAD_PARSE_FAILED, session.getId(), resolveClientIp(session), ex.getMessage());
            sendSystemMessage(session, "请求解析失败: " + ex.getMessage());
            return;
        }
        try {
            restartStreams(session, request);
        } catch (Exception ex) {
            log.warn("event={} stage={} sessionId={} ip={} project={} env={} service={} mode={} error={}",
                    EVENT_WS_STREAM,
                    STAGE_SUBSCRIBE_FAILED,
                    session.getId(),
                    resolveClientIp(session),
                    request.normalizedProject(),
                    request.normalizedEnv(),
                    request.normalizedService(),
                    request.normalizedReadMode(),
                    ex.getMessage());
            sendSystemMessage(session, "启动订阅失败: " + ex.getMessage());
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, @NonNull Throwable exception) {
        log.warn("event={} stage={} sessionId={} ip={} error={}",
                EVENT_WS_STREAM,
                STAGE_TRANSPORT_ERROR,
                session.getId(),
                resolveClientIp(session),
                exception.getMessage());
        closeContext(session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        log.info("event={} stage={} sessionId={} ip={} code={} reason={}",
                EVENT_WS_STREAM,
                STAGE_CLOSED,
                session.getId(),
                resolveClientIp(session),
                status.getCode(),
                status.getReason());
        closeContext(session.getId());
    }

    /**
     * 重启会话的日志流订阅。
     *
     * @param session WebSocket 会话
     * @param request 订阅请求
     */
    private void restartStreams(WebSocketSession session, LogStreamRequest request) {
        closeContext(session.getId());
        String clientIp = resolveClientIp(session);
        log.info("event={} stage={} sessionId={} ip={} project={} env={} service={} mode={}",
                EVENT_WS_STREAM,
                STAGE_SUBSCRIBE_START,
                session.getId(),
                clientIp,
                request.normalizedProject(),
                request.normalizedEnv(),
                request.normalizedService(),
                request.normalizedReadMode());
        List<LogTarget> targets = logPanelQueryService.resolveTargets(request);
        List<SshLogService.StreamHandle> handles = new ArrayList<>();
        for (LogTarget target : targets) {
            try {
                SshLogService.StreamHandle handle = sshLogService.startStreaming(target, request, line ->
                        sendLogLine(session, line));
                handles.add(handle);
                sendSystemMessage(session, "已连接 " + target.displayName() + " -> " + handle.getFilePath());
            } catch (Exception ex) {
                sendSystemMessage(session, "连接失败 " + target.displayName() + ": " + ex.getMessage());
            }
        }
        if (handles.isEmpty()) {
            throw new IllegalStateException("没有可用日志流，请检查环境/服务或连接配置");
        }
        streamContexts.put(session.getId(), new StreamContext(handles));
        log.info("event={} stage={} sessionId={} ip={} streamCount={}",
                EVENT_WS_STREAM, STAGE_SUBSCRIBE_SUCCESS, session.getId(), clientIp, handles.size());
        sendSystemMessage(session, "已启动 " + handles.size() + " 个日志流");
    }

    /**
     * 发送业务日志行（原文透传，不附加环境/服务前缀）。
     *
     * @param session WebSocket 会话
     * @param content 日志内容
     */
    private void sendLogLine(WebSocketSession session, String content) {
        sendText(session, content);
    }

    /**
     * 发送系统提示消息。
     *
     * @param session WebSocket 会话
     * @param content 提示内容
     */
    private void sendSystemMessage(WebSocketSession session, String content) {
        sendText(session, "[系统提示] " + content);
    }

    /**
     * 安全发送文本消息。
     *
     * @param session WebSocket 会话
     * @param content 文本内容
     */
    private void sendText(WebSocketSession session, String content) {
        if (session == null || !session.isOpen()) {
            return;
        }
        synchronized (session) {
            if (!session.isOpen()) {
                return;
            }
            try {
                session.sendMessage(new TextMessage(content + "\n"));
            } catch (IOException ignored) {
                // ignore
            }
        }
    }

    /**
     * 关闭并清理会话上下文。
     *
     * @param sessionId 会话 ID
     */
    private void closeContext(String sessionId) {
        StreamContext context = streamContexts.remove(sessionId);
        if (context != null) {
            context.closeAll();
        }
    }

    /**
     * 解析 WebSocket 会话客户端 IP。
     *
     * @param session WebSocket 会话
     * @return 客户端 IP
     */
    private String resolveClientIp(WebSocketSession session) {
        if (session == null) {
            return IpUtil.UNKNOWN;
        } else {
            session.getAttributes();
        }
        Object value = session.getAttributes().get(LogStreamHandshakeInterceptor.ATTR_CLIENT_IP);
        if (!(value instanceof String)) {
            return IpUtil.UNKNOWN;
        }
        String ip = ((String) value).trim();
        return ip.isEmpty() ? IpUtil.UNKNOWN : ip;
    }

    /**
     * WebSocket 会话流上下文。
     */
    private static class StreamContext {

        /**
         * 当前会话内所有活跃日志流句柄。
         */
        private final List<SshLogService.StreamHandle> handles;

        /**
         * 构造方法。
         *
         * @param handles 流句柄列表
         */
        private StreamContext(List<SshLogService.StreamHandle> handles) {
            this.handles = handles;
        }

        /**
         * 关闭全部流句柄。
         */
        private void closeAll() {
            for (SshLogService.StreamHandle handle : handles) {
                try {
                    handle.close();
                } catch (Exception ignored) {
                    // ignore
                }
            }
        }
    }
}
