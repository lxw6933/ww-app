package com.ww.app.ssh.service;

import com.ww.app.common.utils.ThreadUtil;
import com.ww.app.ssh.model.HostMetricSnapshot;
import com.ww.app.ssh.model.LogTarget;
import com.ww.app.ssh.model.MetricsActiveRegisterRequest;
import com.ww.app.ssh.model.MetricsSnapshotResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * 主机指标快照服务。
 * <p>
 * 单体部署场景下，使用本地内存维护“活跃维度”与“指标快照”：
 * 1. 前端仅负责登记活跃维度与读取缓存，不再直接驱动 SSH 采集；<br>
 * 2. 后端定时任务按来源类型做分频刷新：JVM 视图 3 秒、左侧指标面板 15 秒；<br>
 * 3. 首次活跃登记时异步预热一次快照，缩短首屏等待时间；<br>
 * 4. 响应中显式返回 ready/stale/lastError，便于前端区分准备中、失败与旧缓存。<br>
 * </p>
 */
@Slf4j
@Service
public class MetricsSnapshotService {

    /**
     * 冷启动读取时等待在途预热结果的最长时长（毫秒）。
     * <p>
     * 仅在“当前还没有可用快照，但后台已在刷新”时短暂等待一次，
     * 提升首次进入页面时的首屏命中率，避免必须等到下一轮前端轮询。
     * </p>
     */
    private static final long COLD_READ_WAIT_MS = 1200L;

    /**
     * 活跃维度保活时长（毫秒）。
     * <p>
     * 前端心跳会持续续期；超过该时间未续期则视为页面已离开。
     * </p>
     */
    private static final long ACTIVE_KEY_TTL_MS = 45_000L;

    /**
     * 左侧主机指标面板刷新间隔（毫秒）。
     */
    private static final long PANEL_REFRESH_INTERVAL_MS = 15_000L;

    /**
     * JVM 监控视图刷新间隔（毫秒）。
     */
    private static final long JVM_REFRESH_INTERVAL_MS = 3_000L;

    /**
     * 非活跃快照保留时长（毫秒）。
     * <p>
     * 维度失活后继续保留一段时间，便于短时间内页面往返切换时复用最近一次快照。
     * </p>
     */
    private static final long SNAPSHOT_RETENTION_MS = 5 * 60_000L;

    /**
     * 单次并行采集的超时时间（秒）。
     */
    private static final long COLLECT_TIMEOUT_SECONDS = 30L;

    /**
     * 单目标 SSH 采集线程池核心线程数。
     */
    private static final int METRICS_CORE_THREADS = 8;

    /**
     * 单目标 SSH 采集线程池最大线程数。
     */
    private static final int METRICS_MAX_THREADS = 32;

    /**
     * 维度级刷新编排线程池核心线程数。
     */
    private static final int REFRESH_DISPATCH_CORE_THREADS = 1;

    /**
     * 维度级刷新编排线程池最大线程数。
     */
    private static final int REFRESH_DISPATCH_MAX_THREADS = 4;

    /**
     * 配置查询服务。
     */
    private final LogPanelQueryService logPanelQueryService;

    /**
     * SSH 日志服务。
     */
    private final SshLogService sshLogService;

    /**
     * 活跃指标维度登记表。
     */
    private final Map<String, ActiveMetricEntry> activeMetricRegistry = new ConcurrentHashMap<>();

    /**
     * 指标快照缓存。
     */
    private final Map<String, SnapshotCacheEntry> snapshotCache = new ConcurrentHashMap<>();

    /**
     * 正在刷新的维度任务表。
     * <p>
     * 用于防止调度线程重复提交同一维度的刷新任务。
     * </p>
     */
    private final Map<String, CompletableFuture<List<HostMetricSnapshot>>> inflightMap = new ConcurrentHashMap<>();

    /**
     * 维度级刷新编排线程池。
     * <p>
     * 该线程池负责“某个项目/环境/服务维度的一次完整刷新”，
     * 每次刷新内部再并行拆分到单目标 SSH 采集线程池。
     * </p>
     */
    private final ExecutorService refreshDispatcher = ThreadUtil.initThreadPoolExecutor(
            "metrics-refresh-dispatcher",
            REFRESH_DISPATCH_CORE_THREADS,
            REFRESH_DISPATCH_MAX_THREADS,
            60,
            TimeUnit.SECONDS,
            64,
            new ThreadPoolExecutor.CallerRunsPolicy()
    );

    /**
     * 单目标 SSH 采集线程池。
     */
    private final ExecutorService metricsExecutor = ThreadUtil.initThreadPoolExecutor(
            "server-metrics",
            METRICS_CORE_THREADS,
            METRICS_MAX_THREADS,
            60,
            TimeUnit.SECONDS,
            256,
            new ThreadPoolExecutor.CallerRunsPolicy()
    );

    /**
     * 构造方法。
     *
     * @param logPanelQueryService 配置查询服务
     * @param sshLogService SSH 日志服务
     */
    public MetricsSnapshotService(LogPanelQueryService logPanelQueryService,
                                  SshLogService sshLogService) {
        this.logPanelQueryService = logPanelQueryService;
        this.sshLogService = sshLogService;
    }

    /**
     * 注册活跃指标维度。
     * <p>
     * 首次登记时会做配置有效性校验，并在无缓存时异步预热一次快照，
     * 但不会阻塞当前请求线程等待 SSH 采集结果。
     * </p>
     *
     * @param project 项目
     * @param env 环境
     * @param service 服务
     * @param source 来源类型
     */
    public void registerActiveKey(String project, String env, String service, String source) {
        MetricsCacheKey cacheKey = normalizeCacheKey(project, env, service);
        String normalizedSource = normalizeSource(source);
        String key = cacheKey.toCacheKey();
        long now = System.currentTimeMillis();

        ActiveMetricEntry existing = activeMetricRegistry.get(key);
        if (existing != null) {
            boolean activated = existing.touchSource(normalizedSource, now);
            if (activated) {
                log.info("主机指标活跃来源恢复: key={}, source={}", key, normalizedSource);
            }
            triggerWarmupIfNecessary(existing, "register-" + normalizedSource);
            return;
        }

        // 首次登记时先校验配置，避免前端长期停留在“准备中”状态。
        logPanelQueryService.resolveTargets(cacheKey.getProject(), cacheKey.getEnv(), cacheKey.getService());

        ActiveMetricEntry newEntry = new ActiveMetricEntry(
                cacheKey.getProject(), cacheKey.getEnv(), cacheKey.getService());
        newEntry.touchSource(normalizedSource, now);
        ActiveMetricEntry previous = activeMetricRegistry.putIfAbsent(key, newEntry);
        ActiveMetricEntry activeEntry = previous == null ? newEntry : previous;
        if (previous != null) {
            boolean activated = previous.touchSource(normalizedSource, now);
            if (activated) {
                log.info("主机指标活跃来源恢复: key={}, source={}", key, normalizedSource);
            }
        } else {
            log.info("主机指标活跃维度注册: key={}, source={}", key, normalizedSource);
        }
        triggerWarmupIfNecessary(activeEntry, "register-" + normalizedSource);
    }

    /**
     * 读取某个维度的指标快照。
     * <p>
     * 该方法只读本地缓存，不执行同步 SSH 采集。
     * </p>
     *
     * @param project 项目
     * @param env 环境
     * @param service 服务
     * @return 带状态的指标快照响应
     */
    public MetricsSnapshotResponse readSnapshot(String project, String env, String service) {
        MetricsCacheKey cacheKey = normalizeCacheKey(project, env, service);
        String key = cacheKey.toCacheKey();
        long now = System.currentTimeMillis();
        ActiveMetricEntry activeEntry = activeMetricRegistry.get(key);
        SnapshotCacheEntry snapshotEntry = resolveReadableSnapshot(key);

        MetricsSnapshotResponse response = new MetricsSnapshotResponse();
        if (snapshotEntry == null) {
            response.setReady(false);
            response.setStale(false);
            response.setMetrics(new ArrayList<>());
            return response;
        }

        response.setReady(snapshotEntry.isReady());
        response.setStale(snapshotEntry.isStale(resolveRefreshInterval(activeEntry, now), now));
        response.setUpdatedAt(snapshotEntry.getUpdatedAtValue());
        response.setLastAttemptAt(snapshotEntry.getLastAttemptAtValue());
        response.setLastError(snapshotEntry.getLastErrorValue());
        response.setMetrics(snapshotEntry.copyMetrics());
        return response;
    }

    /**
     * 获取当前可读快照。
     * <p>
     * 若本地尚无可用快照，但该维度已有在途预热/刷新任务，
     * 则短暂等待一次后台结果，提升首次进入页面时的命中率。
     * </p>
     *
     * @param cacheKey 缓存键
     * @return 可读快照；仍未就绪时返回当前缓存值或 null
     */
    private SnapshotCacheEntry resolveReadableSnapshot(String cacheKey) {
        SnapshotCacheEntry snapshotEntry = snapshotCache.get(cacheKey);
        if (snapshotEntry != null && snapshotEntry.isReady()) {
            return snapshotEntry;
        }

        CompletableFuture<List<HostMetricSnapshot>> inflightFuture = inflightMap.get(cacheKey);
        if (inflightFuture == null) {
            return snapshotEntry;
        }

        try {
            inflightFuture.get(COLD_READ_WAIT_MS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        } catch (TimeoutException ignored) {
            // 预热仍未完成时保持非阻塞，交由前端短轮询兜底
        } catch (ExecutionException ignored) {
            // 后续从缓存条目中读取 lastError，不在这里重复抛出
        }
        SnapshotCacheEntry latest = snapshotCache.get(cacheKey);
        return latest == null ? snapshotEntry : latest;
    }

    /**
     * 刷新全部活跃维度的指标快照。
     * <p>
     * 调度线程只负责扫描与分发，不在当前线程内阻塞等待 SSH 采集完成。
     * </p>
     */
    public void refreshActiveSnapshots() {
        long now = System.currentTimeMillis();
        cleanupExpiredEntries(now);
        if (activeMetricRegistry.isEmpty()) {
            return;
        }

        int dispatched = 0;
        List<ActiveMetricEntry> activeEntries = new ArrayList<>(activeMetricRegistry.values());
        activeEntries.sort(Comparator
                .comparing(ActiveMetricEntry::getProject)
                .thenComparing(ActiveMetricEntry::getEnv)
                .thenComparing(ActiveMetricEntry::getService));

        for (ActiveMetricEntry activeEntry : activeEntries) {
            long refreshIntervalMs = activeEntry.resolveRefreshIntervalMs(now);
            if (refreshIntervalMs <= 0L) {
                continue;
            }
            SnapshotCacheEntry snapshotEntry = snapshotCache.get(activeEntry.toCacheKey());
            if (snapshotEntry != null && !snapshotEntry.shouldRefresh(now, refreshIntervalMs)) {
                continue;
            }
            if (dispatchRefresh(activeEntry, "scheduled")) {
                dispatched++;
            }
        }

        if (dispatched > 0) {
            log.debug("主机指标调度扫描完成: activeKeys={}, dispatched={}, inflight={}",
                    activeEntries.size(), dispatched, inflightMap.size());
        }
    }

    /**
     * 在首次活跃登记且尚无缓存时触发异步预热。
     *
     * @param activeEntry 活跃维度
     * @param trigger 触发来源
     */
    private void triggerWarmupIfNecessary(ActiveMetricEntry activeEntry, String trigger) {
        if (activeEntry == null) {
            return;
        }
        String key = activeEntry.toCacheKey();
        SnapshotCacheEntry snapshotEntry = snapshotCache.get(key);
        if (snapshotEntry != null && snapshotEntry.hasAttempted()) {
            return;
        }
        if (dispatchRefresh(activeEntry, trigger)) {
            log.info("主机指标快照预热已提交: key={}, trigger={}", key, trigger);
        }
    }

    /**
     * 分发单个活跃维度的刷新任务。
     *
     * @param activeEntry 活跃维度
     * @param trigger 触发来源
     * @return true 表示成功提交刷新任务
     */
    private boolean dispatchRefresh(ActiveMetricEntry activeEntry, String trigger) {
        if (activeEntry == null) {
            return false;
        }
        String cacheKey = activeEntry.toCacheKey();
        CompletableFuture<List<HostMetricSnapshot>> newFuture = new CompletableFuture<>();
        CompletableFuture<List<HostMetricSnapshot>> existingFuture = inflightMap.putIfAbsent(cacheKey, newFuture);
        if (existingFuture != null) {
            return false;
        }
        try {
            refreshDispatcher.execute(() -> refreshSnapshot(cacheKey, activeEntry, newFuture, trigger));
            return true;
        } catch (RejectedExecutionException ex) {
            inflightMap.remove(cacheKey, newFuture);
            log.warn("主机指标刷新任务被拒绝: key={}, trigger={}, error={}", cacheKey, trigger, ex.getMessage());
            return false;
        }
    }

    /**
     * 执行单个维度的快照刷新。
     *
     * @param cacheKey 缓存键
     * @param activeEntry 活跃维度
     * @param future 在途任务 Future
     * @param trigger 触发来源
     */
    private void refreshSnapshot(String cacheKey,
                                 ActiveMetricEntry activeEntry,
                                 CompletableFuture<List<HostMetricSnapshot>> future,
                                 String trigger) {
        long startMs = System.currentTimeMillis();
        SnapshotCacheEntry previous = snapshotCache.get(cacheKey);
        try {
            List<HostMetricSnapshot> metrics = doCollect(
                    activeEntry.getProject(),
                    activeEntry.getEnv(),
                    activeEntry.getService()
            );
            SnapshotCacheEntry successEntry = SnapshotCacheEntry.success(System.currentTimeMillis(), metrics);
            snapshotCache.put(cacheKey, successEntry);
            future.complete(new ArrayList<>(metrics));

            long elapsed = System.currentTimeMillis() - startMs;
            if (previous == null || !previous.isReady() || previous.hasLastError()) {
                log.info("主机指标快照刷新完成: key={}, trigger={}, size={}, elapsed={}ms",
                        cacheKey, trigger, metrics.size(), elapsed);
            } else {
                log.debug("主机指标快照刷新完成: key={}, trigger={}, size={}, elapsed={}ms",
                        cacheKey, trigger, metrics.size(), elapsed);
            }
        } catch (Exception ex) {
            String errorMessage = limitMessage(ex.getMessage());
            SnapshotCacheEntry failedEntry = SnapshotCacheEntry.failed(
                    System.currentTimeMillis(), errorMessage, previous);
            snapshotCache.put(cacheKey, failedEntry);
            future.complete(failedEntry.copyMetrics());
            log.warn("刷新主机指标缓存失败: key={}, trigger={}, elapsed={}ms, error={}",
                    cacheKey, trigger, System.currentTimeMillis() - startMs, errorMessage);
        } finally {
            inflightMap.remove(cacheKey, future);
        }
    }

    /**
     * 清理过期活跃维度与陈旧快照。
     *
     * @param currentTimeMillis 当前时间戳
     */
    private void cleanupExpiredEntries(long currentTimeMillis) {
        activeMetricRegistry.entrySet().removeIf(entry -> {
            ActiveMetricEntry value = entry.getValue();
            boolean expired = value == null || value.isExpired(currentTimeMillis);
            if (expired && value != null) {
                log.debug("主机指标活跃维度失活: key={}", value.toCacheKey());
            }
            return expired;
        });
        snapshotCache.entrySet().removeIf(entry -> {
            SnapshotCacheEntry value = entry.getValue();
            if (value == null) {
                return true;
            }
            if (activeMetricRegistry.containsKey(entry.getKey())) {
                return false;
            }
            return value.isExpired(currentTimeMillis);
        });
    }

    /**
     * 执行真正的 SSH 并行采集逻辑。
     * <p>
     * 对每个 target 提交独立任务到专用线程池，并使用统一超时等待全部任务。
     * 单个 target 采集失败或超时只会回填该节点的 error 快照，不影响其他 target。
     * </p>
     *
     * @param project 项目
     * @param env 环境
     * @param service 服务
     * @return 排序后的指标快照列表
     */
    private List<HostMetricSnapshot> doCollect(String project, String env, String service) {
        List<LogTarget> targets = logPanelQueryService.resolveTargets(project, env, service);
        if (targets == null || targets.isEmpty()) {
            return new ArrayList<>();
        }

        List<Callable<HostMetricSnapshot>> tasks = new ArrayList<>(targets.size());
        for (LogTarget target : targets) {
            tasks.add(() -> collectMetricSafely(target));
        }

        List<HostMetricSnapshot> metrics = new ArrayList<>(targets.size());
        try {
            List<Future<HostMetricSnapshot>> futures = metricsExecutor.invokeAll(
                    tasks, COLLECT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            for (int i = 0; i < futures.size(); i++) {
                Future<HostMetricSnapshot> future = futures.get(i);
                LogTarget target = targets.get(i);
                if (future.isCancelled()) {
                    log.warn("主机指标采集超时: target={}", target.displayName());
                    metrics.add(buildErrorSnapshot(target, "采集超时"));
                    continue;
                }
                try {
                    HostMetricSnapshot snapshot = future.get();
                    metrics.add(snapshot == null ? buildErrorSnapshot(target, "采集结果为空") : snapshot);
                } catch (ExecutionException ex) {
                    Throwable cause = ex.getCause() == null ? ex : ex.getCause();
                    log.warn("主机指标采集任务异常: target={}, error={}", target.displayName(), cause.getMessage());
                    metrics.add(buildErrorSnapshot(target, "采集异常: " + cause.getMessage()));
                }
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("主机指标采集被中断", ex);
        }

        metrics.sort(Comparator
                .comparing((HostMetricSnapshot metric) -> "ok".equals(metric.getStatus()) ? 1 : 0)
                .thenComparing(metric -> nullSafe(metric.getService()))
                .thenComparing(metric -> nullSafe(metric.getHost())));
        return metrics;
    }

    /**
     * 安全采集单个目标的主机指标。
     *
     * @param target 目标节点
     * @return 指标快照
     */
    private HostMetricSnapshot collectMetricSafely(LogTarget target) {
        try {
            return sshLogService.queryHostMetric(target);
        } catch (Exception ex) {
            log.warn("采集主机指标失败: target={}, error={}", target.displayName(), ex.getMessage());
            return buildErrorSnapshot(target, "采集异常: " + ex.getMessage());
        }
    }

    /**
     * 构造错误快照，避免单个节点失败影响整体结果结构。
     *
     * @param target 目标节点
     * @param message 错误消息
     * @return 错误快照
     */
    private HostMetricSnapshot buildErrorSnapshot(LogTarget target, String message) {
        HostMetricSnapshot errSnapshot = new HostMetricSnapshot();
        errSnapshot.setProject(target == null ? "" : target.getProject());
        errSnapshot.setEnv(target == null ? "" : target.getEnv());
        errSnapshot.setService(target == null ? "" : target.getService());
        errSnapshot.setHost(target == null || target.getServerNode() == null
                ? "" : nullSafe(target.getServerNode().getHost()));
        errSnapshot.setStatus("error");
        errSnapshot.setMessage(message == null ? "采集失败" : message);
        errSnapshot.setUpdatedAt(System.currentTimeMillis());
        return errSnapshot;
    }

    /**
     * 规范化缓存键。
     *
     * @param project 项目
     * @param env 环境
     * @param service 服务
     * @return 缓存键对象
     */
    private MetricsCacheKey normalizeCacheKey(String project, String env, String service) {
        String normalizedProject = trimToEmpty(project);
        String normalizedEnv = trimToEmpty(env);
        String normalizedService = trimToEmpty(service);
        if (normalizedEnv.isEmpty()) {
            throw new IllegalArgumentException("环境不能为空");
        }
        if (normalizedService.isEmpty()) {
            throw new IllegalArgumentException("服务不能为空");
        }
        return new MetricsCacheKey(normalizedProject, normalizedEnv, normalizedService);
    }

    /**
     * 规范化来源类型。
     *
     * @param source 原始来源
     * @return 标准来源值
     */
    private String normalizeSource(String source) {
        String normalized = trimToEmpty(source).toLowerCase();
        return MetricsActiveRegisterRequest.SOURCE_JVM.equals(normalized)
                ? MetricsActiveRegisterRequest.SOURCE_JVM
                : MetricsActiveRegisterRequest.SOURCE_PANEL;
    }

    /**
     * 解析当前活跃维度对应的刷新间隔。
     *
     * @param activeEntry 活跃维度
     * @param currentTimeMillis 当前时间戳
     * @return 刷新间隔；无活跃来源返回 -1
     */
    private long resolveRefreshInterval(ActiveMetricEntry activeEntry, long currentTimeMillis) {
        return activeEntry == null ? -1L : activeEntry.resolveRefreshIntervalMs(currentTimeMillis);
    }

    /**
     * 截断错误消息，避免日志与前端状态文案过长。
     *
     * @param message 原始错误
     * @return 截断后的消息
     */
    private String limitMessage(String message) {
        String normalized = trimToEmpty(message);
        if (normalized.length() <= 180) {
            return normalized;
        }
        return normalized.substring(0, 180);
    }

    /**
     * 字符串去空格并兜底空值。
     *
     * @param source 原始值
     * @return 非 null 字符串
     */
    private String trimToEmpty(String source) {
        return source == null ? "" : source.trim();
    }

    /**
     * 字符串空值兜底。
     *
     * @param source 原始值
     * @return 非 null 字符串
     */
    private String nullSafe(String source) {
        return source == null ? "" : source;
    }

    /**
     * 应用关闭前优雅停止线程池。
     */
    @PreDestroy
    public void shutdown() {
        shutdownExecutor(refreshDispatcher);
        shutdownExecutor(metricsExecutor);
    }

    /**
     * 关闭线程池并等待结束。
     *
     * @param executor 线程池
     */
    private void shutdownExecutor(ExecutorService executor) {
        if (executor == null) {
            return;
        }
        executor.shutdown();
        try {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException ignored) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 指标缓存键。
     */
    private static class MetricsCacheKey {

        /**
         * 项目名称。
         */
        private final String project;

        /**
         * 环境名称。
         */
        private final String env;

        /**
         * 服务名称。
         */
        private final String service;

        /**
         * 构造方法。
         *
         * @param project 项目
         * @param env 环境
         * @param service 服务
         */
        private MetricsCacheKey(String project, String env, String service) {
            this.project = project;
            this.env = env;
            this.service = service;
        }

        /**
         * 获取项目名称。
         *
         * @return 项目名称
         */
        private String getProject() {
            return project;
        }

        /**
         * 获取环境名称。
         *
         * @return 环境名称
         */
        private String getEnv() {
            return env;
        }

        /**
         * 获取服务名称。
         *
         * @return 服务名称
         */
        private String getService() {
            return service;
        }

        /**
         * 转换为缓存键字符串。
         *
         * @return 缓存键
         */
        private String toCacheKey() {
            return project + "|" + env + "|" + service;
        }
    }

    /**
     * 活跃维度登记项。
     */
    private static class ActiveMetricEntry {

        /**
         * 项目名称。
         */
        private final String project;

        /**
         * 环境名称。
         */
        private final String env;

        /**
         * 服务名称。
         */
        private final String service;

        /**
         * 指标面板最近心跳时间。
         */
        private volatile long panelLastSeenAt;

        /**
         * JVM 视图最近心跳时间。
         */
        private volatile long jvmLastSeenAt;

        /**
         * 构造方法。
         *
         * @param project 项目
         * @param env 环境
         * @param service 服务
         */
        private ActiveMetricEntry(String project, String env, String service) {
            this.project = project;
            this.env = env;
            this.service = service;
        }

        /**
         * 获取项目名称。
         *
         * @return 项目名称
         */
        private String getProject() {
            return project;
        }

        /**
         * 获取环境名称。
         *
         * @return 环境名称
         */
        private String getEnv() {
            return env;
        }

        /**
         * 获取服务名称。
         *
         * @return 服务名称
         */
        private String getService() {
            return service;
        }

        /**
         * 续期某个来源的最近访问时间。
         *
         * @param source 来源类型
         * @param currentTimeMillis 当前时间戳
         * @return true 表示该来源由失活恢复为活跃
         */
        private boolean touchSource(String source, long currentTimeMillis) {
            if (MetricsActiveRegisterRequest.SOURCE_JVM.equals(source)) {
                boolean wasActive = isJvmActive(currentTimeMillis);
                this.jvmLastSeenAt = currentTimeMillis;
                return !wasActive;
            }
            boolean wasActive = isPanelActive(currentTimeMillis);
            this.panelLastSeenAt = currentTimeMillis;
            return !wasActive;
        }

        /**
         * 判断指标面板来源是否仍活跃。
         *
         * @param currentTimeMillis 当前时间戳
         * @return true 表示活跃
         */
        private boolean isPanelActive(long currentTimeMillis) {
            return currentTimeMillis - panelLastSeenAt < ACTIVE_KEY_TTL_MS;
        }

        /**
         * 判断 JVM 来源是否仍活跃。
         *
         * @param currentTimeMillis 当前时间戳
         * @return true 表示活跃
         */
        private boolean isJvmActive(long currentTimeMillis) {
            return currentTimeMillis - jvmLastSeenAt < ACTIVE_KEY_TTL_MS;
        }

        /**
         * 判断当前维度是否已整体失活。
         *
         * @param currentTimeMillis 当前时间戳
         * @return true 表示失活
         */
        private boolean isExpired(long currentTimeMillis) {
            return !isPanelActive(currentTimeMillis) && !isJvmActive(currentTimeMillis);
        }

        /**
         * 解析当前应使用的刷新间隔。
         * <p>
         * 只要 JVM 视图仍活跃，就按 JVM 粒度刷新；
         * 否则回退到左侧指标面板粒度。
         * </p>
         *
         * @param currentTimeMillis 当前时间戳
         * @return 刷新间隔；无活跃来源返回 -1
         */
        private long resolveRefreshIntervalMs(long currentTimeMillis) {
            if (isJvmActive(currentTimeMillis)) {
                return JVM_REFRESH_INTERVAL_MS;
            }
            if (isPanelActive(currentTimeMillis)) {
                return PANEL_REFRESH_INTERVAL_MS;
            }
            return -1L;
        }

        /**
         * 转换为缓存键字符串。
         *
         * @return 缓存键
         */
        private String toCacheKey() {
            return project + "|" + env + "|" + service;
        }
    }

    /**
     * 快照缓存条目。
     */
    private static class SnapshotCacheEntry {

        /**
         * 是否已有成功快照。
         */
        private final boolean ready;

        /**
         * 最近一次成功更新时间。
         */
        private final long updatedAt;

        /**
         * 最近一次尝试时间。
         */
        private final long lastAttemptAt;

        /**
         * 最近一次失败信息。
         */
        private final String lastError;

        /**
         * 指标列表。
         */
        private final List<HostMetricSnapshot> metrics;

        /**
         * 构造方法。
         *
         * @param ready 是否已有成功快照
         * @param updatedAt 最近一次成功更新时间
         * @param lastAttemptAt 最近一次尝试时间
         * @param lastError 最近一次失败信息
         * @param metrics 指标列表
         */
        private SnapshotCacheEntry(boolean ready,
                                   long updatedAt,
                                   long lastAttemptAt,
                                   String lastError,
                                   List<HostMetricSnapshot> metrics) {
            this.ready = ready;
            this.updatedAt = updatedAt;
            this.lastAttemptAt = lastAttemptAt;
            this.lastError = lastError == null ? "" : lastError;
            this.metrics = metrics == null ? new ArrayList<>() : new ArrayList<>(metrics);
        }

        /**
         * 构造成功快照。
         *
         * @param now 当前时间
         * @param metrics 指标列表
         * @return 缓存条目
         */
        private static SnapshotCacheEntry success(long now, List<HostMetricSnapshot> metrics) {
            return new SnapshotCacheEntry(true, now, now, "", metrics);
        }

        /**
         * 构造失败快照。
         * <p>
         * 若之前已有成功快照，则保留旧数据并标记 stale；
         * 否则仅回填失败信息。
         * </p>
         *
         * @param now 当前时间
         * @param errorMessage 错误信息
         * @param previous 旧缓存
         * @return 缓存条目
         */
        private static SnapshotCacheEntry failed(long now, String errorMessage, SnapshotCacheEntry previous) {
            if (previous != null && previous.ready) {
                return new SnapshotCacheEntry(
                        true, previous.updatedAt, now, errorMessage, previous.metrics);
            }
            return new SnapshotCacheEntry(false, 0L, now, errorMessage, new ArrayList<>());
        }

        /**
         * 是否已有成功快照。
         *
         * @return true 表示可直接展示
         */
        private boolean isReady() {
            return ready;
        }

        /**
         * 是否至少尝试过一次采集。
         *
         * @return true 表示已尝试过
         */
        private boolean hasAttempted() {
            return lastAttemptAt > 0L;
        }

        /**
         * 是否包含最近一次失败信息。
         *
         * @return true 表示最近采集失败
         */
        private boolean hasLastError() {
            return !lastError.isEmpty();
        }

        /**
         * 复制指标列表。
         *
         * @return 指标列表副本
         */
        private List<HostMetricSnapshot> copyMetrics() {
            return new ArrayList<>(metrics);
        }

        /**
         * 判断是否需要再次刷新。
         *
         * @param currentTimeMillis 当前时间戳
         * @param refreshIntervalMs 刷新间隔
         * @return true 表示应刷新
         */
        private boolean shouldRefresh(long currentTimeMillis, long refreshIntervalMs) {
            if (refreshIntervalMs <= 0L) {
                return false;
            }
            if (lastAttemptAt <= 0L) {
                return true;
            }
            return currentTimeMillis - lastAttemptAt >= refreshIntervalMs;
        }

        /**
         * 判断当前展示的数据是否已过期。
         *
         * @param refreshIntervalMs 当前维度的刷新间隔
         * @param currentTimeMillis 当前时间戳
         * @return true 表示 stale
         */
        private boolean isStale(long refreshIntervalMs, long currentTimeMillis) {
            if (hasLastError() && ready) {
                return true;
            }
            if (refreshIntervalMs <= 0L || lastAttemptAt <= 0L) {
                return false;
            }
            return currentTimeMillis - lastAttemptAt >= refreshIntervalMs * 2;
        }

        /**
         * 获取成功更新时间。
         *
         * @return 成功更新时间；没有成功快照时返回 null
         */
        private Long getUpdatedAtValue() {
            return updatedAt > 0L ? updatedAt : null;
        }

        /**
         * 获取最后一次尝试时间。
         *
         * @return 尝试时间；未尝试时返回 null
         */
        private Long getLastAttemptAtValue() {
            return lastAttemptAt > 0L ? lastAttemptAt : null;
        }

        /**
         * 获取最近一次错误信息。
         *
         * @return 错误信息；为空时返回 null
         */
        private String getLastErrorValue() {
            return lastError.isEmpty() ? null : lastError;
        }

        /**
         * 判断缓存条目是否已过期。
         *
         * @param currentTimeMillis 当前时间戳
         * @return true 表示过期
         */
        private boolean isExpired(long currentTimeMillis) {
            long baseTime = lastAttemptAt > 0L ? lastAttemptAt : updatedAt;
            if (baseTime <= 0L) {
                return true;
            }
            return currentTimeMillis - baseTime >= SNAPSHOT_RETENTION_MS;
        }
    }
}
