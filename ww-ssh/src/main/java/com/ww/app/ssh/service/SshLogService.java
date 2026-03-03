package com.ww.app.ssh.service;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.ww.app.ssh.config.LogPanelProperties;
import com.ww.app.ssh.model.LogStreamRequest;
import com.ww.app.ssh.model.LogTarget;
import com.ww.app.ssh.service.support.LogLineFilterMatcher;
import com.ww.app.ssh.service.support.SshCommandBuilder;
import lombok.Getter;
import lombok.NonNull;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * SSH 日志读取服务。
 * <p>
 * 负责远程文件发现、命令执行以及 tail 流式读取。
 * </p>
 */
@Service
public class SshLogService {

    /**
     * SSH 命令构建器。
     */
    private final SshCommandBuilder sshCommandBuilder;

    /**
     * 日志行过滤匹配器。
     */
    private final LogLineFilterMatcher logLineFilterMatcher;

    /**
     * 流式读取线程池。
     */
    private final ExecutorService streamExecutor = Executors.newCachedThreadPool(new NamedThreadFactory());

    /**
     * 构造方法。
     *
     * @param sshCommandBuilder    SSH 命令构建器
     * @param logLineFilterMatcher 日志行过滤匹配器
     */
    public SshLogService(SshCommandBuilder sshCommandBuilder, LogLineFilterMatcher logLineFilterMatcher) {
        this.sshCommandBuilder = sshCommandBuilder;
        this.logLineFilterMatcher = logLineFilterMatcher;
    }

    /**
     * 查询服务日志目录下的候选日志文件。
     *
     * @param target 目标服务节点
     * @return 日志文件路径列表
     */
    public List<String> listLogFiles(LogTarget target) {
        String logPath = trimToEmpty(target.getServerNode().getLogPath());
        if (logPath.isEmpty()) {
            return new ArrayList<>();
        }
        String command = sshCommandBuilder.buildListFilesCommand(logPath);
        List<String> rawLines = executeCommandForLines(target, command, 300);
        Set<String> deduplicated = new LinkedHashSet<>();
        for (String line : rawLines) {
            String trimmed = trimToEmpty(line);
            if (!trimmed.isEmpty()) {
                deduplicated.add(trimmed);
            }
        }
        return new ArrayList<>(deduplicated);
    }

    /**
     * 启动单个目标的实时日志流。
     *
     * @param target       目标服务节点
     * @param request      日志订阅请求
     * @param lineConsumer 日志行消费回调
     * @return 可关闭的流句柄
     */
    public StreamHandle startStreaming(LogTarget target, LogStreamRequest request, Consumer<String> lineConsumer) {
        String resolvedFile = resolveLogFilePath(target, request.normalizedFilePath());
        String includeKeyword = request.normalizedIncludeKeyword();
        String excludeKeyword = request.normalizedExcludeKeyword();
        Session session = null;
        ChannelExec channel = null;
        try {
            session = openSession(target.getServerNode());
            channel = (ChannelExec) session.openChannel("exec");
            channel.setInputStream(null);
            channel.setPty(true);
            channel.setCommand(sshCommandBuilder.buildTailCommand(resolvedFile, request.normalizedLines()));
            InputStream inputStream = channel.getInputStream();
            channel.connect(resolveTimeout(target.getServerNode()));

            Session finalSession = session;
            ChannelExec finalChannel = channel;
            Future<?> streamTask = streamExecutor.submit(() -> readStreamLoop(inputStream, includeKeyword, excludeKeyword, lineConsumer));
            return new StreamHandle(target, resolvedFile, streamTask, finalChannel, finalSession);
        } catch (Exception ex) {
            disconnectQuietly(channel);
            disconnectQuietly(session);
            throw new IllegalStateException("启动日志流失败: " + target.displayName() + " - " + ex.getMessage(), ex);
        }
    }

    /**
     * 应用关闭前释放线程池资源。
     */
    @PreDestroy
    public void shutdown() {
        streamExecutor.shutdownNow();
    }

    /**
     * 循环读取远程日志流并转发。
     *
     * @param inputStream 远程输入流
     * @param includeKeyword 包含关键字
     * @param excludeKeyword 排除关键字
     * @param consumer    行消费回调
     */
    private void readStreamLoop(InputStream inputStream,
                                String includeKeyword,
                                String excludeKeyword,
                                Consumer<String> consumer) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while (!Thread.currentThread().isInterrupted() && (line = reader.readLine()) != null) {
                if (logLineFilterMatcher.matches(line, includeKeyword, excludeKeyword)) {
                    consumer.accept(line);
                }
            }
        } catch (IOException ex) {
            consumer.accept("[系统提示] 日志流读取异常: " + ex.getMessage());
        }
    }

    /**
     * 自动解析真实日志文件路径。
     * <p>
     * 规则：
     * 1. 前端显式传入 filePath 时直接使用；<br>
     * 2. 否则从配置 logPath 中自动发现最新日志文件。
     * </p>
     *
     * @param target         目标服务节点
     * @param requestedPath  前端指定路径
     * @return 可用于 tail 的文件路径
     */
    private String resolveLogFilePath(LogTarget target, String requestedPath) {
        if (!requestedPath.isEmpty()) {
            return requestedPath;
        }
        String configuredPath = trimToEmpty(target.getServerNode().getLogPath());
        if (configuredPath.isEmpty()) {
            throw new IllegalArgumentException("未配置默认日志目录，且请求未指定 filePath");
        }
        String command = sshCommandBuilder.buildLatestFileCommand(configuredPath);
        List<String> lines = executeCommandForLines(target, command, 5);
        for (String line : lines) {
            String trimmed = trimToEmpty(line);
            if (!trimmed.isEmpty()) {
                return trimmed;
            }
        }
        throw new IllegalStateException("未发现可读取的日志文件: " + target.displayName());
    }

    /**
     * 执行命令并读取输出行。
     *
     * @param target   目标服务节点
     * @param command  待执行命令
     * @param maxLines 最多返回行数
     * @return 命令输出
     */
    private List<String> executeCommandForLines(LogTarget target, String command, int maxLines) {
        List<String> lines = new ArrayList<>();
        Session session = null;
        ChannelExec channel = null;
        try {
            session = openSession(target.getServerNode());
            channel = (ChannelExec) session.openChannel("exec");
            channel.setInputStream(null);
            channel.setPty(true);
            channel.setCommand(command);
            InputStream inputStream = channel.getInputStream();
            channel.connect(resolveTimeout(target.getServerNode()));

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    lines.add(line);
                    if (lines.size() >= maxLines) {
                        break;
                    }
                }
            }
            return lines;
        } catch (Exception ex) {
            throw new IllegalStateException("执行远程命令失败: " + target.displayName() + " - " + ex.getMessage(), ex);
        } finally {
            disconnectQuietly(channel);
            disconnectQuietly(session);
        }
    }

    /**
     * 建立 SSH Session 连接。
     *
     * @param node 节点配置
     * @return 已连接 Session
     * @throws JSchException 建连失败
     */
    private Session openSession(LogPanelProperties.ServerNode node) throws JSchException {
        JSch jsch = new JSch();
        if (!trimToEmpty(node.getPrivateKeyPath()).isEmpty()) {
            if (!trimToEmpty(node.getPrivateKeyPassphrase()).isEmpty()) {
                jsch.addIdentity(node.getPrivateKeyPath(), node.getPrivateKeyPassphrase());
            } else {
                jsch.addIdentity(node.getPrivateKeyPath());
            }
        }
        Session session = jsch.getSession(node.getUsername(), node.getHost(), resolvePort(node));
        if (!trimToEmpty(node.getPassword()).isEmpty()) {
            session.setPassword(node.getPassword());
        }
        Properties config = new Properties();
        config.put("StrictHostKeyChecking", "no");
        if (!trimToEmpty(node.getPreferredAuthentications()).isEmpty()) {
            config.put("PreferredAuthentications", node.getPreferredAuthentications());
        }
        session.setConfig(config);
        session.connect(resolveTimeout(node));
        return session;
    }

    /**
     * 解析端口，缺省为 22。
     *
     * @param node 节点配置
     * @return SSH 端口
     */
    private int resolvePort(LogPanelProperties.ServerNode node) {
        Integer port = node.getPort();
        return port == null || port <= 0 ? 22 : port;
    }

    /**
     * 解析连接超时时间，缺省为 8000ms。
     *
     * @param node 节点配置
     * @return 超时毫秒
     */
    private int resolveTimeout(LogPanelProperties.ServerNode node) {
        Integer timeout = node.getConnectTimeoutMs();
        return timeout == null || timeout <= 0 ? 8000 : timeout;
    }

    /**
     * 字符串去空格并兜底空值。
     *
     * @param source 原字符串
     * @return 非 null 字符串
     */
    private String trimToEmpty(String source) {
        return source == null ? "" : source.trim();
    }

    /**
     * 安静断开 SSH 资源，忽略关闭异常。
     *
     * @param closeable 可关闭资源
     */
    private void disconnectQuietly(Object closeable) {
        if (closeable == null) {
            return;
        }
        try {
            if (closeable instanceof ChannelExec) {
                ((ChannelExec) closeable).disconnect();
            } else if (closeable instanceof Session) {
                ((Session) closeable).disconnect();
            } else if (closeable instanceof Closeable) {
                ((Closeable) closeable).close();
            }
        } catch (Exception ignored) {
            // ignore
        }
    }

    /**
     * 日志流句柄。
     * <p>
     * 一个句柄对应一个目标服务的长连接 tail 任务。
     * </p>
     */
    public static class StreamHandle {

        /**
         * 目标服务。
         * -- GETTER --
         *  获取目标服务。
         */
        @Getter
        private final LogTarget target;

        /**
         * 实际读取的日志文件。
         * -- GETTER --
         *  获取日志文件路径。
         */
        @Getter
        private final String filePath;

        /**
         * 异步读取任务。
         */
        private final Future<?> streamTask;

        /**
         * SSH 执行通道。
         */
        private final ChannelExec channel;

        /**
         * SSH 会话。
         */
        private final Session session;

        /**
         * 构造方法。
         *
         * @param target     目标服务
         * @param filePath   日志文件路径
         * @param streamTask 异步任务
         * @param channel    SSH 通道
         * @param session    SSH 会话
         */
        public StreamHandle(LogTarget target, String filePath, Future<?> streamTask, ChannelExec channel, Session session) {
            this.target = target;
            this.filePath = filePath;
            this.streamTask = streamTask;
            this.channel = channel;
            this.session = session;
        }

        /**
         * 关闭流任务与底层 SSH 资源。
         */
        public void close() {
            if (streamTask != null) {
                streamTask.cancel(true);
            }
            if (channel != null) {
                channel.disconnect();
            }
            if (session != null) {
                session.disconnect();
            }
        }
    }

    /**
     * 简单线程工厂，用于标识日志流线程。
     */
    private static class NamedThreadFactory implements ThreadFactory {

        /**
         * 线程编号序列。
         */
        private final AtomicInteger sequence = new AtomicInteger(1);

        @Override
        public Thread newThread(@NonNull Runnable runnable) {
            Thread thread = new Thread(runnable, "ww-ssh-log-stream-" + sequence.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        }
    }
}
