package com.ww.app.ssh.service;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.ww.app.ssh.config.LogPanelProperties;
import com.ww.app.ssh.model.HostMetricSnapshot;
import com.ww.app.ssh.model.InstanceOperationRequest;
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
     * 实例运维命令退出码回显标记。
     */
    private static final String INSTANCE_EXIT_MARKER = "__WW_INSTANCE_EXIT__:";

    /**
     * 实例状态：运行中。
     */
    private static final String INSTANCE_STATUS_RUNNING = "running";

    /**
     * 实例状态：已停止。
     */
    private static final String INSTANCE_STATUS_STOPPED = "stopped";

    /**
     * 实例状态：未知。
     */
    private static final String INSTANCE_STATUS_UNKNOWN = "unknown";

    /**
     * 实例状态：未配置。
     */
    private static final String INSTANCE_STATUS_UNCONFIGURED = "unconfigured";

    /**
     * 启动/重启动作状态校验最大重试次数。
     */
    private static final int OP_VERIFY_RETRY_START = 6;

    /**
     * 停止动作状态校验最大重试次数。
     */
    private static final int OP_VERIFY_RETRY_STOP = 4;

    /**
     * 状态校验重试间隔（毫秒）。
     */
    private static final long OP_VERIFY_SLEEP_MS = 1200L;

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
            applyInstanceStatus(snapshot, target);
            snapshot.setStatus("ok");
            snapshot.setMessage("采集成功");
        } catch (Exception ex) {
            applyInstanceStatus(snapshot, target);
            snapshot.setStatus("error");
            snapshot.setMessage(limitMessage(ex.getMessage()));
        }
        return snapshot;
    }

    /**
     * 执行实例启停运维动作。
     * <p>
     * 动作执行依赖 {@code manageCommandFile} 配置，
     * 由目标机器本地脚本负责具体启停逻辑。
     * </p>
     *
     * @param target 目标实例
     * @param action 操作动作（start/restart/stop）
     * @return 命令输出文本
     */
    public String operateInstance(LogTarget target, String action) {
        String normalizedAction = trimToEmpty(action).toLowerCase();
        if (InstanceOperationRequest.ACTION_START.equals(normalizedAction)
                || InstanceOperationRequest.ACTION_STOP.equals(normalizedAction)) {
            InstanceStatusProbe current = probeInstanceStatus(target);
            if (InstanceOperationRequest.ACTION_START.equals(normalizedAction) && current.isRunning()) {
                return "实例当前已在运行状态，已跳过重复启动";
            }
            if (InstanceOperationRequest.ACTION_STOP.equals(normalizedAction) && current.isStopped()) {
                return "实例当前已在停止状态，已跳过重复停止";
            }
        }
        String commandFile = resolveManageCommandFile(target);
        String command = sshCommandBuilder.buildInstanceOperationCommand(commandFile, action);
        CommandReadResult commandResult = executeCommandForResult(target, command, 2000, INSTANCE_EXIT_MARKER);
        List<String> lines = commandResult.getLines();
        int exitCode = 1;
        boolean hasExitMarker = false;
        List<String> userOutput = new ArrayList<>();
        for (String line : lines) {
            String normalized = line == null ? "" : line.trim();
            if (normalized.startsWith(INSTANCE_EXIT_MARKER)) {
                exitCode = parseExitCode(normalized.substring(INSTANCE_EXIT_MARKER.length()));
                hasExitMarker = true;
                continue;
            }
            userOutput.add(line);
        }
        if (!hasExitMarker) {
            if (commandResult.isOutputTruncated()) {
                throw new IllegalStateException("命令输出过长且未读到退出码标记，请收敛脚本输出后重试");
            }
            throw new IllegalStateException("命令执行结果未知：未获取退出码，请检查脚本是否中途 exit 或输出过多");
        }
        if (userOutput.isEmpty()) {
            if (exitCode != 0) {
                throw new IllegalStateException("命令执行失败，退出码: " + exitCode);
            }
            return "命令已执行完成，无输出";
        }
        StringBuilder builder = new StringBuilder();
        for (String line : userOutput) {
            if (builder.length() > 0) {
                builder.append('\n');
            }
            builder.append(line == null ? "" : line);
        }
        if (commandResult.isOutputTruncated()) {
            if (builder.length() > 0) {
                builder.append('\n');
            }
            builder.append("[系统提示] 命令输出过长，已截断部分内容");
        }
        if (exitCode != 0) {
            throw new IllegalStateException("命令执行失败，退出码: " + exitCode + "，输出: " + builder);
        }
        String output = builder.toString();
        verifyOperationEffect(target, action, output);
        return output;
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
     * 解析实例运维脚本路径。
     *
     * @param target 目标实例
     * @return 脚本路径
     */
    private String resolveManageCommandFile(LogTarget target) {
        if (target == null || target.getServerNode() == null) {
            throw new IllegalArgumentException("实例配置不存在，无法执行运维操作");
        }
        String commandFile = trimToEmpty(target.getServerNode().getManageCommandFile());
        if (commandFile.isEmpty()) {
            throw new IllegalArgumentException("未配置 manageCommandFile，无法执行实例运维操作");
        }
        return commandFile;
    }

    /**
     * 采集实例运行状态。
     *
     * @param snapshot 指标快照
     * @param target   目标实例
     */
    private void applyInstanceStatus(HostMetricSnapshot snapshot, LogTarget target) {
        if (snapshot == null) {
            return;
        }
        if (!hasManageCommandFile(target)) {
            snapshot.setInstanceStatus(INSTANCE_STATUS_UNCONFIGURED);
            snapshot.setInstanceStatusDetail("未配置运维脚本");
            return;
        }
        try {
            InstanceStatusProbe probe = probeInstanceStatus(target);
            snapshot.setInstanceStatus(probe.status);
            String detail = trimToEmpty(probe.detail);
            if (detail.isEmpty()) {
                if (INSTANCE_STATUS_RUNNING.equals(probe.status)) {
                    detail = "运行中";
                } else if (INSTANCE_STATUS_STOPPED.equals(probe.status)) {
                    detail = "已停止";
                } else {
                    detail = "状态未知";
                }
            }
            snapshot.setInstanceStatusDetail(detail);
        } catch (Exception ex) {
            snapshot.setInstanceStatus(INSTANCE_STATUS_UNKNOWN);
            snapshot.setInstanceStatusDetail("状态检测失败: " + limitMessage(ex.getMessage()));
        }
    }

    /**
     * 汇总状态脚本输出，避免前端显示过长文本。
     *
     * @param lines 输出行
     * @return 摘要文本
     */
    private String summarizeStatusOutput(List<String> lines) {
        if (lines == null || lines.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        int count = 0;
        for (String line : lines) {
            String normalized = trimToEmpty(line);
            if (normalized.isEmpty()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(" | ");
            }
            builder.append(normalized);
            count++;
            if (count >= 2 || builder.length() >= 96) {
                break;
            }
        }
        String text = builder.toString();
        if (text.length() <= 96) {
            return text;
        }
        return text.substring(0, 96);
    }

    /**
     * 校验实例运维动作是否真正生效。
     * <p>
     * 仅对 start/restart/stop 执行状态回读校验：
     * 1. start/restart 期望状态为运行中；<br>
     * 2. stop 期望状态为已停止。<br>
     * 为兼容脚本异步启动/停止场景，按动作做短时重试。
     * </p>
     *
     * @param target          目标实例
     * @param action          运维动作
     * @param operationOutput 动作命令输出
     */
    private void verifyOperationEffect(LogTarget target, String action, String operationOutput) {
        String normalizedAction = trimToEmpty(action).toLowerCase();
        if (!InstanceOperationRequest.ACTION_START.equals(normalizedAction)
                && !InstanceOperationRequest.ACTION_RESTART.equals(normalizedAction)
                && !InstanceOperationRequest.ACTION_STOP.equals(normalizedAction)) {
            return;
        }
        boolean expectedRunning = !InstanceOperationRequest.ACTION_STOP.equals(normalizedAction);
        int retries = expectedRunning ? OP_VERIFY_RETRY_START : OP_VERIFY_RETRY_STOP;
        InstanceStatusProbe lastProbe = null;
        for (int i = 0; i < retries; i++) {
            lastProbe = probeInstanceStatus(target);
            if (expectedRunning && lastProbe.isRunning()) {
                return;
            }
            if (!expectedRunning && lastProbe.isStopped()) {
                return;
            }
            if (i + 1 < retries) {
                sleepQuietly(OP_VERIFY_SLEEP_MS);
            }
        }
        if (lastProbe == null) {
            String message = "命令已执行，但状态校验失败：未获取到状态回读结果";
            String trimmedOutput = trimToEmpty(operationOutput);
            if (!trimmedOutput.isEmpty()) {
                message = message + "，命令输出: " + limitMessage(trimmedOutput);
            }
            throw new IllegalStateException(message);
        }
        if (expectedRunning && lastProbe.isRunning()) {
            return;
        }
        if (!expectedRunning && !lastProbe.isRunning()) {
            return;
        }
        String expectedText = expectedRunning ? "运行中" : "已停止";
        String actualText = toStatusText(lastProbe.status);
        String detail = lastProbe == null ? "状态回读失败" : trimToEmpty(lastProbe.detail);
        String message = "命令已执行，但状态校验未通过，期望: " + expectedText
                + "，实际: " + actualText
                + (detail.isEmpty() ? "" : "，状态详情: " + detail);
        String trimmedOutput = trimToEmpty(operationOutput);
        if (!trimmedOutput.isEmpty()) {
            message = message + "，命令输出: " + limitMessage(trimmedOutput);
        }
        throw new IllegalStateException(message);
    }

    /**
     * 读取实例状态探测结果。
     *
     * @param target 目标实例
     * @return 状态探测结果
     */
    private InstanceStatusProbe probeInstanceStatus(LogTarget target) {
        String commandFile = resolveManageCommandFile(target);
        String command = sshCommandBuilder.buildInstanceStatusCommand(commandFile);
        CommandReadResult commandResult = executeCommandForResult(target, command, 160, INSTANCE_EXIT_MARKER);
        List<String> lines = commandResult.getLines();
        int exitCode = 1;
        boolean hasExitMarker = false;
        List<String> output = new ArrayList<>();
        for (String line : lines) {
            String normalized = line == null ? "" : line.trim();
            if (normalized.startsWith(INSTANCE_EXIT_MARKER)) {
                exitCode = parseExitCode(normalized.substring(INSTANCE_EXIT_MARKER.length()));
                hasExitMarker = true;
                continue;
            }
            if (!normalized.isEmpty()) {
                output.add(line);
            }
        }
        String detail = summarizeStatusOutput(output);
        if (exitCode == 0) {
            return new InstanceStatusProbe(INSTANCE_STATUS_RUNNING, detail.isEmpty() ? "运行中" : detail);
        }
        String normalizedDetail = detail.toLowerCase();
        if (isStoppedDetail(normalizedDetail)) {
            return new InstanceStatusProbe(INSTANCE_STATUS_STOPPED, detail.isEmpty()
                    ? "状态脚本返回退出码: " + exitCode
                    : detail);
        }
        if (isRunningDetail(normalizedDetail)) {
            return new InstanceStatusProbe(INSTANCE_STATUS_RUNNING, detail);
        }
        if (!hasExitMarker) {
            String fallbackDetail = detail;
            if (fallbackDetail.isEmpty()) {
                fallbackDetail = commandResult.isOutputTruncated()
                        ? "状态脚本输出过长且未读到退出码"
                        : "状态脚本未返回退出码";
            }
            return new InstanceStatusProbe(INSTANCE_STATUS_UNKNOWN, fallbackDetail);
        }
        return new InstanceStatusProbe(INSTANCE_STATUS_UNKNOWN, detail.isEmpty()
                ? "状态脚本返回退出码(未知语义): " + exitCode
                : detail);
    }

    /**
     * 判断状态详情是否表达“已停止/未运行”。
     *
     * @param normalizedDetail 小写状态详情
     * @return true 表示已停止
     */
    private boolean isStoppedDetail(String normalizedDetail) {
        return normalizedDetail.contains("not running")
                || normalizedDetail.contains("stopped")
                || normalizedDetail.contains("not started")
                || normalizedDetail.contains("isn't running")
                || normalizedDetail.contains("no process")
                || normalizedDetail.contains("未运行")
                || normalizedDetail.contains("已停止")
                || normalizedDetail.contains("未启动")
                || normalizedDetail.contains("进程不存在")
                || normalizedDetail.contains("没有运行");
    }

    /**
     * 判断状态详情是否表达“运行中”。
     *
     * @param normalizedDetail 小写状态详情
     * @return true 表示运行中
     */
    private boolean isRunningDetail(String normalizedDetail) {
        if (isStoppedDetail(normalizedDetail)) {
            return false;
        }
        return normalizedDetail.contains("running")
                || normalizedDetail.contains("started")
                || normalizedDetail.contains("is running")
                || normalizedDetail.contains("pid=")
                || normalizedDetail.contains("启动中")
                || normalizedDetail.contains("已启动")
                || normalizedDetail.contains("运行中");
    }

    /**
     * 将状态值映射为中文文案。
     *
     * @param status 状态值
     * @return 中文文案
     */
    private String toStatusText(String status) {
        if (INSTANCE_STATUS_RUNNING.equals(status)) {
            return "运行中";
        }
        if (INSTANCE_STATUS_STOPPED.equals(status)) {
            return "已停止";
        }
        return "未知";
    }

    /**
     * 安静休眠，忽略中断异常并恢复中断标记。
     *
     * @param millis 休眠毫秒
     */
    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 解析命令退出码文本。
     *
     * @param raw 原始退出码文本
     * @return 退出码，解析失败时按 1 处理
     */
    private int parseExitCode(String raw) {
        try {
            return Integer.parseInt(trimToEmpty(raw));
        } catch (Exception ex) {
            return 1;
        }
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
        return executeCommandForResult(target, command, maxLines, "").getLines();
    }

    /**
     * 执行远程命令并读取输出结果。
     * <p>
     * 该方法支持“限量采集 + 标记行探测”：
     * 1. 当未指定标记时，达到 maxLines 即停止读取，保持历史行为；<br>
     * 2. 当指定 markerPrefix 时，超过 maxLines 后继续探测退出标记，避免因输出截断导致状态误判。<br>
     * 返回结果中会携带是否截断标志，便于上层给出更准确提示。
     * </p>
     *
     * @param target       目标服务节点
     * @param command      待执行命令
     * @param maxLines     最大采集行数
     * @param markerPrefix 标记前缀（可为空）
     * @return 命令读取结果
     */
    private CommandReadResult executeCommandForResult(LogTarget target,
                                                      String command,
                                                      int maxLines,
                                                      String markerPrefix) {
        List<String> lines = new ArrayList<>();
        boolean outputTruncated = false;
        boolean markerDetected = false;
        String normalizedMarker = trimToEmpty(markerPrefix);
        boolean detectMarker = !normalizedMarker.isEmpty();
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
                    String trimmed = trimToEmpty(line);
                    boolean markerLine = detectMarker && trimmed.startsWith(normalizedMarker);
                    if (markerLine) {
                        markerDetected = true;
                    }
                    if (lines.size() < maxLines || markerLine) {
                        lines.add(line);
                    } else {
                        outputTruncated = true;
                    }
                    if (!detectMarker && lines.size() >= maxLines) {
                        break;
                    }
                    if (detectMarker && markerDetected && outputTruncated) {
                        break;
                    }
                }
            }
            return new CommandReadResult(lines, outputTruncated, markerDetected);
        } catch (Exception ex) {
            throw new IllegalStateException("执行远程命令失败: " + target.displayName() + " - " + ex.getMessage(), ex);
        } finally {
            disconnectQuietly(channel);
            disconnectQuietly(session);
        }
    }

    /**
     * 命令读取结果。
     */
    private static class CommandReadResult {

        /**
         * 已读取输出行。
         */
        private final List<String> lines;

        /**
         * 是否发生输出截断。
         */
        private final boolean outputTruncated;

        /**
         * 是否命中标记行。
         */
        private final boolean markerDetected;

        /**
         * 构造方法。
         *
         * @param lines           输出行
         * @param outputTruncated 是否截断
         * @param markerDetected  是否命中标记
         */
        private CommandReadResult(List<String> lines, boolean outputTruncated, boolean markerDetected) {
            this.lines = lines;
            this.outputTruncated = outputTruncated;
            this.markerDetected = markerDetected;
        }

        /**
         * 获取输出行。
         *
         * @return 输出行
         */
        private List<String> getLines() {
            return lines;
        }

        /**
         * 判断是否输出截断。
         *
         * @return true 表示截断
         */
        private boolean isOutputTruncated() {
            return outputTruncated;
        }

        /**
         * 判断是否命中标记行。
         *
         * @return true 表示命中
         */
        private boolean isMarkerDetected() {
            return markerDetected;
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
        snapshot.setCanManage(hasManageCommandFile(target));
        snapshot.setInstanceStatus(hasManageCommandFile(target) ? INSTANCE_STATUS_UNKNOWN : INSTANCE_STATUS_UNCONFIGURED);
        snapshot.setInstanceStatusDetail(hasManageCommandFile(target) ? "检测中" : "未配置运维脚本");
        snapshot.setUpdatedAt(System.currentTimeMillis());
        snapshot.setStatus("error");
        snapshot.setMessage("采集中");
        return snapshot;
    }

    /**
     * 判断目标实例是否已配置运维脚本。
     *
     * @param target 目标实例
     * @return true 表示可执行启停操作
     */
    private boolean hasManageCommandFile(LogTarget target) {
        if (target == null || target.getServerNode() == null) {
            return false;
        }
        return !trimToEmpty(target.getServerNode().getManageCommandFile()).isEmpty();
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
     * 实例状态探测结果。
     */
    private static class InstanceStatusProbe {

        /**
         * 状态值（running/stopped/unknown）。
         */
        private final String status;

        /**
         * 状态详情文本。
         */
        private final String detail;

        /**
         * 构造方法。
         *
         * @param status 状态值
         * @param detail 状态详情
         */
        private InstanceStatusProbe(String status, String detail) {
            this.status = status;
            this.detail = detail;
        }

        /**
         * 是否运行中。
         *
         * @return true 表示运行中
         */
        private boolean isRunning() {
            return INSTANCE_STATUS_RUNNING.equals(status);
        }

        /**
         * 是否已停止。
         *
         * @return true 表示已停止
         */
        private boolean isStopped() {
            return INSTANCE_STATUS_STOPPED.equals(status);
        }

        /**
         * 是否未知状态。
         *
         * @return true 表示未知
         */
        private boolean isUnknown() {
            return !isRunning() && !isStopped();
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
