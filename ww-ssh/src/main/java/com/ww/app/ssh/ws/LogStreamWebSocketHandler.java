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

import javax.annotation.PreDestroy;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

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
     * 日志阶段：发送失败。
     */
    private static final String STAGE_SEND_FAILED = "send-failed";

    /**
     * 日志阶段：传输异常。
     */
    private static final String STAGE_TRANSPORT_ERROR = "transport-error";

    /**
     * 日志阶段：连接关闭。
     */
    private static final String STAGE_CLOSED = "closed";

    /**
     * WebSocket 发送缓冲刷新间隔（毫秒）。
     */
    private static final long SESSION_SEND_FLUSH_MS = 60L;

    /**
     * 单次批量发送阈值（字节）。
     */
    private static final int SESSION_SEND_BATCH_BYTES = 32 * 1024;

    /**
     * 单会话发送缓冲区上限（字节）。
     */
    private static final int SESSION_SEND_BUFFER_LIMIT_BYTES = 256 * 1024;

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
     * WebSocket 批量发送调度器。
     */
    private final ScheduledExecutorService sendExecutor;

    /**
     * 发送线程编号序列。
     */
    private final AtomicInteger sendThreadSequence = new AtomicInteger(1);

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
        this.sendExecutor = Executors.newScheduledThreadPool(4, runnable -> {
            Thread thread = new Thread(runnable, "ww-ssh-ws-send-" + sendThreadSequence.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        });
    }

    /**
     * 应用关闭前释放发送调度线程池。
     */
    @PreDestroy
    public void shutdown() {
        sendExecutor.shutdownNow();
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        getOrCreateContext(session.getId());
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
        validateFilePathPolicy(request);
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
        StreamContext context = new StreamContext();
        streamContexts.put(session.getId(), context);
        for (LogTarget target : targets) {
            try {
                SshLogService.StreamHandle handle = sshLogService.startStreaming(
                        target,
                        request,
                        clientIp,
                        session.getId(),
                        line -> {
                    if (!sendLogLine(session, line)) {
                        throw new SessionClosedException("WebSocket连接已不可用");
                    }
                });
                context.addHandle(handle);
                if (!sendSystemMessage(session, "已连接 " + target.displayName() + " -> " + handle.getFilePath())) {
                    throw new SessionClosedException("WebSocket连接已不可用");
                }
            } catch (SessionClosedException ex) {
                closeContext(session.getId());
                throw new IllegalStateException(ex.getMessage(), ex);
            } catch (Exception ex) {
                if (!session.isOpen()) {
                    closeContext(session.getId());
                    throw new IllegalStateException("WebSocket连接已断开", ex);
                }
                if (!sendSystemMessage(session, "连接失败 " + target.displayName() + ": " + ex.getMessage())) {
                    closeContext(session.getId());
                    throw new IllegalStateException("WebSocket连接已断开", ex);
                }
            }
        }
        if (context.handleCount() <= 0) {
            closeContext(session.getId());
            throw new IllegalStateException("没有可用日志流，请检查环境/服务或连接配置");
        }
        log.info("event={} stage={} sessionId={} ip={} streamCount={}",
                EVENT_WS_STREAM, STAGE_SUBSCRIBE_SUCCESS, session.getId(), clientIp, context.handleCount());
        sendSystemMessage(session, "已启动 " + context.handleCount() + " 个日志流");
    }

    /**
     * 校验日志文件选择策略。
     * <p>
     * 单服务模式要求显式传入 filePath，避免订阅时回落到后端默认文件导致排查对象不清晰。
     * </p>
     *
     * @param request 订阅请求
     */
    private void validateFilePathPolicy(LogStreamRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("请求参数不能为空");
        }
        if (!request.isAllService() && request.normalizedFilePath().isEmpty()) {
            throw new IllegalArgumentException("单服务模式下必须显式选择日志文件");
        }
    }

    /**
     * 发送业务日志行（原文透传，不附加环境/服务前缀）。
     *
     * @param session WebSocket 会话
     * @param content 日志内容
     */
    private boolean sendLogLine(WebSocketSession session, String content) {
        return sendText(session, content);
    }

    /**
     * 发送系统提示消息。
     *
     * @param session WebSocket 会话
     * @param content 提示内容
     */
    private boolean sendSystemMessage(WebSocketSession session, String content) {
        return sendText(session, "[系统提示] " + content);
    }

    /**
     * 安全发送文本消息。
     *
     * @param session WebSocket 会话
     * @param content 文本内容
     */
    private boolean sendText(WebSocketSession session, String content) {
        if (session == null) {
            return false;
        }
        if (!session.isOpen()) {
            closeContext(session.getId());
            return false;
        }
        StreamContext context = getOrCreateContext(session.getId());
        if (context.enqueue(session, content)) {
            return true;
        }
        log.warn("event={} stage={} sessionId={} ip={} error={}",
                EVENT_WS_STREAM,
                STAGE_SEND_FAILED,
                session.getId(),
                resolveClientIp(session),
                "send-buffer-overflow-or-session-closed");
        closeContext(session.getId());
        closeSessionQuietly(session);
        return false;
    }

    /**
     * 安静关闭 WebSocket 会话。
     *
     * @param session WebSocket 会话
     */
    private void closeSessionQuietly(WebSocketSession session) {
        if (session == null) {
            return;
        }
        try {
            if (session.isOpen()) {
                session.close(CloseStatus.SERVER_ERROR);
            }
        } catch (Exception ignored) {
            // ignore
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
     * 获取或创建会话发送上下文。
     *
     * @param sessionId 会话 ID
     * @return 会话上下文
     */
    private StreamContext getOrCreateContext(String sessionId) {
        return streamContexts.computeIfAbsent(sessionId, key -> new StreamContext());
    }

    /**
     * 立即发送已合并的文本消息。
     *
     * @param session WebSocket 会话
     * @param content 已拼装完成的文本内容
     * @return true 表示发送成功
     */
    private boolean sendTextNow(WebSocketSession session, String content) {
        if (session == null || !session.isOpen()) {
            return false;
        }
        synchronized (session) {
            if (!session.isOpen()) {
                return false;
            }
            try {
                session.sendMessage(new TextMessage(content));
                return true;
            } catch (IOException ex) {
                log.warn("event={} stage={} sessionId={} ip={} error={}",
                        EVENT_WS_STREAM, STAGE_SEND_FAILED, session.getId(), resolveClientIp(session), ex.getMessage());
                return false;
            }
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
    private class StreamContext {

        /**
         * 当前会话内所有活跃日志流句柄。
         */
        private final List<SshLogService.StreamHandle> handles = new CopyOnWriteArrayList<>();

        /**
         * 发送缓冲区互斥锁。
         */
        private final Object sendMonitor = new Object();

        /**
         * 待发送文本缓冲区。
         */
        private final StringBuilder pendingText = new StringBuilder();

        /**
         * 当前缓冲区字节数。
         */
        private int pendingBytes;

        /**
         * 当前是否正在执行发送。
         */
        private boolean sending;

        /**
         * 延迟刷新任务句柄。
         */
        private ScheduledFuture<?> flushFuture;

        /**
         * 会话上下文关闭标记。
         */
        private final AtomicBoolean closed = new AtomicBoolean(false);

        /**
         * 追加一个流句柄。
         *
         * @param handle 流句柄
         */
        private void addHandle(SshLogService.StreamHandle handle) {
            if (handle != null) {
                handles.add(handle);
            }
        }

        /**
         * 获取当前流句柄数量。
         *
         * @return 句柄数量
         */
        private int handleCount() {
            return handles.size();
        }

        /**
         * 将文本消息放入会话发送缓冲区。
         * <p>
         * 采用“按会话聚合 + 短周期批量 flush”的方式，减少高频单行日志导致的
         * sendMessage 调用次数；若单会话缓冲持续堆积超出上限，则视为慢消费者。
         * </p>
         *
         * @param session WebSocket 会话
         * @param content 文本内容
         * @return true 表示已成功入队
         */
        private boolean enqueue(WebSocketSession session, String content) {
            String payload = content + "\n";
            int bytes = payload.getBytes(StandardCharsets.UTF_8).length;
            synchronized (sendMonitor) {
                if (closed.get()) {
                    return false;
                }
                if (pendingBytes + bytes > SESSION_SEND_BUFFER_LIMIT_BYTES) {
                    return false;
                }
                pendingText.append(payload);
                pendingBytes += bytes;
                scheduleFlushLocked(session, pendingBytes >= SESSION_SEND_BATCH_BYTES ? 0L : SESSION_SEND_FLUSH_MS);
                return true;
            }
        }

        /**
         * 在已持有发送锁的前提下调度 flush 任务。
         *
         * @param session WebSocket 会话
         * @param delayMs 延迟毫秒
         */
        private void scheduleFlushLocked(WebSocketSession session, long delayMs) {
            if (closed.get()) {
                return;
            }
            if (flushFuture != null && !flushFuture.isDone()) {
                if (delayMs > 0L) {
                    return;
                }
                flushFuture.cancel(false);
            }
            flushFuture = sendExecutor.schedule(() -> flushPending(session), Math.max(0L, delayMs), TimeUnit.MILLISECONDS);
        }

        /**
         * 刷新当前缓冲区，将多条消息合并后一次性发送。
         *
         * @param session WebSocket 会话
         */
        private void flushPending(WebSocketSession session) {
            String payload;
            synchronized (sendMonitor) {
                flushFuture = null;
                if (closed.get() || sending || pendingBytes <= 0 || pendingText.length() <= 0) {
                    return;
                }
                sending = true;
                payload = pendingText.toString();
                pendingText.setLength(0);
                pendingBytes = 0;
            }
            boolean success = sendTextNow(session, payload);
            boolean shouldClose = !success;
            synchronized (sendMonitor) {
                sending = false;
                if (!shouldClose && !closed.get() && pendingBytes > 0) {
                    scheduleFlushLocked(session, pendingBytes >= SESSION_SEND_BATCH_BYTES ? 0L : SESSION_SEND_FLUSH_MS);
                }
            }
            if (shouldClose && session != null) {
                closeContext(session.getId());
                closeSessionQuietly(session);
            }
        }

        /**
         * 关闭全部流句柄。
         */
        private void closeAll() {
            if (!closed.compareAndSet(false, true)) {
                return;
            }
            synchronized (sendMonitor) {
                if (flushFuture != null) {
                    flushFuture.cancel(false);
                    flushFuture = null;
                }
                pendingText.setLength(0);
                pendingBytes = 0;
                sending = false;
            }
            for (SshLogService.StreamHandle handle : handles) {
                try {
                    handle.close();
                } catch (Exception ignored) {
                    // ignore
                }
            }
        }
    }

    /**
     * WebSocket 会话已不可用异常。
     */
    private static class SessionClosedException extends RuntimeException {

        private SessionClosedException(String message) {
            super(message);
        }
    }
}
