package com.ww.app.ssh.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ww.app.ssh.model.LogStreamRequest;
import com.ww.app.ssh.model.LogTarget;
import com.ww.app.ssh.service.LogPanelQueryService;
import com.ww.app.ssh.service.SshLogService;
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
        sendSystemMessage(session, "连接已建立，请发送订阅参数");
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        LogStreamRequest request;
        try {
            request = objectMapper.readValue(message.getPayload(), LogStreamRequest.class);
        } catch (Exception ex) {
            sendSystemMessage(session, "请求解析失败: " + ex.getMessage());
            return;
        }
        try {
            restartStreams(session, request);
        } catch (Exception ex) {
            sendSystemMessage(session, "启动订阅失败: " + ex.getMessage());
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        closeContext(session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
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
