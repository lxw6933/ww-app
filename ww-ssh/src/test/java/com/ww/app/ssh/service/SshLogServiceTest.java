package com.ww.app.ssh.service;

import com.ww.app.ssh.config.LogPanelProperties;
import com.ww.app.ssh.model.ConcurrentStreamUsageSnapshot;
import com.ww.app.ssh.model.LogStreamRequest;
import com.ww.app.ssh.model.LogTarget;
import com.ww.app.ssh.service.support.LogLineFilterMatcher;
import com.ww.app.ssh.service.support.SshCommandBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * {@link SshLogService} cat 快照扫描窗口策略测试。
 * <p>
 * 该测试聚焦“过滤场景自动扩大扫描窗口”逻辑，
 * 保障小行数配置（如 200）在内容过滤时不会因扫描窗口过小而误判为无命中。
 * </p>
 */
class SshLogServiceTest {

    /**
     * 被测对象。
     */
    private final SshLogService sshLogService =
            new SshLogService(new SshCommandBuilder(), new LogLineFilterMatcher());

    /**
     * 无过滤条件时应保持历史行为：扫描窗口等于请求行数。
     *
     * @throws Exception 反射调用异常
     */
    @Test
    void shouldKeepRequestedWindowWhenNoFilterRules() throws Exception {
        int scanLines = invokeResolveCatScanLines(200, Collections.emptyList());
        Assertions.assertEquals(200, scanLines);
    }

    /**
     * 有过滤条件且请求行数较小时，应按倍数放大扫描窗口。
     *
     * @throws Exception 反射调用异常
     */
    @Test
    void shouldEnlargeScanWindowWhenFilterRulesPresent() throws Exception {
        int scanLines = invokeResolveCatScanLines(200, Collections.singletonList(rule("include", "ERROR")));
        Assertions.assertEquals(1000, scanLines);
    }

    /**
     * 有过滤条件且放大后超上限时，应限制在 5000 行以内。
     *
     * @throws Exception 反射调用异常
     */
    @Test
    void shouldCapScanWindowAtUpperBoundWhenFilterRulesPresent() throws Exception {
        int scanLines = invokeResolveCatScanLines(1200, Collections.singletonList(rule("include", "ERROR")));
        Assertions.assertEquals(5000, scanLines);
    }

    /**
     * 校验包含规则识别逻辑，确保包含条件触发全文件预筛路径。
     *
     * @throws Exception 反射调用异常
     */
    @Test
    void shouldRecognizeIncludeRule() throws Exception {
        boolean includeMatched = invokeHasIncludeFilterRule(Collections.singletonList(rule("include", "ERROR")));
        boolean onlyExclude = invokeHasIncludeFilterRule(Collections.singletonList(rule("exclude", "DEBUG")));
        Assertions.assertTrue(includeMatched);
        Assertions.assertFalse(onlyExclude);
    }

    /**
     * 校验默认文件选择逻辑：info 日志优先于其他类型日志。
     *
     * @throws Exception 反射调用异常
     */
    @Test
    void shouldPreferInfoFileWhenResolvingDefaultLogFile() throws Exception {
        String resolved = invokeResolvePreferredLogFilePath(Arrays.asList(
                "/data/logs/service-job.log",
                "/data/logs/service-error.log",
                "/data/logs/service-info.log"
        ));
        Assertions.assertEquals("/data/logs/service-info.log", resolved);
    }

    /**
     * 校验同优先级文件的比较逻辑：按完整路径倒序选择。
     *
     * @throws Exception 反射调用异常
     */
    @Test
    void shouldChooseDescendingPathWhenPriorityIsSame() throws Exception {
        String resolved = invokeResolvePreferredLogFilePath(Arrays.asList(
                "/data/logs/service-debug-20260308.log",
                "/data/logs/service-debug-20260309.log"
        ));
        Assertions.assertEquals("/data/logs/service-debug-20260309.log", resolved);
    }

    /**
     * 校验候选为空时返回空字符串，供上层执行 latest 回退逻辑。
     *
     * @throws Exception 反射调用异常
     */
    @Test
    void shouldReturnEmptyWhenNoCandidateLogFiles() throws Exception {
        String resolved = invokeResolvePreferredLogFilePath(Collections.emptyList());
        Assertions.assertEquals("", resolved);
    }

    /**
     * 校验日志文件列表缓存键会同时区分节点身份与日志路径，避免不同来源误复用。
     *
     * @throws Exception 反射调用异常
     */
    @Test
    void shouldSeparateLogFileCacheByNodeAndPath() throws Exception {
        LogPanelProperties.ServerNode nodeA = buildServerNode("10.0.0.1", 22, "app", "pwd-a");
        LogPanelProperties.ServerNode nodeB = buildServerNode("10.0.0.2", 22, "app", "pwd-a");
        String keyA = invokeBuildLogFileCacheKey(nodeA, "/data/logs");
        String keyB = invokeBuildLogFileCacheKey(nodeA, "/data/logs/archive");
        String keyC = invokeBuildLogFileCacheKey(nodeB, "/data/logs");
        Assertions.assertNotEquals(keyA, keyB);
        Assertions.assertNotEquals(keyA, keyC);
    }

    /**
     * 校验 SSH 会话缓存键会区分凭证摘要，避免同主机改密后误用旧会话。
     *
     * @throws Exception 反射调用异常
     */
    @Test
    void shouldSeparateSessionCacheKeyByCredentialDigest() throws Exception {
        LogPanelProperties.ServerNode nodeA = buildServerNode("10.0.0.1", 22, "app", "pwd-a");
        LogPanelProperties.ServerNode nodeB = buildServerNode("10.0.0.1", 22, "app", "pwd-b");
        String keyA = invokeBuildSessionCacheKey(nodeA);
        String keyB = invokeBuildSessionCacheKey(nodeB);
        Assertions.assertNotEquals(keyA, keyB);
    }

    /**
     * 校验当 logPath 本身就是单文件时，允许精确命中该文件。
     *
     * @throws Exception 反射调用异常
     */
    @Test
    void shouldAllowRequestedPathWhenConfiguredAsSingleFile() throws Exception {
        String resolved = invokeValidateRequestedLogFilePath(
                "/data/logs/app.log",
                Collections.emptyList(),
                "/data/logs/app.log");
        Assertions.assertEquals("/data/logs/app.log", resolved);
    }

    /**
     * 校验目录模式下仅允许读取候选文件列表中的日志文件。
     *
     * @throws Exception 反射调用异常
     */
    @Test
    void shouldAllowRequestedPathWhenItExistsInCandidateFiles() throws Exception {
        String resolved = invokeValidateRequestedLogFilePath(
                "/data/logs",
                Arrays.asList("/data/logs/app.log", "/data/logs/app-error.log"),
                "/data/logs/app-error.log");
        Assertions.assertEquals("/data/logs/app-error.log", resolved);
    }

    /**
     * 校验目录模式下会拒绝不在候选列表内的任意文件。
     *
     * @throws Exception 反射调用异常
     */
    @Test
    void shouldRejectRequestedPathOutsideCandidateFiles() throws Exception {
        IllegalArgumentException exception = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> invokeValidateRequestedLogFilePath(
                        "/data/logs",
                        Collections.singletonList("/data/logs/app.log"),
                        "/etc/passwd"));
        Assertions.assertTrue(exception.getMessage().contains("不在当前服务允许范围内"));
    }

    /**
     * 校验包含路径回退片段的 filePath 会被拒绝，避免越权读取。
     *
     */
    @Test
    void shouldRejectRequestedPathContainingParentTraversal() {
        IllegalArgumentException exception = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> invokeValidateRequestedLogFilePath(
                        "/data/logs",
                        Collections.singletonList("/data/logs/app.log"),
                        "/data/logs/../secret.txt"));
        Assertions.assertTrue(exception.getMessage().contains("路径非法"));
    }

    /**
     * 校验未配置或配置非法时，会回退到默认实时流上限 48。
     *
     * @throws Exception 反射调用异常
     */
    @Test
    void shouldFallbackToDefaultMaxConcurrentStreamsWhenConfigInvalid() throws Exception {
        LogPanelProperties properties = new LogPanelProperties();
        properties.setMaxConcurrentStreams(0);
        SshLogService service = new SshLogService(new SshCommandBuilder(), new LogLineFilterMatcher(), properties);
        Assertions.assertEquals(48, invokeReadMaxConcurrentStreams(service));
    }

    /**
     * 校验可通过配置覆盖实时流上限。
     *
     * @throws Exception 反射调用异常
     */
    @Test
    void shouldUseConfiguredMaxConcurrentStreams() throws Exception {
        LogPanelProperties properties = new LogPanelProperties();
        properties.setMaxConcurrentStreams(96);
        SshLogService service = new SshLogService(new SshCommandBuilder(), new LogLineFilterMatcher(), properties);
        Assertions.assertEquals(96, invokeReadMaxConcurrentStreams(service));
    }

    /**
     * 校验并发流概览会按来源 IP 分组，并汇总总量、剩余量与会话数。
     *
     * @throws Exception 反射调用异常
     */
    @Test
    void shouldGroupConcurrentStreamUsageByClientIp() throws Exception {
        LogPanelProperties properties = new LogPanelProperties();
        properties.setMaxConcurrentStreams(4);
        SshLogService service = new SshLogService(new SshCommandBuilder(), new LogLineFilterMatcher(), properties);

        invokeRegisterActiveStream(service, "stream-1", "10.0.0.10", "session-a",
                buildLogTarget("mall", "prod", "mall-basic@node1", "192.168.1.11"),
                "/data/logs/mall-basic-info.log", "tail");
        invokeRegisterActiveStream(service, "stream-2", "10.0.0.10", "session-a",
                buildLogTarget("mall", "prod", "mall-basic@node2", "192.168.1.12"),
                "/data/logs/mall-basic-error.log", "tail");
        invokeRegisterActiveStream(service, "stream-3", "10.0.0.11", "session-b",
                buildLogTarget("mall", "prod", "mall-order@node1", "192.168.1.21"),
                "/data/logs/mall-order-info.log", "tail");

        ConcurrentStreamUsageSnapshot snapshot = service.getConcurrentStreamUsageSnapshot();
        Assertions.assertEquals(4, snapshot.getMaxConcurrentStreams());
        Assertions.assertEquals(3, snapshot.getActiveConcurrentStreams());
        Assertions.assertEquals(1, snapshot.getRemainingConcurrentStreams());
        Assertions.assertEquals(2, snapshot.getActiveClientIpCount());
        Assertions.assertEquals(2, snapshot.getIpGroups().size());
        Assertions.assertEquals("10.0.0.10", snapshot.getIpGroups().get(0).getClientIp());
        Assertions.assertEquals(2, snapshot.getIpGroups().get(0).getStreamCount());
        Assertions.assertEquals(1, snapshot.getIpGroups().get(0).getSessionCount());
        Assertions.assertEquals("10.0.0.11", snapshot.getIpGroups().get(1).getClientIp());
        Assertions.assertEquals(1, snapshot.getIpGroups().get(1).getStreamCount());
        Assertions.assertEquals(1, snapshot.getIpGroups().get(1).getSessionCount());

        List<String> services = snapshot.getIpGroups().get(0).getStreams().stream()
                .map(ConcurrentStreamUsageSnapshot.StreamUsageItem::getService)
                .collect(Collectors.toList());
        Assertions.assertTrue(services.contains("mall-basic@node1"));
        Assertions.assertTrue(services.contains("mall-basic@node2"));
    }

    /**
     * 校验存在活跃执行通道时，空闲会话清理不会误回收仍在 tail 的会话；
     * 当通道释放后，再次清理应恢复正常回收。
     *
     * @throws Exception 反射调用异常
     */
    @Test
    void shouldSkipIdleCleanupWhileSessionStillHasActiveChannels() throws Exception {
        SshLogService service = new SshLogService(new SshCommandBuilder(), new LogLineFilterMatcher());
        LogPanelProperties.ServerNode node = buildServerNode("10.0.0.1", 22, "app", "pwd");
        Object sessionHolder = createSessionHolder("cache-key-1", node);
        long expiredAt = System.currentTimeMillis() - 10 * 60_000L;
        invokeSessionTouch(sessionHolder, expiredAt);
        invokeSessionRetainChannel(sessionHolder, expiredAt);
        invokeSessionCache(service).put("cache-key-1", sessionHolder);

        invokeSetLastSessionCleanupAt(service, 0L);
        invokeCleanupIdleSessionsIfNeeded(service, System.currentTimeMillis());
        Assertions.assertTrue(invokeSessionCache(service).containsKey("cache-key-1"));

        invokeSessionReleaseChannel(sessionHolder, System.currentTimeMillis());
        invokeSetLastSessionCleanupAt(service, 0L);
        invokeCleanupIdleSessionsIfNeeded(service, System.currentTimeMillis());
        Assertions.assertFalse(invokeSessionCache(service).containsKey("cache-key-1"));
    }

    /**
     * 校验执行通道上下文的关闭逻辑具备幂等性，避免重复关闭导致活跃通道计数被多次扣减。
     *
     * @throws Exception 反射调用异常
     */
    @Test
    void shouldReleaseActiveChannelCountOnlyOnceWhenContextClosedRepeatedly() throws Exception {
        Object sessionHolder = createSessionHolder("cache-key-2", buildServerNode("10.0.0.2", 22, "app", "pwd"));
        invokeSessionRetainChannel(sessionHolder, System.currentTimeMillis());
        Object channelContext = createExecChannelContext(sessionHolder);

        invokeExecChannelContextClose(channelContext);
        invokeExecChannelContextClose(channelContext);

        Assertions.assertFalse(invokeSessionHasActiveChannels(sessionHolder));
    }

    /**
     * 通过反射调用私有方法，验证 cat 扫描窗口计算结果。
     *
     * @param requestedLines 请求展示行数
     * @param filterRules    过滤规则
     * @return 实际扫描行数
     * @throws Exception 反射调用异常
     */
    private int invokeResolveCatScanLines(int requestedLines, List<LogStreamRequest.FilterRule> filterRules)
            throws Exception {
        Method method = SshLogService.class
                .getDeclaredMethod("resolveCatScanLines", int.class, List.class);
        method.setAccessible(true);
        return (Integer) method.invoke(sshLogService, requestedLines, filterRules);
    }

    /**
     * 通过反射调用“是否包含 include 规则”判定方法。
     *
     * @param filterRules 规则集合
     * @return true 表示包含 include 规则
     * @throws Exception 反射调用异常
     */
    private boolean invokeHasIncludeFilterRule(List<LogStreamRequest.FilterRule> filterRules) throws Exception {
        Method method = SshLogService.class.getDeclaredMethod("hasIncludeFilterRule", List.class);
        method.setAccessible(true);
        return (Boolean) method.invoke(sshLogService, filterRules);
    }

    /**
     * 通过反射调用默认文件选择方法。
     *
     * @param candidates 候选文件路径
     * @return 选中的默认路径
     * @throws Exception 反射调用异常
     */
    private String invokeResolvePreferredLogFilePath(List<String> candidates) throws Exception {
        Method method = SshLogService.class.getDeclaredMethod("resolvePreferredLogFilePath", List.class);
        method.setAccessible(true);
        return (String) method.invoke(sshLogService, candidates);
    }

    /**
     * 通过反射调用日志文件缓存键构建方法。
     *
     * @param node    节点配置
     * @param logPath 日志路径
     * @return 缓存键
     * @throws Exception 反射调用异常
     */
    private String invokeBuildLogFileCacheKey(LogPanelProperties.ServerNode node, String logPath) throws Exception {
        Method method = SshLogService.class.getDeclaredMethod(
                "buildLogFileCacheKey", LogPanelProperties.ServerNode.class, String.class);
        method.setAccessible(true);
        return (String) method.invoke(sshLogService, node, logPath);
    }

    /**
     * 通过反射调用 SSH 会话缓存键构建方法。
     *
     * @param node 节点配置
     * @return 会话缓存键
     * @throws Exception 反射调用异常
     */
    private String invokeBuildSessionCacheKey(LogPanelProperties.ServerNode node) throws Exception {
        Method method = SshLogService.class.getDeclaredMethod(
                "buildSessionCacheKey", LogPanelProperties.ServerNode.class);
        method.setAccessible(true);
        return (String) method.invoke(sshLogService, node);
    }

    /**
     * 通过反射调用日志文件路径校验方法。
     *
     * @param configuredPath 配置日志目录或文件
     * @param candidateFiles 候选日志文件
     * @param requestedPath  前端请求文件
     * @return 规范化后的文件路径
     * @throws Exception 反射调用异常
     */
    private String invokeValidateRequestedLogFilePath(String configuredPath,
                                                      List<String> candidateFiles,
                                                      String requestedPath) throws Exception {
        Method method = SshLogService.class.getDeclaredMethod(
                "validateRequestedLogFilePath", String.class, List.class, String.class);
        method.setAccessible(true);
        try {
            return (String) method.invoke(sshLogService, configuredPath, candidateFiles, requestedPath);
        } catch (Exception ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof Exception) {
                throw (Exception) cause;
            }
            throw ex;
        }
    }

    /**
     * 通过反射读取实时流上限字段。
     *
     * @param service 被测服务
     * @return 实时流上限
     * @throws Exception 反射调用异常
     */
    private int invokeReadMaxConcurrentStreams(SshLogService service) throws Exception {
        java.lang.reflect.Field field = SshLogService.class.getDeclaredField("maxConcurrentStreams");
        field.setAccessible(true);
        return (Integer) field.get(service);
    }

    /**
     * 读取 SSH 会话缓存。
     *
     * @param service 被测服务
     * @return 会话缓存映射
     * @throws Exception 反射调用异常
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> invokeSessionCache(SshLogService service) throws Exception {
        Field field = SshLogService.class.getDeclaredField("sessionCache");
        field.setAccessible(true);
        return (Map<String, Object>) field.get(service);
    }

    /**
     * 重置上次会话清理时间，便于在单测中重复触发清理逻辑。
     *
     * @param service 被测服务
     * @param value 目标时间戳
     * @throws Exception 反射调用异常
     */
    private void invokeSetLastSessionCleanupAt(SshLogService service, long value) throws Exception {
        Field field = SshLogService.class.getDeclaredField("lastSessionCleanupAt");
        field.setAccessible(true);
        ((java.util.concurrent.atomic.AtomicLong) field.get(service)).set(value);
    }

    /**
     * 通过反射调用会话清理逻辑。
     *
     * @param service 被测服务
     * @param currentTimeMillis 当前时间
     * @throws Exception 反射调用异常
     */
    private void invokeCleanupIdleSessionsIfNeeded(SshLogService service, long currentTimeMillis) throws Exception {
        Method method = SshLogService.class.getDeclaredMethod("cleanupIdleSessionsIfNeeded", long.class);
        method.setAccessible(true);
        method.invoke(service, currentTimeMillis);
    }

    /**
     * 构造私有的 SessionHolder。
     *
     * @param cacheKey 会话缓存键
     * @param node 节点配置
     * @return SessionHolder 实例
     * @throws Exception 反射调用异常
     */
    private Object createSessionHolder(String cacheKey, LogPanelProperties.ServerNode node) throws Exception {
        Class<?> clazz = Class.forName("com.ww.app.ssh.service.SshLogService$SessionHolder");
        Constructor<?> constructor = clazz.getDeclaredConstructor(String.class, LogPanelProperties.ServerNode.class);
        constructor.setAccessible(true);
        return constructor.newInstance(cacheKey, node);
    }

    /**
     * 构造私有的 ExecChannelContext。
     *
     * @param sessionHolder 会话持有者
     * @return 执行通道上下文
     * @throws Exception 反射调用异常
     */
    private Object createExecChannelContext(Object sessionHolder) throws Exception {
        Class<?> sessionHolderClass = Class.forName("com.ww.app.ssh.service.SshLogService$SessionHolder");
        Class<?> contextClass = Class.forName("com.ww.app.ssh.service.SshLogService$ExecChannelContext");
        Constructor<?> constructor = contextClass.getDeclaredConstructor(
                sessionHolderClass, com.jcraft.jsch.ChannelExec.class, java.io.InputStream.class);
        constructor.setAccessible(true);
        return constructor.newInstance(sessionHolder, null, null);
    }

    /**
     * 回写 SessionHolder 的最近访问时间。
     *
     * @param sessionHolder 会话持有者
     * @param currentTimeMillis 当前时间
     * @throws Exception 反射调用异常
     */
    private void invokeSessionTouch(Object sessionHolder, long currentTimeMillis) throws Exception {
        Method method = sessionHolder.getClass().getDeclaredMethod("touch", long.class);
        method.setAccessible(true);
        method.invoke(sessionHolder, currentTimeMillis);
    }

    /**
     * 模拟执行通道占用会话。
     *
     * @param sessionHolder 会话持有者
     * @param currentTimeMillis 当前时间
     * @throws Exception 反射调用异常
     */
    private void invokeSessionRetainChannel(Object sessionHolder, long currentTimeMillis) throws Exception {
        Method method = sessionHolder.getClass().getDeclaredMethod("retainChannel", long.class);
        method.setAccessible(true);
        method.invoke(sessionHolder, currentTimeMillis);
    }

    /**
     * 模拟执行通道释放会话。
     *
     * @param sessionHolder 会话持有者
     * @param currentTimeMillis 当前时间
     * @throws Exception 反射调用异常
     */
    private void invokeSessionReleaseChannel(Object sessionHolder, long currentTimeMillis) throws Exception {
        Method method = sessionHolder.getClass().getDeclaredMethod("releaseChannel", long.class);
        method.setAccessible(true);
        method.invoke(sessionHolder, currentTimeMillis);
    }

    /**
     * 判断会话上是否仍有活跃执行通道。
     *
     * @param sessionHolder 会话持有者
     * @return true 表示仍存在活跃通道
     * @throws Exception 反射调用异常
     */
    private boolean invokeSessionHasActiveChannels(Object sessionHolder) throws Exception {
        Method method = sessionHolder.getClass().getDeclaredMethod("hasActiveChannels");
        method.setAccessible(true);
        return (Boolean) method.invoke(sessionHolder);
    }

    /**
     * 触发执行通道上下文关闭。
     *
     * @param channelContext 执行通道上下文
     * @throws Exception 反射调用异常
     */
    private void invokeExecChannelContextClose(Object channelContext) throws Exception {
        Method method = channelContext.getClass().getDeclaredMethod("close");
        method.setAccessible(true);
        method.invoke(channelContext);
    }

    /**
     * 通过反射登记一条活跃并发流。
     *
     * @param service 被测服务
     * @param streamId 流唯一标识
     * @param clientIp 客户端来源 IP
     * @param sessionId WebSocket 会话 ID
     * @param target 目标服务
     * @param filePath 实际读取的文件路径
     * @param readMode 读取模式
     * @throws Exception 反射调用异常
     */
    private void invokeRegisterActiveStream(SshLogService service,
                                            String streamId,
                                            String clientIp,
                                            String sessionId,
                                            LogTarget target,
                                            String filePath,
                                            String readMode) throws Exception {
        Method method = SshLogService.class.getDeclaredMethod("registerActiveStream",
                String.class, String.class, String.class, LogTarget.class, String.class, String.class);
        method.setAccessible(true);
        method.invoke(service, streamId, clientIp, sessionId, target, filePath, readMode);
    }

    /**
     * 构建过滤规则对象。
     *
     * @param type 规则类型
     * @param data 规则内容
     * @return 过滤规则
     */
    private LogStreamRequest.FilterRule rule(String type, String data) {
        LogStreamRequest.FilterRule filterRule = new LogStreamRequest.FilterRule();
        filterRule.setType(type);
        filterRule.setData(data);
        return filterRule;
    }

    /**
     * 构建测试节点配置。
     *
     * @param host     主机地址
     * @param port     SSH 端口
     * @param username 用户名
     * @param password 密码
     * @return 节点配置
     */
    private LogPanelProperties.ServerNode buildServerNode(String host, Integer port, String username, String password) {
        LogPanelProperties.ServerNode serverNode = new LogPanelProperties.ServerNode();
        serverNode.setHost(host);
        serverNode.setPort(port);
        serverNode.setUsername(username);
        serverNode.setPassword(password);
        return serverNode;
    }

    /**
     * 构建测试用日志目标。
     *
     * @param project 项目名称
     * @param env 环境名称
     * @param service 服务名称
     * @param host 目标主机地址
     * @return 日志目标
     */
    private LogTarget buildLogTarget(String project, String env, String service, String host) {
        return new LogTarget(project, env, service, buildServerNode(host, 22, "app", "pwd"));
    }
}
