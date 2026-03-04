package com.ww.app.ssh.service;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.ww.app.ssh.config.LogPanelProperties;
import com.ww.app.ssh.model.HostMetricSnapshot;
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
     * 采集目标主机实时指标。
     * <p>
     * 该方法会通过 SSH 分别执行 CPU、内存、负载采集命令，
     * 并将结果转换为统一快照对象返回。
     * 若任一步骤失败，不抛出异常，而是返回 status=error 的结果，
     * 避免单个节点失败影响整个页面展示。
     * </p>
     *
     * @param target 目标服务节点
     * @return 指标快照
     */
    public HostMetricSnapshot queryHostMetric(LogTarget target) {
        HostMetricSnapshot snapshot = initMetricSnapshot(target);
        try {
            String cpuLine = firstLine(executeCommandForLines(target, sshCommandBuilder.buildCpuUsageCommand(), 1));
            String memoryLine = firstLine(executeCommandForLines(target, sshCommandBuilder.buildMemoryUsageCommand(), 1));
            String loadLine = firstLine(executeCommandForLines(target, sshCommandBuilder.buildLoadAverageCommand(), 1));

            snapshot.setCpuUsagePercent(normalizePercent(parseDouble(cpuLine)));
            applyMemoryMetrics(snapshot, memoryLine);
            applyLoadMetrics(snapshot, loadLine);
            snapshot.setStatus("ok");
            snapshot.setMessage("采集成功");
        } catch (Exception ex) {
            snapshot.setStatus("error");
            snapshot.setMessage(limitMessage(ex.getMessage()));
        }
        return snapshot;
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
        List<LogStreamRequest.FilterRule> filterRules = resolveEffectiveRules(request);
        String command = sshCommandBuilder.buildTailCommand(resolvedFile, request.normalizedLines());
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

            Session finalSession = session;
            ChannelExec finalChannel = channel;
            List<LogStreamRequest.FilterRule> finalFilterRules = filterRules;
            Future<?> streamTask = streamExecutor.submit(() -> readStreamLoop(inputStream, finalFilterRules, lineConsumer));
            return new StreamHandle(target, resolvedFile, streamTask, finalChannel, finalSession);
        } catch (Exception ex) {
            disconnectQuietly(channel);
            disconnectQuietly(session);
            throw new IllegalStateException("启动日志流失败: " + target.displayName() + " - " + ex.getMessage(), ex);
        }
    }

    /**
     * 通过 cat 快照模式读取日志。
     * <p>
     * 该方法会一次性返回最新 N 行并结束，不会保持长连接监听。
     * </p>
     *
     * @param target  目标服务节点
     * @param request 日志订阅请求
     * @return 过滤后的日志行
     */
    public List<String> readByCat(LogTarget target, LogStreamRequest request) {
        String resolvedFile = resolveLogFilePath(target, request.normalizedFilePath());
        int lines = request.normalizedLines();
        String command = sshCommandBuilder.buildCatCommand(resolvedFile, lines);
        List<String> rawLines = executeCommandForLines(target, command, lines + 20);
        List<LogStreamRequest.FilterRule> filterRules = resolveEffectiveRules(request);
        if (filterRules.isEmpty()) {
            return trimToWindow(rawLines, lines);
        }
        List<String> filtered = new ArrayList<>();
        for (String line : rawLines) {
            if (logLineFilterMatcher.matches(line, filterRules)) {
                filtered.add(line);
            }
        }
        return trimToWindow(filtered, lines);
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
     * @param filterRules 过滤规则
     * @param consumer    行消费回调
     */
    private void readStreamLoop(InputStream inputStream,
                                List<LogStreamRequest.FilterRule> filterRules,
                                Consumer<String> consumer) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while (!Thread.currentThread().isInterrupted() && (line = reader.readLine()) != null) {
                if (logLineFilterMatcher.matches(line, filterRules)) {
                    consumer.accept(line);
                }
            }
        } catch (IOException ex) {
            consumer.accept("[系统提示] 日志流读取异常: " + ex.getMessage());
        }
    }

    /**
     * 构建旧字段兼容过滤规则。
     *
     * @param includeKeyword 包含关键字
     * @param excludeKeyword 排除关键字
     * @return 规则集合
     */
    private List<LogStreamRequest.FilterRule> buildFallbackRules(String includeKeyword, String excludeKeyword) {
        List<LogStreamRequest.FilterRule> rules = new ArrayList<>();
        if (!includeKeyword.isEmpty()) {
            LogStreamRequest.FilterRule include = new LogStreamRequest.FilterRule();
            include.setType(LogStreamRequest.FILTER_TYPE_INCLUDE);
            include.setData(includeKeyword);
            rules.add(include);
        }
        if (!excludeKeyword.isEmpty()) {
            LogStreamRequest.FilterRule exclude = new LogStreamRequest.FilterRule();
            exclude.setType(LogStreamRequest.FILTER_TYPE_EXCLUDE);
            exclude.setData(excludeKeyword);
            rules.add(exclude);
        }
        return rules;
    }

    /**
     * 解析最终生效的过滤规则。
     * <p>
     * 优先使用链式规则；若为空则回退到 include/exclude 旧字段，保证向后兼容。
     * </p>
     *
     * @param request 日志请求
     * @return 生效规则列表
     */
    private List<LogStreamRequest.FilterRule> resolveEffectiveRules(LogStreamRequest request) {
        List<LogStreamRequest.FilterRule> filterRules = request.normalizedFilterRules();
        if (!filterRules.isEmpty()) {
            return filterRules;
        }
        return buildFallbackRules(request.normalizedIncludeKeyword(), request.normalizedExcludeKeyword());
    }

    /**
     * 保留列表尾部窗口，避免返回超量数据。
     *
     * @param source 原始列表
     * @param max    最大保留条数
     * @return 裁剪后的列表
     */
    private List<String> trimToWindow(List<String> source, int max) {
        if (source == null || source.isEmpty()) {
            return new ArrayList<>();
        }
        if (source.size() <= max) {
            return new ArrayList<>(source);
        }
        int start = source.size() - max;
        return new ArrayList<>(source.subList(start, source.size()));
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
     * 初始化主机指标快照基础字段。
     *
     * @param target 目标节点
     * @return 已填充基础信息的快照对象
     */
    private HostMetricSnapshot initMetricSnapshot(LogTarget target) {
        HostMetricSnapshot snapshot = new HostMetricSnapshot();
        snapshot.setEnv(target.getEnv());
        snapshot.setService(target.getService());
        snapshot.setHost(target.getServerNode() == null ? "" : trimToEmpty(target.getServerNode().getHost()));
        snapshot.setUpdatedAt(System.currentTimeMillis());
        snapshot.setStatus("error");
        snapshot.setMessage("采集中");
        return snapshot;
    }

    /**
     * 应用内存指标。
     *
     * @param snapshot   目标快照
     * @param memoryLine 命令输出行，格式：使用率 已用MB 总MB
     */
    private void applyMemoryMetrics(HostMetricSnapshot snapshot, String memoryLine) {
        String[] parts = splitByBlank(memoryLine);
        if (parts.length < 3) {
            return;
        }
        snapshot.setMemoryUsagePercent(normalizePercent(parseDouble(parts[0])));
        snapshot.setMemoryUsedMb(parseLong(parts[1]));
        snapshot.setMemoryTotalMb(parseLong(parts[2]));
    }

    /**
     * 应用负载指标。
     *
     * @param snapshot 目标快照
     * @param loadLine 命令输出行，格式：1m 5m 15m
     */
    private void applyLoadMetrics(HostMetricSnapshot snapshot, String loadLine) {
        String[] parts = splitByBlank(loadLine);
        if (parts.length < 3) {
            return;
        }
        snapshot.setLoad1m(parseDouble(parts[0]));
        snapshot.setLoad5m(parseDouble(parts[1]));
        snapshot.setLoad15m(parseDouble(parts[2]));
    }

    /**
     * 获取首行文本。
     *
     * @param lines 文本行集合
     * @return 首行内容，若为空则返回空字符串
     */
    private String firstLine(List<String> lines) {
        if (lines == null || lines.isEmpty()) {
            return "";
        }
        return trimToEmpty(lines.get(0));
    }

    /**
     * 按空白字符拆分字符串。
     *
     * @param text 输入文本
     * @return 拆分结果
     */
    private String[] splitByBlank(String text) {
        String normalized = trimToEmpty(text);
        return normalized.isEmpty() ? new String[0] : normalized.split("\\s+");
    }

    /**
     * 解析浮点数。
     *
     * @param text 输入文本
     * @return 解析结果，失败返回 null
     */
    private Double parseDouble(String text) {
        try {
            String normalized = trimToEmpty(text);
            if (normalized.isEmpty()) {
                return null;
            }
            return Double.parseDouble(normalized);
        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * 解析长整型。
     *
     * @param text 输入文本
     * @return 解析结果，失败返回 null
     */
    private Long parseLong(String text) {
        try {
            String normalized = trimToEmpty(text);
            if (normalized.isEmpty()) {
                return null;
            }
            return Long.parseLong(normalized);
        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * 规范化百分比范围。
     *
     * @param value 原始值
     * @return 限制在 0~100 的百分比值
     */
    private Double normalizePercent(Double value) {
        if (value == null) {
            return null;
        }
        if (value < 0D) {
            return 0D;
        }
        if (value > 100D) {
            return 100D;
        }
        return value;
    }

    /**
     * 截断错误消息，避免返回过长文本污染前端卡片布局。
     *
     * @param message 错误消息
     * @return 截断后的错误消息
     */
    private String limitMessage(String message) {
        String normalized = trimToEmpty(message);
        if (normalized.length() <= 120) {
            return normalized;
        }
        return normalized.substring(0, 120);
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
