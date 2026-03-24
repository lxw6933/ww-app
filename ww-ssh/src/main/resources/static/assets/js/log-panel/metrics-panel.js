import {el} from './dom.js';

/**
 * 高风险动作二次确认有效期（毫秒）。
 */
const OP_CONFIRM_EXPIRE_MS = 4200;

const INSTANCE_STATUS_META = Object.freeze({
    running: Object.freeze({text: '运行中', level: 'running'}),
    stopped: Object.freeze({text: '已停止', level: 'stopped'}),
    unconfigured: Object.freeze({text: '未配置', level: 'unconfigured'}),
    unknown: Object.freeze({text: '未知', level: 'unknown'})
});

const OPERATION_BUTTON_META = Object.freeze({
    monitor: Object.freeze({
        label: 'JVM',
        title: '打开 JVM 监控',
        style: 'secondary metric-op-btn',
        icon: 'icon-monitor'
    }),
    start: Object.freeze({
        action: 'start',
        label: '启动',
        title: '启动实例',
        style: 'start',
        icon: 'icon-play'
    }),
    restart: Object.freeze({
        action: 'restart',
        label: '重启',
        title: '重启实例',
        style: 'restart',
        icon: 'icon-restart'
    }),
    stop: Object.freeze({
        action: 'stop',
        label: '停止',
        title: '停止实例',
        style: 'stop',
        icon: 'icon-stop'
    }),
    pending: Object.freeze({
        label: '执行中',
        icon: 'icon-loading'
    })
});

const MANAGED_INSTANCE_ACTIONS = Object.freeze([
    OPERATION_BUTTON_META.start,
    OPERATION_BUTTON_META.restart,
    OPERATION_BUTTON_META.stop
]);

const ACTION_TEXT_MAP = Object.freeze({
    start: '启动',
    restart: '重启',
    stop: '停止'
});

const OPERATION_DISABLE_REASON_MAP = Object.freeze({
    running: Object.freeze({
        start: '当前实例已运行'
    }),
    stopped: Object.freeze({
        stop: '当前实例已停止'
    }),
    unconfigured: Object.freeze({
        default: '未配置运维脚本'
    }),
    default: Object.freeze({
        default: '当前状态不支持该动作'
    })
});

/**
 * 服务器指标面板控制器。
 * <p>
 * 负责指标请求、轮询刷新与可视化渲染，避免主流程文件过重。
 * </p>
 */
export class MetricsPanelController {

    /**
     * 构造方法。
     *
     * @param {Object} options 配置项
     * @param {Function} options.getProject 获取当前项目
     * @param {Function} options.getEnv 获取当前环境
     * @param {Function} options.getService 获取当前服务
     */
    constructor(options) {
        this.getProject = typeof options.getProject === 'function' ? options.getProject : (() => '');
        this.getEnv = typeof options.getEnv === 'function' ? options.getEnv : (() => '');
        this.getService = typeof options.getService === 'function' ? options.getService : (() => '');
        this.operateInstance = typeof options.operateInstance === 'function' ? options.operateInstance : null;
        this.openJvmMonitor = typeof options.openJvmMonitor === 'function' ? options.openJvmMonitor : null;
        this.refreshMs = 15000;
        this.registerIntervalMs = 30000;
        this.bootstrapRetryMs = 1200;
        this.timer = null;
        this.bootstrapRetryTimer = null;
        this.token = 0;
        this.lastMetrics = [];
        this.operationStates = new Map();
        this.operationTips = new Map();
        this.operationConfirmStates = new Map();
        this.middlewareCache = new Map();
        this.openMiddlewarePanelKey = '';
        this.renderedMiddlewareContextKey = '';
        this.loading = false;
        this.activeRequestController = null;
        this.activeQueryKey = '';
        this.requestInFlight = false;
        this.pollingEnabled = true;
        this.registeredQueryKey = '';
        this.lastRegisterAt = 0;
        this.visibilityEventsBound = false;
        this.middlewareEventsBound = false;
    }

    /**
     * 初始化指标面板。
     */
    init() {
        this.renderEmpty('请选择项目、环境与服务后查看服务器指标');
        this.bindVisibilityEvents();
        this.bindMiddlewarePanelEvents();
        this.startPolling();
    }

    /**
     * 设置轮询启用状态。
     * <p>
     * JVM 中央视图会复用同一指标接口做高频采样；
     * 此时暂停左侧面板的后台轮询，避免同页重复压测后端 SSH 采集。
     * </p>
     *
     * @param {boolean} enabled 是否启用轮询
     * @param {boolean} refreshNow 恢复后是否立即刷新一次
     */
    setPollingEnabled(enabled, refreshNow) {
        const nextEnabled = !!enabled;
        if (this.pollingEnabled === nextEnabled) {
            if (nextEnabled && refreshNow) {
                this.refresh(true);
            }
            return;
        }
        this.pollingEnabled = nextEnabled;
        if (nextEnabled) {
            this.startPolling();
            if (refreshNow) {
                this.refresh(true);
            }
            return;
        }
        this.stopPolling();
    }

    /**
     * 启动轮询任务。
     */
    startPolling() {
        if (!this.pollingEnabled || this.isPageHidden()) {
            return;
        }
        this.stopPolling();
        this.timer = window.setInterval(() => this.refresh(false), this.refreshMs);
    }

    /**
     * 停止轮询任务。
     */
    stopPolling() {
        if (this.timer) {
            window.clearInterval(this.timer);
            this.timer = null;
        }
        this.clearBootstrapRetry();
        this.cancelActiveRequest();
        this.setLoading(false);
    }

    /**
     * 刷新指标数据。
     *
     * @param {boolean} manual 是否手动刷新
     */
    refresh(manual) {
        if ((!this.pollingEnabled && !manual) || this.isPageHidden()) {
            return;
        }
        const project = this.getProject();
        const env = this.getEnv();
        const service = this.getService();
        if (!project || !env || !service) {
            this.resetRegistrationState();
            this.clearBootstrapRetry();
            this.cancelActiveRequest();
            this.renderEmpty('请选择项目、环境与服务后查看服务器指标');
            return;
        }
        const queryKey = `${String(project)}#${String(env)}#${String(service)}`;
        if (!manual && this.requestInFlight && this.activeQueryKey === queryKey) {
            return;
        }
        this.cancelActiveRequest();
        this.activeQueryKey = queryKey;
        this.renderLoadingState(manual);

        const currentToken = ++this.token;
        const hasAbortController = typeof window !== 'undefined' && typeof window.AbortController === 'function';
        const controller = hasAbortController ? new window.AbortController() : null;
        this.activeRequestController = controller;
        this.requestInFlight = true;
        const options = controller ? {signal: controller.signal} : undefined;

        this.ensureActiveRegistration(project, env, service, options)
            .then(() => this.fetchMetrics(project, env, service, options))
            .then(payload => {
                if (currentToken !== this.token) {
                    return;
                }
                const snapshotPayload = this.normalizeSnapshotPayload(payload);
                if (!snapshotPayload.ready) {
                    if (snapshotPayload.lastError) {
                        this.renderError(`指标采集失败：${snapshotPayload.lastError}`);
                    } else {
                        this.renderEmpty('指标正在准备中，请稍后自动刷新');
                    }
                    this.scheduleBootstrapRetry(queryKey);
                    this.setLoading(false);
                    return;
                }
                this.clearBootstrapRetry();
                this.renderMetrics(snapshotPayload.metrics, snapshotPayload);
                this.setLoading(false);
            })
            .catch(error => {
                if (currentToken !== this.token) {
                    return;
                }
                if (error && error.name === 'AbortError') {
                    return;
                }
                this.clearBootstrapRetry();
                this.renderError(`指标加载失败：${error.message || '网络异常'}`);
                this.setLoading(false);
            })
            .finally(() => {
                if (currentToken !== this.token) {
                    return;
                }
                this.activeRequestController = null;
                this.requestInFlight = false;
            });
    }

    /**
     * 确保当前维度已完成活跃心跳登记。
     *
     * @param {string} project 项目
     * @param {string} env 环境
     * @param {string} service 服务
     * @param {Object} options fetch 配置
     * @returns {Promise<void>} 注册结果
     */
    ensureActiveRegistration(project, env, service, options) {
        const queryKey = `${String(project)}#${String(env)}#${String(service)}`;
        const now = Date.now();
        if (this.registeredQueryKey === queryKey && now - this.lastRegisterAt < this.registerIntervalMs) {
            return Promise.resolve();
        }
        return this.registerActiveQuery(project, env, service, options).then(() => {
            this.registeredQueryKey = queryKey;
            this.lastRegisterAt = Date.now();
        });
    }

    /**
     * 注册当前正在浏览的指标维度。
     *
     * @param {string} project 项目
     * @param {string} env 环境
     * @param {string} service 服务
     * @param {Object} options fetch 配置
     * @returns {Promise<void>} 注册结果
     */
    registerActiveQuery(project, env, service, options) {
        return fetch('/api/metrics/active/register', Object.assign({
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                project: project,
                env: env,
                service: service,
                source: 'panel'
            })
        }, options)).then(response => {
            if (!response.ok) {
                throw new Error(`状态码 ${response.status}`);
            }
        });
    }

    /**
     * 读取主机指标快照。
     *
     * @param {string} project 项目
     * @param {string} env 环境
     * @param {string} service 服务
     * @param {Object} options fetch 配置
     * @returns {Promise<Object>} 指标响应
     */
    fetchMetrics(project, env, service, options) {
        return fetch(`/api/metrics/hosts?project=${encodeURIComponent(project)}&env=${encodeURIComponent(env)}&service=${encodeURIComponent(service)}`, options)
            .then(response => {
                if (!response.ok) {
                    throw new Error(`状态码 ${response.status}`);
                }
                return response.json();
            });
    }

    /**
     * 规范化后端快照响应。
     *
     * @param {Object} payload 原始响应
     * @returns {{ready:boolean,stale:boolean,updatedAt:number|null,lastAttemptAt:number|null,lastError:string,metrics:Array<Object>}} 规范化结果
     */
    normalizeSnapshotPayload(payload) {
        const source = payload && typeof payload === 'object' ? payload : {};
        return {
            ready: !!source.ready,
            stale: !!source.stale,
            updatedAt: Number.isFinite(Number(source.updatedAt)) ? Number(source.updatedAt) : null,
            lastAttemptAt: Number.isFinite(Number(source.lastAttemptAt)) ? Number(source.lastAttemptAt) : null,
            lastError: source.lastError ? String(source.lastError) : '',
            metrics: Array.isArray(source.metrics) ? source.metrics : []
        };
    }

    /**
     * 重置当前维度的注册状态。
     */
    resetRegistrationState() {
        this.registeredQueryKey = '';
        this.lastRegisterAt = 0;
    }

    /**
     * 为首屏“准备中”状态安排一次短间隔补拉。
     *
     * @param {string} queryKey 当前查询键
     */
    scheduleBootstrapRetry(queryKey) {
        if (!queryKey || this.isPageHidden() || !this.pollingEnabled) {
            return;
        }
        if (this.bootstrapRetryTimer) {
            return;
        }
        this.bootstrapRetryTimer = window.setTimeout(() => {
            this.bootstrapRetryTimer = null;
            if (this.activeQueryKey !== queryKey || this.isPageHidden() || !this.pollingEnabled) {
                return;
            }
            this.refresh(true);
        }, this.bootstrapRetryMs);
    }

    /**
     * 清理首屏补拉定时器。
     */
    clearBootstrapRetry() {
        if (!this.bootstrapRetryTimer) {
            return;
        }
        window.clearTimeout(this.bootstrapRetryTimer);
        this.bootstrapRetryTimer = null;
    }

    /**
     * 绑定页面显隐事件。
     * <p>
     * 页面处于后台标签页时暂停轮询与请求，
     * 恢复可见后再立即补一次刷新。
     * </p>
     */
    bindVisibilityEvents() {
        if (this.visibilityEventsBound || typeof document === 'undefined') {
            return;
        }
        this.visibilityEventsBound = true;
        document.addEventListener('visibilitychange', () => {
            if (this.isPageHidden()) {
                this.stopPolling();
                return;
            }
            if (!this.pollingEnabled) {
                return;
            }
            this.startPolling();
            this.refresh(true);
        });
    }

    /**
     * 绑定中间件悬浮面板相关的全局事件。
     * <p>
     * 1. 点击空白区域时关闭已手动展开的面板；<br>
     * 2. 按下 Escape 时统一收起，避免小屏或键盘操作下残留浮层。<br>
     * </p>
     */
    bindMiddlewarePanelEvents() {
        if (this.middlewareEventsBound || typeof document === 'undefined') {
            return;
        }
        this.middlewareEventsBound = true;
        document.addEventListener('click', event => {
            if (event && event.target && event.target.closest && event.target.closest('.metric-middleware')) {
                return;
            }
            this.closeOpenMiddlewarePopovers(null);
        });
        document.addEventListener('keydown', event => {
            if (event && event.key === 'Escape') {
                this.closeOpenMiddlewarePopovers(null);
            }
        });
    }

    /**
     * 判断页面当前是否处于后台不可见状态。
     *
     * @returns {boolean} true 表示不可见
     */
    isPageHidden() {
        return typeof document !== 'undefined' && !!document.hidden;
    }

    /**
     * 关闭当前页面中已手动展开的中间件面板。
     *
     * @param {HTMLElement|null} exceptWrapper 需要保留展开状态的浮层容器
     */
    closeOpenMiddlewarePopovers(exceptWrapper) {
        if (typeof document === 'undefined') {
            return;
        }
        document.querySelectorAll('.metric-middleware.is-open').forEach(wrapper => {
            if (exceptWrapper && wrapper === exceptWrapper) {
                return;
            }
            this.closeMiddlewarePopover(wrapper);
        });
    }

    /**
     * 关闭单个中间件浮层，并同步收起按钮展开态。
     *
     * @param {HTMLElement|null} wrapper 浮层容器
     */
    closeMiddlewarePopover(wrapper) {
        if (!wrapper) {
            return;
        }
        wrapper.classList.remove('is-open');
        const triggerEl = wrapper.querySelector('.metric-middleware-trigger');
        if (triggerEl) {
            triggerEl.setAttribute('aria-expanded', 'false');
        }
        if (wrapper.dataset && wrapper.dataset.middlewareKey === this.openMiddlewarePanelKey) {
            this.openMiddlewarePanelKey = '';
        }
    }

    /**
     * 打开单个中间件浮层，并按当前环境记录展开状态。
     *
     * @param {HTMLElement|null} wrapper 浮层容器
     */
    openMiddlewarePopover(wrapper) {
        if (!wrapper) {
            return;
        }
        this.closeOpenMiddlewarePopovers(wrapper);
        wrapper.classList.add('is-open');
        const triggerEl = wrapper.querySelector('.metric-middleware-trigger');
        if (triggerEl) {
            triggerEl.setAttribute('aria-expanded', 'true');
        }
        this.openMiddlewarePanelKey = wrapper.dataset && wrapper.dataset.middlewareKey
            ? String(wrapper.dataset.middlewareKey)
            : '';
    }

    /**
     * 判断当前环境的中间件浮层是否需要在重渲染后恢复展开。
     *
     * @param {string} service 实例服务名
     * @returns {boolean} true 表示需要恢复展开
     */
    shouldRestoreMiddlewarePopover(service) {
        const targetKey = this.buildMiddlewareKey(service);
        return !!targetKey && targetKey === this.openMiddlewarePanelKey;
    }

    /**
     * 渲染空态。
     *
     * @param {string} text 文案
     */
    renderEmpty(text) {
        this.setLoading(false);
        this.lastMetrics = [];
        this.renderGlobalMiddlewareWidget(null);
        this.renderSummary({
            statusText: '待选择',
            online: null,
            total: null,
            errorCount: null,
            avgCpu: null,
            avgMem: null,
            timeText: '--'
        });
        const listEl = el('metricsList');
        listEl.innerHTML = '';
        const emptyEl = document.createElement('div');
        emptyEl.className = 'metrics-empty';
        emptyEl.textContent = text;
        listEl.appendChild(emptyEl);
    }

    /**
     * 渲染错误态。
     *
     * @param {string} text 错误信息
     */
    renderError(text) {
        this.setLoading(false);
        this.lastMetrics = [];
        this.renderGlobalMiddlewareWidget(null);
        this.renderSummary({
            statusText: '获取失败',
            online: null,
            total: null,
            errorCount: null,
            avgCpu: null,
            avgMem: null,
            timeText: formatTime(Date.now())
        });
        const listEl = el('metricsList');
        listEl.innerHTML = '';
        const errorEl = document.createElement('div');
        errorEl.className = 'metrics-empty';
        errorEl.textContent = text;
        listEl.appendChild(errorEl);
    }

    /**
     * 渲染指标列表。
     *
     * @param {Array<Object>} list 指标集合
     */
    renderMetrics(list, snapshotPayload) {
        this.lastMetrics = Array.isArray(list) ? list.slice() : [];
        const listEl = el('metricsList');
        listEl.innerHTML = '';
        if (!list || list.length === 0) {
            this.renderEmpty('未查询到实例指标，请检查服务配置');
            return;
        }

        this.renderGlobalMiddlewareWidget(this.resolveGlobalMiddlewareTarget(list));
        const hostGroups = this.groupByHost(list);
        const total = list.length;
        const totalServers = hostGroups.length;
        const okServerMetrics = hostGroups
            .map(group => group && group.serverMetric ? group.serverMetric : null)
            .filter(item => item && item.status === 'ok');
        const errorCount = total - list.filter(item => item && item.status === 'ok').length;
        const healthyServers = hostGroups.filter(group => group && group.serverMetric && group.serverMetric.status === 'ok').length;
        const unhealthyServers = Math.max(totalServers - healthyServers, 0);
        const avgCpu = average(okServerMetrics.map(item => item.cpuUsagePercent));
        const avgMem = average(okServerMetrics.map(item => item.memoryUsagePercent));
        const avgDisk = average(okServerMetrics.map(item => item.diskUsagePercent));

        this.renderSummary({
            statusText: snapshotPayload && snapshotPayload.stale ? '缓存数据' : '采集正常',
            healthyServers: healthyServers,
            totalServers: totalServers,
            unhealthyServers: unhealthyServers,
            avgCpu: avgCpu,
            avgMem: avgMem,
            avgDisk: avgDisk,
            timeText: formatTime(snapshotPayload && snapshotPayload.updatedAt ? snapshotPayload.updatedAt : Date.now())
        });

        hostGroups.forEach(group => {
            listEl.appendChild(this.createHostGroup(group));
        });
    }

    /**
     * 渲染加载中状态。
     * <p>
     * 1. 有历史数据时对原内容做“半透明过渡”；<br>
     * 2. 无历史数据时展示骨架屏，避免切换环境/服务时出现闪烁空白。
     * </p>
     *
     * @param {boolean} manual 是否手动刷新
     */
    renderLoadingState(manual) {
        const listEl = el('metricsList');
        const hasCards = !!(listEl && listEl.querySelector('.metric-host-group'));
        const currentMiddlewareContextKey = this.buildMiddlewareKey('');
        if (this.renderedMiddlewareContextKey && this.renderedMiddlewareContextKey !== currentMiddlewareContextKey) {
            this.renderGlobalMiddlewareWidget(null);
        }
        if (!hasCards) {
            this.renderGlobalMiddlewareWidget(null);
        }
        if (hasCards && !manual) {
            this.setLoading(false);
            return;
        }
        const shouldDimPanel = !!manual || !hasCards;
        this.setLoading(shouldDimPanel);
        this.renderSummary({
            statusText: manual ? '刷新中' : '加载中',
            healthyServers: null,
            totalServers: null,
            unhealthyServers: null,
            avgCpu: null,
            avgMem: null,
            avgDisk: null,
            timeText: formatTime(Date.now())
        });
        if (!listEl) {
            return;
        }
        if (hasCards) {
            return;
        }
        this.renderLoadingSkeleton();
    }

    /**
     * 取消当前正在进行的指标请求。
     * <p>
     * 仅中断网络请求，不主动改写面板内容，
     * 由后续 refresh/render 流程决定最终展示状态。
     * </p>
     */
    cancelActiveRequest() {
        if (!this.activeRequestController) {
            this.requestInFlight = false;
            this.activeQueryKey = '';
            return;
        }
        this.activeRequestController.abort();
        this.activeRequestController = null;
        this.requestInFlight = false;
        this.activeQueryKey = '';
    }

    /**
     * 渲染指标骨架屏。
     */
    renderLoadingSkeleton() {
        const listEl = el('metricsList');
        if (!listEl) {
            return;
        }
        listEl.innerHTML = '';
        for (let i = 0; i < 2; i++) {
            const skeletonEl = document.createElement('section');
            skeletonEl.className = 'metrics-skeleton-card';
            skeletonEl.appendChild(createSkeletonLine('w-40'));
            skeletonEl.appendChild(createSkeletonLine('w-90'));
            skeletonEl.appendChild(createSkeletonLine('w-85'));
            skeletonEl.appendChild(createSkeletonLine('w-65'));
            listEl.appendChild(skeletonEl);
        }
    }

    /**
     * 设置面板加载样式。
     *
     * @param {boolean} loading 是否加载中
     */
    setLoading(loading) {
        this.loading = !!loading;
        const summaryEl = el('metricsSummary');
        if (summaryEl) {
            summaryEl.classList.toggle('is-loading', this.loading);
        }
        const listEl = el('metricsList');
        if (listEl) {
            listEl.classList.toggle('is-loading', this.loading);
        }
    }

    /**
     * 按主机地址分组指标。
     *
     * @param {Array<Object>} list 指标集合
     * @returns {Array<{host:string,items:Array<Object>,runningCount:number,errorCount:number,lastUpdated:number,serverMetric:Object}>} 分组结果
     */
    groupByHost(list) {
        const hostMap = new Map();
        (list || []).forEach(item => {
            const normalizedHost = this.normalizeHostForGroup(item && item.host);
            if (!hostMap.has(normalizedHost.key)) {
                hostMap.set(normalizedHost.key, {
                    host: normalizedHost.display,
                    items: []
                });
            }
            hostMap.get(normalizedHost.key).items.push(item || {});
        });
        const groups = [];
        hostMap.forEach(groupValue => {
            const host = groupValue && groupValue.host ? groupValue.host : '未知主机';
            const items = groupValue && Array.isArray(groupValue.items) ? groupValue.items : [];
            const sortedItems = items.slice().sort((left, right) => {
                const leftError = left && left.status === 'ok' ? 1 : 0;
                const rightError = right && right.status === 'ok' ? 1 : 0;
                if (leftError !== rightError) {
                    return leftError - rightError;
                }
                return String(left && left.service ? left.service : '').localeCompare(String(right && right.service ? right.service : ''));
            });
            groups.push({
                host: host,
                items: sortedItems,
                runningCount: sortedItems.filter(item => resolveInstanceStatus(item && item.instanceStatus).level === 'running').length,
                errorCount: sortedItems.filter(item => item && item.status !== 'ok').length,
                lastUpdated: sortedItems.reduce((max, item) => Math.max(max, Number(item && item.updatedAt ? item.updatedAt : 0)), 0),
                serverMetric: this.pickServerMetric(sortedItems)
            });
        });
        groups.sort((left, right) => {
            if (left.errorCount !== right.errorCount) {
                return right.errorCount - left.errorCount;
            }
            return left.host.localeCompare(right.host);
        });
        return groups;
    }

    /**
     * 规范化主机名，避免同机因配置差异（大小写、默认端口）被拆成多组。
     *
     * @param {string} rawHost 原始主机文本
     * @returns {{key:string,display:string}} 分组键与展示名
     */
    normalizeHostForGroup(rawHost) {
        const source = rawHost === null || rawHost === undefined ? '' : String(rawHost).trim();
        if (!source) {
            return {
                key: 'unknown-host',
                display: '未知主机'
            };
        }

        let normalized = source;
        const bracketCloseIndex = normalized.indexOf(']');
        if (normalized.startsWith('[') && bracketCloseIndex > 0) {
            const ipPart = normalized.substring(1, bracketCloseIndex).trim();
            const suffix = normalized.substring(bracketCloseIndex + 1).trim();
            if (ipPart && (!suffix || /^:\d+$/.test(suffix))) {
                normalized = ipPart;
            }
        } else {
            const firstColonIndex = normalized.indexOf(':');
            const lastColonIndex = normalized.lastIndexOf(':');
            if (firstColonIndex > 0 && firstColonIndex === lastColonIndex) {
                const possiblePort = normalized.substring(lastColonIndex + 1).trim();
                if (/^\d+$/.test(possiblePort)) {
                    normalized = normalized.substring(0, lastColonIndex).trim();
                }
            }
        }

        if (!normalized) {
            normalized = source;
        }
        return {
            key: normalized.toLowerCase(),
            display: normalized
        };
    }

    /**
     * 选择主机级指标快照。
     * <p>
     * 同一台主机下多个实例的机器指标理论上应一致，这里优先取采集成功的实例快照。
     * </p>
     *
     * @param {Array<Object>} items 同主机实例快照
     * @returns {Object} 主机指标快照
     */
    pickServerMetric(items) {
        const list = Array.isArray(items) ? items : [];
        if (list.length === 0) {
            return {};
        }
        for (let i = 0; i < list.length; i++) {
            if (list[i] && list[i].status === 'ok') {
                return list[i];
            }
        }
        return list[0];
    }

    /**
     * 渲染主机分组卡片。
     *
     * @param {{host:string,items:Array<Object>,runningCount:number,errorCount:number,lastUpdated:number,serverMetric:Object}} group 主机分组
     * @returns {HTMLDivElement} 主机分组节点
     */
    createHostGroup(group) {
        const groupEl = document.createElement('section');
        groupEl.className = 'metric-host-group';

        const headEl = document.createElement('div');
        headEl.className = 'metric-host-head';

        const titleEl = document.createElement('h3');
        titleEl.className = 'metric-host-title';
        titleEl.textContent = group.host || '未知主机';
        headEl.appendChild(titleEl);

        const badgeWrapEl = document.createElement('div');
        badgeWrapEl.className = 'metric-host-badges';
        badgeWrapEl.appendChild(createHostBadge('实例', `${group.items.length}`, '', 'instances'));
        badgeWrapEl.appendChild(createHostBadge('运行', `${group.runningCount}`, '', 'running'));
        badgeWrapEl.appendChild(createHostBadge('异常', `${group.errorCount}`, group.errorCount > 0 ? 'error' : '', 'error'));
        const updateText = group.lastUpdated > 0 ? formatTime(group.lastUpdated) : '--';
        badgeWrapEl.appendChild(createHostBadge('更新', updateText, '', 'updated'));
        headEl.appendChild(badgeWrapEl);

        groupEl.appendChild(headEl);
        groupEl.appendChild(this.createServerCard(group && group.serverMetric ? group.serverMetric : {}, group.host));

        const listEl = document.createElement('div');
        listEl.className = 'metric-host-services';
        group.items.forEach(item => {
            listEl.appendChild(this.createServiceCard(item, group.host, true));
        });
        groupEl.appendChild(listEl);
        return groupEl;
    }

    /**
     * 渲染主机级指标卡片（同一台主机仅展示一次）。
     *
     * @param {Object} item 指标项
     * @param {string} host 主机名称
     * @returns {HTMLDivElement} 主机卡片
     */
    createServerCard(item, host) {
        const cardEl = document.createElement('div');
        const ok = item && item.status === 'ok';
        cardEl.className = ok ? 'metric-card metric-server-card' : 'metric-card metric-server-card error';

        const headEl = document.createElement('div');
        headEl.className = 'metric-head';

        const titleEl = document.createElement('p');
        titleEl.className = 'metric-title';
        titleEl.title = `服务器指标 @ ${host || '未知主机'}`;
        titleEl.textContent = '服务器指标';

        const tagEl = document.createElement('span');
        tagEl.className = ok ? 'metric-tag' : 'metric-tag error';
        tagEl.textContent = ok ? '采集正常' : '采集异常';

        headEl.appendChild(titleEl);
        headEl.appendChild(tagEl);
        cardEl.appendChild(headEl);

        const kpiGridEl = document.createElement('div');
        kpiGridEl.className = 'metric-kpi-grid';
        kpiGridEl.appendChild(createUsageVisual(
            'CPU',
            item && item.cpuUsagePercent,
            formatPercent(item && item.cpuUsagePercent)
        ));
        kpiGridEl.appendChild(createUsageVisual(
            '内存',
            item && item.memoryUsagePercent,
            formatPercent(item && item.memoryUsagePercent),
            formatMemoryRange(item)
        ));
        kpiGridEl.appendChild(createUsageVisual(
            '交换',
            item && item.swapUsagePercent,
            formatPercent(item && item.swapUsagePercent),
            formatSwapRange(item)
        ));
        kpiGridEl.appendChild(createUsageVisual(
            '磁盘',
            item && item.diskUsagePercent,
            formatPercent(item && item.diskUsagePercent),
            formatDiskRange(item)
        ));
        cardEl.appendChild(kpiGridEl);
        cardEl.appendChild(createLoadBlock(item));

        if (!ok) {
            const messageEl = document.createElement('p');
            messageEl.className = 'metric-message';
            messageEl.textContent = item && item.message ? item.message : '采集失败';
            cardEl.appendChild(messageEl);
        }
        return cardEl;
    }

    /**
     * 渲染服务指标卡片。
     *
     * @param {Object} item 指标项
     * @param {string} host 主机名称
     * @param {boolean} instanceOnly 是否仅展示实例信息（不重复渲染主机指标）
     * @returns {HTMLDivElement} 服务卡片
     */
    createServiceCard(item, host, instanceOnly) {
        const cardEl = document.createElement('div');
        const ok = item && item.status === 'ok';
        const compact = !!instanceOnly;
        cardEl.className = ok
            ? `metric-card metric-instance-card${compact ? ' compact' : ''}`
            : `metric-card metric-instance-card${compact ? ' compact' : ''} error`;

        const headEl = document.createElement('div');
        headEl.className = 'metric-head';

        const titleEl = document.createElement('p');
        titleEl.className = 'metric-title';
        const serviceText = item && item.service ? item.service : '未知服务';
        titleEl.title = `${serviceText} @ ${host || '未知主机'}`;
        titleEl.textContent = serviceText;

        const tagEl = document.createElement('span');
        tagEl.className = ok ? 'metric-tag' : 'metric-tag error';
        tagEl.textContent = ok ? '采集正常' : '采集异常';

        headEl.appendChild(titleEl);
        headEl.appendChild(tagEl);
        cardEl.appendChild(headEl);

        if (!compact) {
            const kpiGridEl = document.createElement('div');
            kpiGridEl.className = 'metric-kpi-grid';
            kpiGridEl.appendChild(createUsageVisual(
                'CPU',
                item && item.cpuUsagePercent,
                formatPercent(item && item.cpuUsagePercent)
            ));
            kpiGridEl.appendChild(createUsageVisual(
                '内存',
                item && item.memoryUsagePercent,
                formatPercent(item && item.memoryUsagePercent),
                formatMemoryRange(item)
            ));
            kpiGridEl.appendChild(createUsageVisual(
                '交换',
                item && item.swapUsagePercent,
                formatPercent(item && item.swapUsagePercent),
                formatSwapRange(item)
            ));
            kpiGridEl.appendChild(createUsageVisual(
                '磁盘',
                item && item.diskUsagePercent,
                formatPercent(item && item.diskUsagePercent),
                formatDiskRange(item)
            ));
            cardEl.appendChild(kpiGridEl);
            cardEl.appendChild(createLoadBlock(item));
        }
        const mainEl = document.createElement('div');
        mainEl.className = 'metric-instance-main';
        const instanceStatusNode = createInstanceStatusBlock(item);
        if (instanceStatusNode) {
            mainEl.appendChild(instanceStatusNode);
        }
        if (!ok) {
            const messageEl = document.createElement('p');
            messageEl.className = 'metric-message';
            messageEl.textContent = item && item.message ? item.message : '采集失败';
            mainEl.appendChild(messageEl);
        }
        cardEl.appendChild(mainEl);
        cardEl.appendChild(this.createOperationBar(item));
        return cardEl;
    }

    /**
     * 更新 JVM GC 趋势缓存。
     * <p>
     * 缓存以 project/env/service 为键，仅保留最近 N 个采样点，
     * 用于实例卡片中的实时折线图展示。
     * </p>
     *
     * @param {Array<Object>} list 最新指标列表
     */
    /**
     * 渲染指标摘要（结构化卡片）。
     *
     * @param {Object} options 摘要信息
     */
    renderSummary(options) {
        const summaryEl = el('metricsSummary');
        if (!summaryEl) {
            return;
        }
        const data = options || {};
        summaryEl.innerHTML = '';
        summaryEl.appendChild(createSummaryItem('状态', data.statusText || '--'));
        summaryEl.appendChild(createSummaryItem(
            '主机健康',
            (data.healthyServers === null
                || data.healthyServers === undefined
                || data.totalServers === null
                || data.totalServers === undefined)
                ? '--'
                : `${data.healthyServers}/${data.totalServers} 台`,
            'strong'
        ));
        summaryEl.appendChild(createSummaryItem(
            '主机异常',
            (data.unhealthyServers === null || data.unhealthyServers === undefined) ? '--' : `${data.unhealthyServers} 台`
        ));
        summaryEl.appendChild(createSummaryItem('平均CPU', formatPercent(data.avgCpu)));
        summaryEl.appendChild(createSummaryItem('平均内存', formatPercent(data.avgMem)));
        summaryEl.appendChild(createSummaryItem('平均磁盘', formatPercent(data.avgDisk)));
        summaryEl.appendChild(createSummaryItem('更新时间', data.timeText || '--'));
    }

    /**
     * 渲染实例运维操作区。
     *
     * @param {Object} item 指标项
     * @returns {HTMLDivElement} 操作区节点
     */
    createOperationBar(item) {
        const barEl = document.createElement('div');
        barEl.className = 'metric-ops';
        const service = item && item.service ? String(item.service) : '';
        const canManage = truthy(item && item.canManage);
        const canMonitorJvm = truthy(item && item.canMonitorJvm);
        if (!service) {
            return barEl;
        }

        if (canMonitorJvm && this.openJvmMonitor) {
            barEl.appendChild(this.createMonitorButton(service));
        }

        if (canManage && this.operateInstance) {
            const statusInfo = resolveInstanceStatus(item && item.instanceStatus);
            const pendingAction = this.getPendingAction(service);
            MANAGED_INSTANCE_ACTIONS.forEach(option => {
                barEl.appendChild(this.createManagedOperationButton(service, option, statusInfo.level, pendingAction));
            });

            const feedbackNode = this.createOperationFeedbackNode(service, pendingAction);
            if (feedbackNode) {
                barEl.appendChild(feedbackNode);
            }
            return barEl;
        }

        const noteEl = document.createElement('span');
        noteEl.className = 'metric-op-note';
        if (!canManage && !canMonitorJvm) {
            noteEl.textContent = '当前目标仅支持日志与主机指标';
        } else if (!canManage) {
            noteEl.textContent = '未配置运维脚本';
        } else {
            noteEl.textContent = '当前页面未启用实例运维';
        }
        barEl.appendChild(noteEl);
        return barEl;
    }

    /**
     * 创建中间件入口组件。
     * <p>
     * 同一环境下的服务共用一套中间件配置，因此该入口虽然挂在实例卡片上，
     * 但展示与缓存都按“项目 + 环境”维度共享，避免同环境重复请求。
     * </p>
     *
     * @param {Object} item 指标对象
     * @returns {HTMLDivElement} 中间件入口组件
     */
    createMiddlewareWidget(item, options) {
        const service = item && item.service ? String(item.service) : '';
        const middlewareCount = Number(item && item.middlewareCount);
        const placement = options && options.placement ? String(options.placement) : 'panel';
        const middlewareKey = this.buildMiddlewareKey(service);
        const wrapperEl = document.createElement('div');
        wrapperEl.className = placement === 'header' ? 'metric-middleware metric-middleware-header' : 'metric-middleware';
        wrapperEl.dataset.middlewareKey = middlewareKey;

        const triggerEl = document.createElement('button');
        triggerEl.type = 'button';
        triggerEl.className = 'metric-middleware-trigger';
        triggerEl.title = '查看当前环境共享的中间件后台';
        triggerEl.setAttribute('aria-haspopup', 'dialog');
        triggerEl.setAttribute('aria-expanded', 'false');

        const labelEl = document.createElement('span');
        labelEl.className = 'metric-middleware-trigger-label';
        labelEl.textContent = '中间件';
        triggerEl.appendChild(labelEl);

        if (Number.isFinite(middlewareCount) && middlewareCount > 0) {
            const countEl = document.createElement('span');
            countEl.className = 'metric-middleware-trigger-count';
            countEl.textContent = String(middlewareCount);
            triggerEl.appendChild(countEl);
        }

        const caretEl = document.createElement('span');
        caretEl.className = 'metric-middleware-trigger-caret';
        caretEl.setAttribute('aria-hidden', 'true');
        caretEl.textContent = '▾';
        triggerEl.appendChild(caretEl);

        const popoverEl = document.createElement('div');
        popoverEl.className = 'metric-middleware-popover';
        popoverEl.setAttribute('role', 'dialog');
        popoverEl.setAttribute('aria-label', 'middleware-console');

        const bodyEl = document.createElement('div');
        bodyEl.className = 'metric-middleware-popover-body';
        popoverEl.appendChild(bodyEl);

        const ensureLoaded = () => this.ensureMiddlewareLoaded(service, bodyEl);
        const preloadPopover = event => {
            if (event) {
                event.stopPropagation();
            }
            ensureLoaded();
        };
        const markExpanded = expanded => {
            triggerEl.setAttribute('aria-expanded', expanded ? 'true' : 'false');
        };
        triggerEl.addEventListener('mouseenter', () => markExpanded(true));
        wrapperEl.addEventListener('mouseleave', () => markExpanded(wrapperEl.classList.contains('is-open')));
        triggerEl.addEventListener('mouseenter', preloadPopover);
        triggerEl.addEventListener('focus', event => {
            markExpanded(true);
            preloadPopover(event);
        });
        triggerEl.addEventListener('blur', () => markExpanded(wrapperEl.classList.contains('is-open')));
        triggerEl.addEventListener('click', event => {
            event.preventDefault();
            preloadPopover(event);
            if (wrapperEl.classList.contains('is-open')) {
                this.closeMiddlewarePopover(wrapperEl);
                return;
            }
            this.openMiddlewarePopover(wrapperEl);
        });
        popoverEl.addEventListener('click', event => {
            event.stopPropagation();
        });

        wrapperEl.appendChild(triggerEl);
        wrapperEl.appendChild(popoverEl);
        this.renderMiddlewarePopover(bodyEl, service);
        if (this.shouldRestoreMiddlewarePopover(service)) {
            this.openMiddlewarePopover(wrapperEl);
            ensureLoaded();
        }
        return wrapperEl;
    }

    /**
     * 懒加载当前环境共享的中间件后台列表。
     *
     * @param {string} service 实例服务键
     * @param {HTMLDivElement} panelBody 浮层主体节点
     */
    /**
     * 从当前指标列表中挑选一个可代表当前环境的中间件入口目标。
     *
     * @param {Array<Object>} list 指标列表
     * @returns {{service:string,middlewareCount:number}|null} 代表目标
     */
    resolveGlobalMiddlewareTarget(list) {
        const items = Array.isArray(list) ? list : [];
        for (let i = 0; i < items.length; i++) {
            const item = items[i];
            if (!truthy(item && item.canOpenMiddleware)) {
                continue;
            }
            const service = item && item.service ? String(item.service) : '';
            if (!service) {
                continue;
            }
            return {
                service: service,
                middlewareCount: Number(item && item.middlewareCount)
            };
        }
        return null;
    }

    /**
     * 在服务器指标标题区渲染环境级中间件入口。
     *
     * @param {{service:string,middlewareCount:number}|null} target 代表目标
     */
    renderGlobalMiddlewareWidget(target) {
        const mountEl = el('metricsPanelActions');
        if (!mountEl) {
            return;
        }
        mountEl.innerHTML = '';
        if (!target || !target.service) {
            this.openMiddlewarePanelKey = '';
            this.renderedMiddlewareContextKey = '';
            mountEl.classList.add('hidden');
            return;
        }
        this.renderedMiddlewareContextKey = this.buildMiddlewareKey(target.service);
        mountEl.classList.remove('hidden');
        mountEl.appendChild(this.createMiddlewareWidget({
            service: target.service,
            middlewareCount: target.middlewareCount
        }, {
            placement: 'header'
        }));
    }

    ensureMiddlewareLoaded(service, panelBody) {
        const key = this.buildMiddlewareKey(service);
        if (!key || !panelBody) {
            return;
        }
        const cacheEntry = this.middlewareCache.get(key);
        if (cacheEntry && (cacheEntry.status === 'loading' || cacheEntry.status === 'success')) {
            this.renderMiddlewarePopover(panelBody, service);
            return;
        }

        this.middlewareCache.set(key, {
            status: 'loading',
            items: [],
            message: ''
        });
        this.renderMiddlewarePopover(panelBody, service);

        fetch(this.buildMiddlewareListUrl(service))
            .then(response => {
                if (!response.ok) {
                    throw new Error(`状态码 ${response.status}`);
                }
                return response.json();
            })
            .then(payload => {
                this.middlewareCache.set(key, {
                    status: 'success',
                    items: this.normalizeMiddlewareItems(payload),
                    message: ''
                });
                this.refreshMiddlewarePanels(service);
            })
            .catch(error => {
                const message = error && error.message ? String(error.message) : '网络异常';
                this.middlewareCache.set(key, {
                    status: 'error',
                    items: [],
                    message: `中间件配置加载失败：${message}`
                });
                this.refreshMiddlewarePanels(service);
            });
    }

    /**
     * 刷新当前页面上同一环境对应的中间件浮层内容。
     *
     * @param {string} service 实例服务键
     */
    refreshMiddlewarePanels(service) {
        const targetKey = this.buildMiddlewareKey(service);
        if (!targetKey || typeof document === 'undefined') {
            return;
        }
        document.querySelectorAll('.metric-middleware').forEach(wrapper => {
            if (!wrapper || wrapper.dataset.middlewareKey !== targetKey) {
                return;
            }
            const bodyEl = wrapper.querySelector('.metric-middleware-popover-body');
            if (bodyEl) {
                this.renderMiddlewarePopover(bodyEl, service);
            }
        });
    }

    /**
     * 渲染中间件浮层内容。
     *
     * @param {HTMLDivElement} panelBody 浮层主体节点
     * @param {string} service 实例服务键
     */
    renderMiddlewarePopover(panelBody, service) {
        if (!panelBody) {
            return;
        }
        const cacheEntry = this.middlewareCache.get(this.buildMiddlewareKey(service));
        panelBody.innerHTML = '';
        panelBody.appendChild(this.createMiddlewarePopoverHeader(cacheEntry));

        const contentEl = document.createElement('div');
        contentEl.className = 'metric-middleware-list';
        panelBody.appendChild(contentEl);

        if (!cacheEntry || cacheEntry.status === 'loading') {
            const loadingEl = document.createElement('div');
            loadingEl.className = 'metric-middleware-empty';
            loadingEl.textContent = '中间件配置加载中...';
            contentEl.appendChild(loadingEl);
            return;
        }

        if (cacheEntry.status === 'error') {
            const errorEl = document.createElement('div');
            errorEl.className = 'metric-middleware-empty error';
            errorEl.textContent = cacheEntry.message || '中间件配置加载失败';
            contentEl.appendChild(errorEl);

            const retryBtn = document.createElement('button');
            retryBtn.type = 'button';
            retryBtn.className = 'secondary metric-middleware-retry-btn';
            retryBtn.textContent = '重试';
            retryBtn.addEventListener('click', event => {
                event.preventDefault();
                event.stopPropagation();
                this.middlewareCache.delete(this.buildMiddlewareKey(service));
                this.ensureMiddlewareLoaded(service, panelBody);
            });
            contentEl.appendChild(retryBtn);
            return;
        }

        if (!cacheEntry.items || cacheEntry.items.length === 0) {
            const emptyEl = document.createElement('div');
            emptyEl.className = 'metric-middleware-empty';
            emptyEl.textContent = '当前环境未配置共享中间件后台';
            contentEl.appendChild(emptyEl);
            return;
        }

        cacheEntry.items.forEach(item => {
            contentEl.appendChild(this.createMiddlewareItem(service, item));
        });
    }

    /**
     * 创建单个中间件后台卡片。
     *
     * @param {string} service 实例服务键
     * @param {Object} item 中间件配置
     * @returns {HTMLDivElement} 卡片节点
     */
    /**
     * 创建中间件浮层头部，统一说明与关闭入口。
     *
     * @param {{items:Array<Object>}|undefined} cacheEntry 缓存条目
     * @returns {HTMLDivElement} 头部节点
     */
    createMiddlewarePopoverHeader(cacheEntry) {
        const headEl = document.createElement('div');
        headEl.className = 'metric-middleware-popover-head';

        const titleWrapEl = document.createElement('div');
        titleWrapEl.className = 'metric-middleware-popover-title-wrap';

        const titleEl = document.createElement('div');
        titleEl.className = 'metric-middleware-popover-title';
        titleEl.textContent = `${this.getEnv ? String(this.getEnv() || '') : ''} 环境中间件`;
        titleWrapEl.appendChild(titleEl);

        const descEl = document.createElement('div');
        descEl.className = 'metric-middleware-popover-desc';
        const itemCount = cacheEntry && Array.isArray(cacheEntry.items) ? cacheEntry.items.length : 0;
        descEl.textContent = itemCount > 0
            ? `已配置 ${itemCount} 个共享后台入口`
            : '统一查看当前环境共享的中间件后台';
        titleWrapEl.appendChild(descEl);

        const closeBtn = document.createElement('button');
        closeBtn.type = 'button';
        closeBtn.className = 'secondary metric-middleware-close-btn';
        closeBtn.textContent = '关闭';
        closeBtn.addEventListener('click', event => {
            event.preventDefault();
            event.stopPropagation();
            this.closeMiddlewarePopover(
                event.currentTarget && event.currentTarget.closest
                    ? event.currentTarget.closest('.metric-middleware')
                    : null
            );
        });

        headEl.appendChild(titleWrapEl);
        headEl.appendChild(closeBtn);
        return headEl;
    }

    createMiddlewareItem(service, item) {
        const itemEl = document.createElement('div');
        itemEl.className = 'metric-middleware-item';

        const headEl = document.createElement('div');
        headEl.className = 'metric-middleware-item-head';

        const titleEl = document.createElement('span');
        titleEl.className = 'metric-middleware-name';
        titleEl.textContent = item && item.name ? String(item.name) : '未命名中间件';
        headEl.appendChild(titleEl);

        if (item && item.code) {
            const codeEl = document.createElement('span');
            codeEl.className = 'metric-middleware-code';
            codeEl.textContent = String(item.code);
            headEl.appendChild(codeEl);
        }
        itemEl.appendChild(headEl);

        itemEl.appendChild(this.createMiddlewareField('账号', item && item.username ? String(item.username) : '--', false, {
            copyText: item && item.username ? String(item.username) : '',
            copyLabel: '复制账号'
        }));
        itemEl.appendChild(this.createMiddlewareField('密码', item && item.password ? String(item.password) : '--', false, {
            copyText: item && item.password ? String(item.password) : '',
            copyLabel: '复制密码'
        }));
        itemEl.appendChild(this.createMiddlewareField('地址', item && item.url ? String(item.url) : '--', false, {
            copyText: item && item.url ? String(item.url) : '',
            copyLabel: '复制地址'
        }));

        const actionRowEl = document.createElement('div');
        actionRowEl.className = 'metric-middleware-actions';

        const actionBtn = document.createElement('button');
        actionBtn.type = 'button';
        actionBtn.className = 'secondary metric-middleware-open-btn';
        actionBtn.textContent = '打开后台';
        const launchable = !!(item && item.launchable);
        actionBtn.disabled = !launchable;
        if (!launchable) {
            actionBtn.title = '当前地址格式无法识别，无法统一跳转';
        }
        actionBtn.addEventListener('click', event => {
            event.preventDefault();
            event.stopPropagation();
            if (!launchable) {
                return;
            }
            const launchUrl = this.buildMiddlewareLaunchUrl(service, item && item.code ? String(item.code) : '');
            const popup = window.open(launchUrl, '_blank');
            if (popup) {
                popup.opener = null;
            }
        });
        actionRowEl.appendChild(actionBtn);
        itemEl.appendChild(actionRowEl);
        return itemEl;
    }

    /**
     * 创建中间件信息字段行。
     *
     * @param {string} label 字段标签
     * @param {string} value 字段值
     * @param {boolean} isLink 是否按链接展示
     * @param {{copyText:string,copyLabel:string}|undefined} options 扩展配置
     * @returns {HTMLDivElement} 字段节点
     */
    createMiddlewareField(label, value, isLink, options) {
        const fieldEl = document.createElement('div');
        fieldEl.className = 'metric-middleware-field';

        const labelEl = document.createElement('span');
        labelEl.className = 'metric-middleware-field-label';
        labelEl.textContent = label;
        fieldEl.appendChild(labelEl);

        const contentEl = document.createElement('div');
        contentEl.className = 'metric-middleware-field-main';

        if (isLink && value && value !== '--') {
            const anchorEl = document.createElement('a');
            anchorEl.className = 'metric-middleware-field-value is-link';
            anchorEl.href = value;
            anchorEl.target = '_blank';
            anchorEl.rel = 'noopener noreferrer';
            anchorEl.textContent = value;
            anchorEl.title = value;
            contentEl.appendChild(anchorEl);
            fieldEl.appendChild(contentEl);
            return fieldEl;
        }

        const valueEl = document.createElement('span');
        valueEl.className = 'metric-middleware-field-value';
        valueEl.textContent = value;
        valueEl.title = value;
        contentEl.appendChild(valueEl);

        const copyText = options && options.copyText ? String(options.copyText) : '';
        if (copyText && value !== '--') {
            contentEl.appendChild(this.createMiddlewareCopyButton(copyText, options && options.copyLabel
                ? String(options.copyLabel)
                : '复制'));
        }

        fieldEl.appendChild(contentEl);
        return fieldEl;
    }

    /**
     * 创建中间件字段右侧的复制图标按钮。
     *
     * @param {string} text 待复制文本
     * @param {string} label 按钮说明
     * @returns {HTMLButtonElement} 图标按钮
     */
    createMiddlewareCopyButton(text, label) {
        const buttonEl = document.createElement('button');
        buttonEl.type = 'button';
        buttonEl.className = 'secondary metric-middleware-copy-btn';
        buttonEl.title = label || '复制';
        buttonEl.setAttribute('aria-label', label || '复制');
        buttonEl.dataset.iconDefault = 'icon-copy';
        buttonEl.dataset.iconSuccess = 'icon-check';
        this.renderMiddlewareCopyButtonIcon(buttonEl, buttonEl.dataset.iconDefault);
        buttonEl.addEventListener('click', event => {
            event.preventDefault();
            event.stopPropagation();
            this.copyMiddlewareText(text, buttonEl, '已复制');
        });
        return buttonEl;
    }

    /**
     * 复制中间件账号或密码，并给予轻量反馈。
     *
     * @param {string} text 待复制文本
     * @param {HTMLButtonElement} buttonEl 触发按钮
     * @param {string} successLabel 成功文案
     */
    copyMiddlewareText(text, buttonEl, successLabel) {
        const normalized = text ? String(text) : '';
        if (!normalized || !buttonEl) {
            return;
        }
        const fallbackCopy = () => {
            const inputEl = document.createElement('textarea');
            inputEl.value = normalized;
            inputEl.setAttribute('readonly', 'readonly');
            inputEl.style.position = 'fixed';
            inputEl.style.top = '-9999px';
            document.body.appendChild(inputEl);
            inputEl.select();
            try {
                document.execCommand('copy');
                this.flashMiddlewareCopyButton(buttonEl, successLabel);
            } finally {
                document.body.removeChild(inputEl);
            }
        };

        if (typeof navigator !== 'undefined'
            && navigator.clipboard
            && typeof navigator.clipboard.writeText === 'function') {
            navigator.clipboard.writeText(normalized)
                .then(() => this.flashMiddlewareCopyButton(buttonEl, successLabel))
                .catch(() => fallbackCopy());
            return;
        }
        fallbackCopy();
    }

    /**
     * 短暂更新复制按钮文案，反馈复制完成。
     *
     * @param {HTMLButtonElement} buttonEl 按钮节点
     * @param {string} successLabel 成功文案
     */
    flashMiddlewareCopyButton(buttonEl, successLabel) {
        if (!buttonEl) {
            return;
        }
        const originalTitle = buttonEl.dataset.originalTitle || buttonEl.title || '';
        buttonEl.dataset.originalTitle = originalTitle;
        buttonEl.title = successLabel || '已复制';
        buttonEl.setAttribute('aria-label', successLabel || '已复制');
        buttonEl.classList.add('is-success');
        this.renderMiddlewareCopyButtonIcon(buttonEl, buttonEl.dataset.iconSuccess || 'icon-check');
        if (buttonEl._middlewareCopyTimer) {
            window.clearTimeout(buttonEl._middlewareCopyTimer);
        }
        buttonEl._middlewareCopyTimer = window.setTimeout(() => {
            buttonEl.title = buttonEl.dataset.originalTitle || originalTitle;
            buttonEl.setAttribute('aria-label', buttonEl.dataset.originalTitle || originalTitle);
            buttonEl.classList.remove('is-success');
            this.renderMiddlewareCopyButtonIcon(buttonEl, buttonEl.dataset.iconDefault || 'icon-copy');
            buttonEl._middlewareCopyTimer = null;
        }, 1200);
    }

    /**
     * 渲染复制按钮图标，统一复用按钮图标雪碧图。
     *
     * @param {HTMLButtonElement} buttonEl 按钮节点
     * @param {string} iconId 图标 ID
     */
    renderMiddlewareCopyButtonIcon(buttonEl, iconId) {
        if (!buttonEl) {
            return;
        }
        buttonEl.textContent = '';
        const iconEl = createOperationButtonIcon(iconId);
        if (iconEl) {
            iconEl.classList.add('metric-middleware-copy-icon');
            buttonEl.appendChild(iconEl);
        }
    }

    /**
     * 规范化中间件后台列表响应。
     *
     * @param {Array<Object>} payload 原始响应
     * @returns {Array<Object>} 规范化列表
     */
    normalizeMiddlewareItems(payload) {
        if (!Array.isArray(payload)) {
            return [];
        }
        return payload.map(item => ({
            code: item && item.code ? String(item.code) : '',
            name: item && item.name ? String(item.name) : '',
            url: item && item.url ? String(item.url) : '',
            launchable: truthy(item && item.launchable),
            username: item && item.username ? String(item.username) : '',
            password: item && item.password ? String(item.password) : '',
            sort: Number.isFinite(Number(item && item.sort)) ? Number(item.sort) : 0
        })).sort((left, right) => {
            if (left.sort !== right.sort) {
                return left.sort - right.sort;
            }
            return String(left.name || left.code).localeCompare(String(right.name || right.code), 'zh-CN');
        });
    }

    /**
     * 构建中间件配置查询地址。
     *
     * @param {string} service 实例服务键
     * @returns {string} 查询地址
     */
    buildMiddlewareListUrl(service) {
        const project = this.getProject ? String(this.getProject() || '') : '';
        const env = this.getEnv ? String(this.getEnv() || '') : '';
        return `/api/middleware/consoles?project=${encodeURIComponent(project)}&env=${encodeURIComponent(env)}&service=${encodeURIComponent(service)}`;
    }

    /**
     * 构建中间件后台跳转地址。
     *
     * @param {string} service 实例服务键
     * @param {string} code 中间件编码
     * @returns {string} 跳转地址
     */
    buildMiddlewareLaunchUrl(service, code) {
        const project = this.getProject ? String(this.getProject() || '') : '';
        const env = this.getEnv ? String(this.getEnv() || '') : '';
        return `/api/middleware/launch?project=${encodeURIComponent(project)}&env=${encodeURIComponent(env)}&service=${encodeURIComponent(service)}&code=${encodeURIComponent(code)}`;
    }

    /**
     * 构建中间件缓存键。
     * <p>
     * 中间件配置按环境共享，因此缓存只按“项目 + 环境”隔离，
     * 不再区分同一环境下的具体实例。
     * </p>
     *
     * @param {string} service 实例服务键
     * @returns {string} 缓存键
     */
    buildMiddlewareKey(service) {
        const project = this.getProject ? String(this.getProject() || '') : '';
        const env = this.getEnv ? String(this.getEnv() || '') : '';
        if (!project || !env) {
            return '';
        }
        return `${project}#${env}#middlewares`;
    }

    /**
     * 创建 JVM 监控按钮。
     *
     * @param {string} service 实例服务名
     * @returns {HTMLButtonElement} 按钮节点
     */
    createMonitorButton(service) {
        const monitorBtn = document.createElement('button');
        monitorBtn.type = 'button';
        monitorBtn.className = OPERATION_BUTTON_META.monitor.style;
        monitorBtn.title = OPERATION_BUTTON_META.monitor.title;
        setOperationButtonVisual(monitorBtn, OPERATION_BUTTON_META.monitor.label, OPERATION_BUTTON_META.monitor.icon);
        monitorBtn.addEventListener('click', () => this.openJvmMonitor(service));
        return monitorBtn;
    }

    /**
     * 创建实例运维动作按钮。
     *
     * @param {string} service 实例服务名
     * @param {{action:string,label:string,title:string,style:string,icon:string}} option 动作配置
     * @param {string} statusLevel 实例状态级别
     * @param {string} pendingAction 执行中动作
     * @returns {HTMLButtonElement} 按钮节点
     */
    createManagedOperationButton(service, option, statusLevel, pendingAction) {
        const buttonEl = document.createElement('button');
        const isPendingAction = !!pendingAction && pendingAction === option.action;
        const disableReason = resolveActionDisableReason(statusLevel, option.action, pendingAction);
        buttonEl.type = 'button';
        buttonEl.className = `secondary metric-op-btn ${option.style}`;
        buttonEl.title = disableReason ? `${option.title}（${disableReason}）` : option.title;
        buttonEl.disabled = !!disableReason;
        setOperationButtonVisual(
            buttonEl,
            isPendingAction ? OPERATION_BUTTON_META.pending.label : option.label,
            isPendingAction ? OPERATION_BUTTON_META.pending.icon : option.icon
        );
        if (isPendingAction) {
            buttonEl.classList.add('is-pending');
        }
        buttonEl.addEventListener('click', () => this.handleOperateClick(service, option.action, option.title));
        return buttonEl;
    }

    /**
     * 创建实例动作反馈提示。
     *
     * @param {string} service 实例服务名
     * @param {string} pendingAction 执行中动作
     * @returns {HTMLSpanElement|null} 提示节点
     */
    createOperationFeedbackNode(service, pendingAction) {
        const tipEl = document.createElement('span');
        if (pendingAction) {
            tipEl.className = 'metric-op-tip pending';
            tipEl.textContent = `${actionText(pendingAction)}中...`;
            return tipEl;
        }
        const tip = this.getOperationTip(service);
        if (!tip) {
            return null;
        }
        tipEl.className = resolveOperationTipClassName(tip.tone);
        tipEl.textContent = tip.text;
        return tipEl;
    }

    /**
     * 处理实例运维按钮点击事件。
     *
     * @param {string} service 实例服务键
     * @param {string} action 动作
     * @param {string} title 按钮标题
     */
    handleOperateClick(service, action, title) {
        if (!service || !action || !this.operateInstance) {
            return;
        }
        if (this.isServicePending(service)) {
            return;
        }
        if (this.requireOperationConfirm(service, action, title)) {
            this.renderMetrics(this.lastMetrics);
            return;
        }
        this.setServicePending(service, action, true);
        this.clearOperationConfirm(service);
        this.renderMetrics(this.lastMetrics);
        this.operateInstance(service, action)
            .then(result => {
                const message = result && result.message ? String(result.message) : '执行成功';
                this.setOperationTip(service, 'success', message);
            })
            .catch(error => {
                const message = error && error.message ? String(error.message) : '执行失败';
                this.setOperationTip(service, 'error', message);
            })
            .finally(() => {
                this.setServicePending(service, action, false);
                this.renderMetrics(this.lastMetrics);
                window.setTimeout(() => this.refresh(true), 900);
            });
    }

    /**
     * 判断并处理动作二次确认。
     * <p>
     * 对重启/停止等高风险动作采用“二次点击确认”：
     * 1. 首次点击仅写入确认状态并提示；
     * 2. 在有效期内再次点击同动作才会真正执行；
     * 3. 过期或切换动作为首次点击重新确认。
     * </p>
     *
     * @param {string} service 实例服务键
     * @param {string} action 动作
     * @param {string} title 按钮标题
     * @returns {boolean} true 表示需要等待二次确认
     */
    requireOperationConfirm(service, action, title) {
        if (action !== 'restart' && action !== 'stop') {
            return false;
        }
        const key = this.buildServiceKey(service);
        const now = Date.now();
        const existing = this.operationConfirmStates.get(key);
        if (existing && existing.action === action && existing.expireAt > now) {
            this.operationConfirmStates.delete(key);
            return false;
        }
        this.operationConfirmStates.set(key, {
            action: action,
            expireAt: now + OP_CONFIRM_EXPIRE_MS
        });
        this.setOperationTip(service, 'warn', `${title}：请再次点击确认`);
        return true;
    }

    /**
     * 清理实例动作二次确认状态。
     *
     * @param {string} service 实例服务键
     */
    clearOperationConfirm(service) {
        this.operationConfirmStates.delete(this.buildServiceKey(service));
    }

    /**
     * 判断某实例是否有动作执行中。
     *
     * @param {string} service 实例服务键
     * @returns {boolean} 是否执行中
     */
    isServicePending(service) {
        return this.operationStates.has(this.buildServiceKey(service));
    }

    /**
     * 获取实例正在执行的动作。
     *
     * @param {string} service 实例服务键
     * @returns {string} 动作值
     */
    getPendingAction(service) {
        return this.operationStates.get(this.buildServiceKey(service)) || '';
    }

    /**
     * 设置实例动作执行状态。
     *
     * @param {string} service 实例服务键
     * @param {string} action 动作
     * @param {boolean} pending 是否执行中
     */
    setServicePending(service, action, pending) {
        const key = this.buildServiceKey(service);
        if (pending) {
            this.operationStates.set(key, action);
            return;
        }
        const current = this.operationStates.get(key);
        if (!current || current === action) {
            this.operationStates.delete(key);
        }
    }

    /**
     * 构建实例服务唯一键（按项目与环境隔离）。
     *
     * @param {string} service 实例服务键
     * @returns {string} 唯一键
     */
    buildServiceKey(service) {
        const project = this.getProject ? String(this.getProject() || '') : '';
        const env = this.getEnv ? String(this.getEnv() || '') : '';
        return `${project}#${env}#${service}`;
    }

    /**
     * 设置实例操作提示。
     *
     * @param {string} service 实例服务键
     * @param {string} tone 提示类型
     * @param {string} text 提示文案
     */
    setOperationTip(service, tone, text) {
        this.operationTips.set(this.buildServiceKey(service), {
            tone: tone === 'error' ? 'error' : (tone === 'warn' ? 'warn' : 'success'),
            text: String(text || ''),
            expireAt: Date.now() + 5200
        });
    }

    /**
     * 读取实例操作提示。
     *
     * @param {string} service 实例服务键
     * @returns {{tone:string,text:string}|null} 提示对象
     */
    getOperationTip(service) {
        const key = this.buildServiceKey(service);
        const tip = this.operationTips.get(key);
        if (!tip) {
            return null;
        }
        if (tip.expireAt <= Date.now()) {
            this.operationTips.delete(key);
            return null;
        }
        return tip;
    }
}

/**
 * 创建运维按钮图标节点。
 *
 * @param {string} iconId 图标 ID
 * @returns {SVGSVGElement|null} SVG 节点
 */
function createOperationButtonIcon(iconId) {
    if (!iconId) {
        return null;
    }
    const svgEl = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
    svgEl.setAttribute('class', 'btn-icon');
    svgEl.setAttribute('aria-hidden', 'true');
    const useEl = document.createElementNS('http://www.w3.org/2000/svg', 'use');
    useEl.setAttribute('href', `/assets/icons/button-icons.svg#${iconId}`);
    svgEl.appendChild(useEl);
    return svgEl;
}

/**
 * 设置运维按钮图标和文案。
 *
 * @param {HTMLButtonElement} button 按钮节点
 * @param {string} label 文案
 * @param {string} iconId 图标 ID
 */
function setOperationButtonVisual(button, label, iconId) {
    if (!button) {
        return;
    }
    button.textContent = '';
    const iconEl = createOperationButtonIcon(iconId);
    if (iconEl) {
        button.appendChild(iconEl);
    }
    const labelEl = document.createElement('span');
    labelEl.className = 'btn-label';
    labelEl.textContent = String(label || '');
    button.appendChild(labelEl);
}

/**
 * 创建摘要项。
 *
 * @param {string} label 标签
 * @param {string} value 值
 * @param {string} valueLevel 样式级别
 * @returns {HTMLDivElement} 节点
 */
function createSummaryItem(label, value, valueLevel) {
    const itemEl = document.createElement('div');
    itemEl.className = 'summary-item';

    const labelEl = document.createElement('span');
    labelEl.className = 'summary-label';
    labelEl.textContent = label;

    const valueEl = document.createElement('span');
    valueEl.className = valueLevel ? `summary-value ${valueLevel}` : 'summary-value';
    valueEl.textContent = value;

    itemEl.appendChild(labelEl);
    itemEl.appendChild(valueEl);
    return itemEl;
}

/**
 * 创建主机分组头部标签。
 *
 * @param {string} label 标签
 * @param {string} value 值
 * @param {string} level 级别
 * @param {string} kind 类型
 * @returns {HTMLSpanElement} 标签节点
 */
function createHostBadge(label, value, level, kind) {
    const badgeEl = document.createElement('span');
    const classNames = ['metric-host-badge'];
    if (kind) {
        classNames.push(`is-${kind}`);
    }
    if (level) {
        classNames.push(level);
    }
    badgeEl.className = classNames.join(' ');

    const labelEl = document.createElement('span');
    labelEl.className = 'metric-host-badge-label';
    labelEl.textContent = label;

    const valueEl = document.createElement('span');
    valueEl.className = 'metric-host-badge-value';
    valueEl.textContent = value;

    badgeEl.appendChild(labelEl);
    badgeEl.appendChild(valueEl);
    return badgeEl;
}

/**
 * 解析按钮禁用原因。
 *
 * @param {string} statusLevel 实例状态级别
 * @param {string} action 动作
 * @param {string} pendingAction 执行中动作
 * @returns {string} 禁用原因，空字符串表示可点击
 */
function resolveActionDisableReason(statusLevel, action, pendingAction) {
    if (pendingAction) {
        return `${actionText(pendingAction)}中`;
    }
    if (isActionAllowed(statusLevel, action)) {
        return '';
    }
    const levelMeta = OPERATION_DISABLE_REASON_MAP[statusLevel] || OPERATION_DISABLE_REASON_MAP.default;
    return levelMeta[action] || levelMeta.default || OPERATION_DISABLE_REASON_MAP.default.default;
}

/**
 * 判断动作在当前状态下是否可执行。
 *
 * @param {string} statusLevel 实例状态级别
 * @param {string} action 动作
 * @returns {boolean} true 表示可执行
 */
function isActionAllowed(statusLevel, action) {
    if (!ACTION_TEXT_MAP[action]) {
        return false;
    }
    if (statusLevel === 'unconfigured') {
        return false;
    }
    if (statusLevel === 'running' && action === 'start') {
        return false;
    }
    if (statusLevel === 'stopped' && action === 'stop') {
        return false;
    }
    return true;
}

/**
 * 动作文案映射。
 *
 * @param {string} action 动作
 * @returns {string} 文案
 */
function actionText(action) {
    return ACTION_TEXT_MAP[action] || '执行';
}

/**
 * 解析操作提示节点样式类。
 *
 * @param {string} tone 提示语气
 * @returns {string} 样式类名
 */
function resolveOperationTipClassName(tone) {
    if (tone === 'error') {
        return 'metric-op-tip error';
    }
    if (tone === 'warn') {
        return 'metric-op-tip pending';
    }
    return 'metric-op-tip success';
}

/**
 * 创建骨架屏占位行。
 *
 * @param {string} widthClass 宽度修饰类
 * @returns {HTMLDivElement} 占位行节点
 */
function createSkeletonLine(widthClass) {
    const lineEl = document.createElement('div');
    lineEl.className = widthClass ? `metrics-skeleton-line ${widthClass}` : 'metrics-skeleton-line';
    return lineEl;
}

/**
 * 创建使用率可视化块。
 *
 * @param {string} label 标签
 * @param {number} percent 百分比
 * @param {string} valueText 文本
 * @param {string} extraText 附加信息
 * @returns {HTMLDivElement} 节点
 */
function createUsageVisual(label, percent, valueText, extraText) {
    const wrapperEl = document.createElement('div');
    const normalizedLabel = String(label || '');
    let typeClass = '';
    if (normalizedLabel.indexOf('CPU') >= 0) {
        typeClass = 'cpu';
    } else if (normalizedLabel.indexOf('内存') >= 0
        || normalizedLabel.indexOf('交换') >= 0
        || normalizedLabel.indexOf('磁盘') >= 0) {
        typeClass = 'memory';
    }
    wrapperEl.className = typeClass ? `metric-kpi ${typeClass}` : 'metric-kpi';

    const lineEl = document.createElement('div');
    lineEl.className = 'metric-kpi-line';

    const labelEl = document.createElement('span');
    labelEl.className = 'metric-kpi-label';
    labelEl.textContent = label;
    lineEl.appendChild(labelEl);

    const level = levelClass(percent);
    const barEl = document.createElement('div');
    barEl.className = typeClass ? `metric-progress is-${typeClass}` : 'metric-progress';
    const fillEl = document.createElement('span');
    fillEl.className = level ? `metric-progress-fill ${level}` : 'metric-progress-fill';
    fillEl.style.width = `${clampPercent(percent)}%`;
    barEl.appendChild(fillEl);

    const textEl = document.createElement('span');
    textEl.className = 'metric-progress-text';

    const leftEl = document.createElement('span');
    leftEl.className = 'metric-progress-left';
    leftEl.textContent = valueText || '--';
    textEl.appendChild(leftEl);

    if (typeClass === 'memory') {
        const rightEl = document.createElement('span');
        rightEl.className = 'metric-progress-right';
        const normalizedExtra = extraText ? String(extraText).trim() : '';
        rightEl.textContent = normalizedExtra || '--/--';
        textEl.appendChild(rightEl);
    }

    barEl.appendChild(textEl);
    lineEl.appendChild(barEl);
    wrapperEl.appendChild(lineEl);
    return wrapperEl;
}

/**
 * 将负载值映射为百分比（以 4.0 为 100% 基准）。
 *
 * @param {number} value 负载值
 * @returns {number} 百分比 0~100
 */
function loadToPercent(value) {
    if (value === null || value === undefined || Number.isNaN(Number(value))) {
        return 0;
    }
    return Math.min(Math.max(0, Number(value) / 4.0 * 100), 100);
}

/**
 * 创建系统负载进度条块（以 1m 负载为主条，右侧显示 1m/5m/15m 三值）。
 *
 * @param {Object} item 指标对象
 * @returns {HTMLDivElement} 节点
 */
function createLoadBlock(item) {
    const load1m = item && item.load1m;
    const level = loadLevelClass(load1m);
    const pct = loadToPercent(load1m);
    const allText = formatLoad(item);

    const rowEl = document.createElement('div');
    rowEl.className = 'metric-kpi-line';

    const labelEl = document.createElement('span');
    labelEl.className = 'metric-kpi-label';
    labelEl.textContent = '负载';
    rowEl.appendChild(labelEl);

    const barEl = document.createElement('div');
    barEl.className = 'metric-progress is-load is-memory';
    const fillEl = document.createElement('span');
    fillEl.className = level ? `metric-progress-fill ${level}` : 'metric-progress-fill';
    fillEl.style.width = `${pct}%`;
    barEl.appendChild(fillEl);

    const textEl = document.createElement('span');
    textEl.className = 'metric-progress-text';

    const leftEl = document.createElement('span');
    leftEl.className = 'metric-progress-left';
    const load1Text = (load1m === null || load1m === undefined || Number.isNaN(Number(load1m)))
        ? '--' : Number(load1m).toFixed(2);
    leftEl.textContent = load1Text;
    textEl.appendChild(leftEl);

    const rightEl = document.createElement('span');
    rightEl.className = 'metric-progress-right';
    rightEl.textContent = allText;
    textEl.appendChild(rightEl);

    barEl.appendChild(textEl);
    rowEl.appendChild(barEl);
    return rowEl;
}

/**
 * 创建实例状态展示块。
 *
 * @param {Object} item 指标对象
 * @returns {HTMLDivElement} 节点
 */
function createInstanceStatusBlock(item) {
    if (!truthy(item && item.canManage)) {
        return null;
    }
    const statusInfo = resolveInstanceStatus(item && item.instanceStatus);
    const detailRaw = item && item.instanceStatusDetail ? String(item.instanceStatusDetail).trim() : '';

    const blockEl = document.createElement('div');
    blockEl.className = 'metric-instance-block';

    const tagEl = document.createElement('span');
    tagEl.className = `metric-instance-tag ${statusInfo.level}`;
    tagEl.textContent = statusInfo.text;
    blockEl.appendChild(tagEl);

    if (detailRaw) {
        const detailEl = document.createElement('span');
        detailEl.className = 'metric-instance-detail';
        detailEl.title = detailRaw;
        detailEl.textContent = abbreviateText(detailRaw, 120);
        blockEl.appendChild(detailEl);
    }
    return blockEl;
}

/**
 * 映射实例状态字段到前端文案与样式级别。
 *
 * @param {string} rawStatus 原始状态
 * @returns {{text:string,level:string}} 映射结果
 */
function resolveInstanceStatus(rawStatus) {
    const status = String(rawStatus || '').trim().toLowerCase();
    return INSTANCE_STATUS_META[status] || INSTANCE_STATUS_META.unknown;
}

/**
 * 统一将后端返回的布尔值、字符串布尔值转换为 true/false。
 *
 * @param {*} value 候选值
 * @returns {boolean} true 表示真值
 */
function truthy(value) {
    if (typeof value === 'boolean') {
        return value;
    }
    if (value === null || value === undefined) {
        return false;
    }
    const normalized = String(value).trim().toLowerCase();
    return normalized === 'true' || normalized === '1' || normalized === 'yes';
}

/**
 * 长文本摘要截断。
 *
 * @param {string} text 原始文本
 * @param {number} maxLength 最大长度
 * @returns {string} 截断结果
 */
function abbreviateText(text, maxLength) {
    const normalized = String(text || '');
    if (!normalized || normalized.length <= maxLength) {
        return normalized;
    }
    return `${normalized.substring(0, maxLength)}...`;
}

/**
 * 计算平均值。
 *
 * @param {Array<number>} list 数值列表
 * @returns {number|null} 平均值
 */
function average(list) {
    const values = (list || []).filter(value => value !== null && value !== undefined && !Number.isNaN(Number(value)));
    if (!values.length) {
        return null;
    }
    const total = values.reduce((sum, current) => sum + Number(current), 0);
    return total / values.length;
}

/**
 * 格式化百分比。
 *
 * @param {number} value 百分比
 * @returns {string} 文本
 */
function formatPercent(value) {
    if (value === null || value === undefined || Number.isNaN(Number(value))) {
        return '--';
    }
    return `${Number(value).toFixed(1)}%`;
}

/**
 * 格式化内存区间。
 *
 * @param {Object} item 指标对象
 * @returns {string} 文本
 */
function formatMemoryRange(item) {
    if (!item) {
        return '--/--';
    }
    const used = formatStorage(item.memoryUsedMb);
    const total = formatStorage(item.memoryTotalMb);
    return `${used}/${total}`;
}

/**
 * 格式化交换内存区间。
 *
 * @param {Object} item 指标对象
 * @returns {string} 文本
 */
function formatSwapRange(item) {
    if (!item) {
        return '--/--';
    }
    const used = formatStorage(item.swapUsedMb);
    const total = formatStorage(item.swapTotalMb);
    return `${used}/${total}`;
}

/**
 * 格式化磁盘容量区间。
 *
 * @param {Object} item 指标对象
 * @returns {string} 文本
 */
function formatDiskRange(item) {
    if (!item) {
        return '--/--';
    }
    const used = formatStorage(item.diskUsedMb);
    const total = formatStorage(item.diskTotalMb);
    return `${used}/${total}`;
}

/**
 * 自动格式化容量单位（MB/GB/TB）。
 *
 * @param {number} valueMb 容量（MB）
 * @returns {string} 文本
 */
function formatStorage(valueMb) {
    if (valueMb === null || valueMb === undefined || Number.isNaN(Number(valueMb))) {
        return '--';
    }
    const mb = Number(valueMb);
    if (mb < 1024) {
        return `${Math.round(mb)}MB`;
    }
    if (mb >= 1024 * 1024) {
        const tb = mb / (1024 * 1024);
        const precision = tb >= 10 ? 0 : 1;
        return `${tb.toFixed(precision)}TB`;
    }
    const gb = mb / 1024;
    const precision = gb >= 10 ? 0 : 1;
    return `${gb.toFixed(precision)}GB`;
}

/**
 * 格式化负载值。
 *
 * @param {Object} item 指标对象
 * @returns {string} 文本
 */
function formatLoad(item) {
    if (!item) {
        return '--';
    }
    return `${numberOrDash(item.load1m)}/${numberOrDash(item.load5m)}/${numberOrDash(item.load15m)}`;
}

/**
 * 数字或占位文案。
 *
 * @param {number} value 数值
 * @returns {string} 文本
 */
function numberOrDash(value) {
    if (value === null || value === undefined || Number.isNaN(Number(value))) {
        return '--';
    }
    return Number(value).toFixed(2);
}

/**
 * 百分比风险级别。
 *
 * @param {number} value 数值
 * @returns {string} 样式级别
 */
function levelClass(value) {
    if (value === null || value === undefined || Number.isNaN(Number(value))) {
        return '';
    }
    const number = Number(value);
    if (number >= 85) {
        return 'error';
    }
    if (number >= 70) {
        return 'warn';
    }
    return '';
}

/**
 * 负载风险级别。
 *
 * @param {number} value 数值
 * @returns {string} 样式级别
 */
function loadLevelClass(value) {
    if (value === null || value === undefined || Number.isNaN(Number(value))) {
        return '';
    }
    const number = Number(value);
    if (number >= 4) {
        return 'error';
    }
    if (number >= 2) {
        return 'warn';
    }
    return '';
}

/**
 * 百分比收敛到 0~100。
 *
 * @param {number} value 百分比
 * @returns {number} 收敛值
 */
function clampPercent(value) {
    if (value === null || value === undefined || Number.isNaN(Number(value))) {
        return 0;
    }
    const normalized = Number(value);
    if (normalized < 0) {
        return 0;
    }
    if (normalized > 100) {
        return 100;
    }
    return normalized;
}

/**
 * 格式化时间（HH:mm:ss）。
 *
 * @param {number} timestamp 毫秒时间戳
 * @returns {string} 时间文本
 */
function formatTime(timestamp) {
    const date = new Date(timestamp);
    const hh = String(date.getHours()).padStart(2, '0');
    const mm = String(date.getMinutes()).padStart(2, '0');
    const ss = String(date.getSeconds()).padStart(2, '0');
    return `${hh}:${mm}:${ss}`;
}
