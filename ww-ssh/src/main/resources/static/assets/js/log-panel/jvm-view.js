import {el} from './dom.js';

const MAX_POINTS = 180;
const DEFAULT_POLL_MS = 3000;
const DEFAULT_CHART_WINDOW = 120;
const CHART_WIDTH = 860;
const CHART_HEIGHT = 280;
const CHART_PADDING = {left: 46, right: 14, top: 16, bottom: 28};

/**
 * JVM 实时监控视图控制器。
 * <p>
 * 在日志页面内提供独立的 JVM/GC 实时监控窗口，支持轮询、图表与明细表展示。
 * </p>
 */
export class JvmViewController {

    /**
     * 构造方法。
     *
     * @param {Object} options 配置项
     * @param {Function} options.getProject 读取当前项目
     * @param {Function} options.getEnv 读取当前环境
     * @param {Function} options.getService 读取当前服务
     * @param {Function} options.isAggregate 判断是否聚合服务
     */
    constructor(options) {
        this.getProject = typeof options.getProject === 'function' ? options.getProject : (() => '');
        this.getEnv = typeof options.getEnv === 'function' ? options.getEnv : (() => '');
        this.getService = typeof options.getService === 'function' ? options.getService : (() => '');
        this.isAggregate = typeof options.isAggregate === 'function' ? options.isAggregate : (() => false);

        this.active = false;
        this.polling = true;
        this.pollMs = DEFAULT_POLL_MS;
        this.chartWindowSize = DEFAULT_CHART_WINDOW;
        this.timer = null;
        this.requestToken = 0;
        this.controller = null;
        this.metrics = [];
        this.history = new Map();
        this.preferredInstance = '';
    }

    /**
     * 初始化视图与事件。
     */
    init() {
        if (!el('jvmShell')) {
            return;
        }
        this.bindEvents();
        this.clearInstanceOptions();
        this.renderEmptyState();
        this.updatePollingButton();
        this.setStatus('请先选择项目/环境/服务，再切换到 JVM 视图', 'unknown');
    }

    /**
     * 设置视图激活状态。
     *
     * @param {boolean} active 是否激活
     */
    setActive(active) {
        const shouldActive = !!active;
        if (this.active === shouldActive) {
            if (this.active) {
                this.refresh(true);
            }
            return;
        }
        this.active = shouldActive;
        if (this.active) {
            this.startPolling();
            this.refresh(true);
            return;
        }
        this.stopPolling();
        this.abortRequest();
    }

    /**
     * 页面退出时释放资源。
     */
    stop() {
        this.stopPolling();
        this.abortRequest();
    }

    /**
     * 处理上下文变更（项目/环境/服务变化）。
     */
    handleContextChanged() {
        this.clearInstanceOptions();
        this.metrics = [];
        this.history.clear();
        if (this.active) {
            this.refresh(true);
            return;
        }
        this.renderContextHint();
    }

    /**
     * 预设实例优先项（例如从实例卡片点击 JVM 进入时）。
     *
     * @param {string} instanceService 实例服务键
     */
    setPreferredInstance(instanceService) {
        this.preferredInstance = String(instanceService || '').trim();
    }

    /**
     * 手动触发刷新。
     *
     * @param {boolean} manual 是否手动刷新
     */
    refresh(manual) {
        const project = String(this.getProject() || '').trim();
        const env = String(this.getEnv() || '').trim();
        const service = normalizeServiceGroup(String(this.getService() || '').trim());
        if (!project || !env || !service || this.isAggregate(project, env, service)) {
            this.setStatus('JVM 监控仅支持具体服务，请先选择单个服务', 'warn');
            this.renderEmptyState();
            return;
        }

        this.abortRequest();
        const token = ++this.requestToken;
        const hasAbort = typeof window.AbortController === 'function';
        this.controller = hasAbort ? new window.AbortController() : null;
        const options = this.controller ? {signal: this.controller.signal} : undefined;

        if (manual) {
            this.setStatus('JVM 指标刷新中...', 'unknown');
        }

        fetch(`/api/metrics/hosts?project=${encodeURIComponent(project)}&env=${encodeURIComponent(env)}&service=${encodeURIComponent(service)}`, options)
            .then(response => {
                if (!response.ok) {
                    throw new Error(`状态码 ${response.status}`);
                }
                return response.json();
            })
            .then(list => {
                if (token !== this.requestToken) {
                    return;
                }
                this.metrics = Array.isArray(list) ? list : [];
                this.updateInstanceOptions(this.metrics);
                this.appendHistories(this.metrics);
                this.renderAll();
                const snapshot = this.getSelectedSnapshot();
                if (snapshot) {
                    const tone = String(snapshot.jvmGcStatus || '').trim().toLowerCase() === 'ok' ? 'ok' : 'warn';
                    this.setStatus(`采样成功: ${String(snapshot.service || '--')}`, tone);
                } else {
                    this.setStatus('当前实例暂无 JVM 采样数据', 'warn');
                }
                this.setLastUpdate(Date.now());
            })
            .catch(error => {
                if (token !== this.requestToken) {
                    return;
                }
                if (error && error.name === 'AbortError') {
                    return;
                }
                this.setStatus(`JVM 采样失败: ${error.message || '网络异常'}`, 'error');
                this.renderAll();
            })
            .finally(() => {
                if (token !== this.requestToken) {
                    return;
                }
                this.controller = null;
            });
    }

    /**
     * 绑定界面事件。
     */
    bindEvents() {
        const instanceEl = el('jvmInstance');
        if (instanceEl) {
            instanceEl.addEventListener('change', () => this.renderAll());
        }

        const intervalEl = el('jvmPollInterval');
        if (intervalEl) {
            intervalEl.addEventListener('change', () => {
                const value = Number(intervalEl.value);
                this.pollMs = Number.isFinite(value) && value > 0 ? value : DEFAULT_POLL_MS;
                if (this.active && this.polling) {
                    this.startPolling();
                }
            });
        }

        const windowEl = el('jvmWindowSize');
        if (windowEl) {
            windowEl.addEventListener('change', () => {
                const value = Number(windowEl.value);
                this.chartWindowSize = Number.isFinite(value) && value > 0 ? value : DEFAULT_CHART_WINDOW;
                this.renderAll();
            });
        }

        const refreshButton = el('btnJvmRefresh');
        if (refreshButton) {
            refreshButton.addEventListener('click', () => this.refresh(true));
        }

        const pollingButton = el('btnJvmTogglePolling');
        if (pollingButton) {
            pollingButton.addEventListener('click', () => this.togglePolling());
        }
    }

    /**
     * 终止进行中的请求。
     */
    abortRequest() {
        if (!this.controller) {
            return;
        }
        this.controller.abort();
        this.controller = null;
    }

    /**
     * 启动轮询。
     */
    startPolling() {
        this.stopPolling();
        this.polling = true;
        this.updatePollingButton();
        this.timer = window.setInterval(() => this.refresh(false), this.pollMs);
    }

    /**
     * 停止轮询。
     */
    stopPolling() {
        if (this.timer) {
            window.clearInterval(this.timer);
            this.timer = null;
        }
        this.updatePollingButton();
    }

    /**
     * 切换轮询状态。
     */
    togglePolling() {
        this.polling = !this.polling;
        if (this.polling) {
            this.startPolling();
            this.setStatus('JVM 轮询已恢复', 'ok');
            return;
        }
        this.stopPolling();
        this.setStatus('JVM 轮询已暂停', 'warn');
    }

    /**
     * 更新轮询按钮文案。
     */
    updatePollingButton() {
        const button = el('btnJvmTogglePolling');
        if (!button) {
            return;
        }
        button.textContent = this.polling ? '暂停轮询' : '恢复轮询';
        button.classList.toggle('primary', this.polling);
    }

    /**
     * 渲染上下文提示。
     */
    renderContextHint() {
        const project = String(this.getProject() || '').trim();
        const env = String(this.getEnv() || '').trim();
        const service = String(this.getService() || '').trim();
        if (!project || !env || !service || this.isAggregate(project, env, service)) {
            this.setStatus('JVM 监控仅支持具体服务，请先选择单个服务', 'warn');
            this.renderEmptyState();
            return;
        }
        this.setStatus('切换到 JVM 视图后会开始实时采样', 'unknown');
    }

    /**
     * 清空实例下拉框。
     */
    clearInstanceOptions() {
        const instanceEl = el('jvmInstance');
        if (!instanceEl) {
            return;
        }
        instanceEl.innerHTML = '';
        instanceEl.add(new Option('等待采样...', ''));
    }

    /**
     * 根据最新指标刷新实例下拉框。
     *
     * @param {Array<Object>} list 指标列表
     */
    updateInstanceOptions(list) {
        const instanceEl = el('jvmInstance');
        if (!instanceEl) {
            return;
        }
        const previous = String(instanceEl.value || '').trim();
        const options = [];
        (list || []).forEach(item => {
            const instance = item && item.service ? String(item.service).trim() : '';
            if (instance && options.indexOf(instance) < 0) {
                options.push(instance);
            }
        });
        instanceEl.innerHTML = '';
        if (!options.length) {
            instanceEl.add(new Option('当前无实例', ''));
            return;
        }
        options.forEach(instance => instanceEl.add(new Option(instance, instance)));
        const prefer = this.preferredInstance || previous;
        applySelectValue(instanceEl, prefer);
        if (!instanceEl.value && instanceEl.options.length > 0) {
            instanceEl.selectedIndex = 0;
        }
        this.preferredInstance = '';
    }

    /**
     * 将采样写入历史缓存。
     *
     * @param {Array<Object>} list 指标列表
     */
    appendHistories(list) {
        (list || []).forEach(item => {
            const project = String(item && item.project ? item.project : '').trim();
            const env = String(item && item.env ? item.env : '').trim();
            const serviceGroup = normalizeServiceGroup(String(item && item.service ? item.service : ''));
            const instance = String(item && item.service ? item.service : '').trim();
            if (!project || !env || !serviceGroup || !instance) {
                return;
            }
            const key = buildHistoryKey(project, env, serviceGroup, instance);
            const point = {
                time: Number(item && item.updatedAt ? item.updatedAt : Date.now()),
                status: String(item && item.jvmGcStatus ? item.jvmGcStatus : '').trim().toLowerCase(),
                message: String(item && item.jvmGcMessage ? item.jvmGcMessage : '').trim(),
                pid: normalizeNumber(item && item.jvmPid),
                eden: normalizeNumber(item && item.jvmEdenUsagePercent),
                old: normalizeNumber(item && item.jvmOldUsagePercent),
                meta: normalizeNumber(item && item.jvmMetaUsagePercent),
                edenUsedMb: normalizeNumber(item && item.jvmEdenUsedMb),
                edenCapacityMb: normalizeNumber(item && item.jvmEdenCapacityMb),
                survivorUsedMb: normalizeNumber(item && item.jvmSurvivorUsedMb),
                survivorCapacityMb: normalizeNumber(item && item.jvmSurvivorCapacityMb),
                oldUsedMb: normalizeNumber(item && item.jvmOldUsedMb),
                oldCapacityMb: normalizeNumber(item && item.jvmOldCapacityMb),
                metaUsedMb: normalizeNumber(item && item.jvmMetaUsedMb),
                metaCapacityMb: normalizeNumber(item && item.jvmMetaCapacityMb),
                ccsUsedMb: normalizeNumber(item && item.jvmCompressedClassUsedMb),
                ccsCapacityMb: normalizeNumber(item && item.jvmCompressedClassCapacityMb),
                heapUsedMb: normalizeNumber(item && item.jvmHeapUsedMb),
                heapCapacityMb: normalizeNumber(item && item.jvmHeapCapacityMb),
                heapUsage: normalizeNumber(item && item.jvmHeapUsagePercent),
                ygc: normalizeNumber(item && item.jvmYoungGcCount),
                fgc: normalizeNumber(item && item.jvmFullGcCount),
                ygct: normalizeNumber(item && item.jvmYoungGcTimeSeconds),
                fgct: normalizeNumber(item && item.jvmFullGcTimeSeconds),
                gct: normalizeNumber(item && item.jvmTotalGcTimeSeconds)
            };
            const history = this.history.get(key) || [];
            const last = history.length ? history[history.length - 1] : null;
            if (last && Number(last.time) === Number(point.time)) {
                history[history.length - 1] = point;
            } else {
                history.push(point);
            }
            while (history.length > MAX_POINTS) {
                history.shift();
            }
            this.history.set(key, history);
        });
    }

    /**
     * 获取当前选择实例的快照。
     *
     * @returns {Object|null} 快照
     */
    getSelectedSnapshot() {
        const instance = String((el('jvmInstance') && el('jvmInstance').value) || '').trim();
        if (!instance) {
            return null;
        }
        for (let i = 0; i < this.metrics.length; i++) {
            const item = this.metrics[i];
            if (item && String(item.service || '').trim() === instance) {
                return item;
            }
        }
        return null;
    }

    /**
     * 获取当前选择实例的历史数据。
     *
     * @returns {Array<Object>} 历史列表
     */
    getSelectedHistory() {
        const project = String(this.getProject() || '').trim();
        const env = String(this.getEnv() || '').trim();
        const service = normalizeServiceGroup(String(this.getService() || '').trim());
        const instance = String((el('jvmInstance') && el('jvmInstance').value) || '').trim();
        if (!project || !env || !service || !instance) {
            return [];
        }
        const key = buildHistoryKey(project, env, service, instance);
        return this.history.get(key) || [];
    }

    /**
     * 渲染全部可视化内容。
     */
    renderAll() {
        const snapshot = this.getSelectedSnapshot();
        const history = this.getSelectedHistory();
        this.renderKpis(snapshot, history);
        this.renderCharts(snapshot, history);
        this.renderDetailTable(snapshot, history);
    }

    /**
     * 渲染空态。
     */
    renderEmptyState() {
        this.renderKpis(null, []);
        this.renderCharts(null, []);
        this.renderDetailTable(null, []);
    }

    /**
     * 渲染 KPI 区域。
     *
     * @param {Object|null} snapshot 当前快照
     * @param {Array<Object>} history 历史数据
     */
    renderKpis(snapshot, history) {
        const statusInfo = resolveGcStatus(snapshot && snapshot.jvmGcStatus);
        const stats = calculateRates(history);
        const heapUsage = normalizeNumber(snapshot && snapshot.jvmHeapUsagePercent);
        const gctTone = stats.gctDelta === null ? 'unknown' : (stats.gctDelta > 0.6 ? 'warn' : 'ok');
        const ygcTone = stats.ygcPerMin === null ? 'unknown' : (stats.ygcPerMin >= 6 ? 'warn' : 'ok');
        const fgcTone = stats.fgcPerMin === null ? 'unknown' : (stats.fgcPerMin >= 0.8 ? 'warn' : 'ok');
        const items = [
            {label: 'JVM 状态', value: statusInfo.text, tip: snapshot && snapshot.jvmGcMessage ? snapshot.jvmGcMessage : '等待采样', tone: statusInfo.level},
            {label: '进程 PID', value: integerOrDash(snapshot && snapshot.jvmPid), tip: '目标 Java 进程号', tone: 'unknown'},
            {
                label: 'Heap 使用率',
                value: percentOrDash(heapUsage),
                tip: `堆占用 ${mbPairOrDash(snapshot && snapshot.jvmHeapUsedMb, snapshot && snapshot.jvmHeapCapacityMb)}`,
                tone: percentTone(heapUsage)
            },
            {label: 'Eden 使用率', value: percentOrDash(snapshot && snapshot.jvmEdenUsagePercent), tip: '年轻代 Eden 区', tone: percentTone(snapshot && snapshot.jvmEdenUsagePercent)},
            {label: 'Old 使用率', value: percentOrDash(snapshot && snapshot.jvmOldUsagePercent), tip: '老年代', tone: percentTone(snapshot && snapshot.jvmOldUsagePercent)},
            {label: 'Meta 使用率', value: percentOrDash(snapshot && snapshot.jvmMetaUsagePercent), tip: '元空间', tone: percentTone(snapshot && snapshot.jvmMetaUsagePercent)},
            {label: 'Eden 占用', value: mbPairOrDash(snapshot && snapshot.jvmEdenUsedMb, snapshot && snapshot.jvmEdenCapacityMb), tip: '已用 / 总量', tone: percentTone(snapshot && snapshot.jvmEdenUsagePercent)},
            {label: 'Survivor 占用', value: mbPairOrDash(snapshot && snapshot.jvmSurvivorUsedMb, snapshot && snapshot.jvmSurvivorCapacityMb), tip: 'S0 + S1 已用 / 总量', tone: percentTone(calcUsagePercent(snapshot && snapshot.jvmSurvivorUsedMb, snapshot && snapshot.jvmSurvivorCapacityMb))},
            {label: 'GCT 累计', value: secondsOrDash(snapshot && snapshot.jvmTotalGcTimeSeconds), tip: `ΔGCT ${numberOrDash(stats.gctDelta)}s`, tone: gctTone},
            {label: 'YGC 频率', value: `${numberOrDash(stats.ygcPerMin)} /min`, tip: `ΔYGC ${integerOrDash(stats.ygcDelta)}`, tone: ygcTone},
            {label: 'FGC 频率', value: `${numberOrDash(stats.fgcPerMin)} /min`, tip: `ΔFGC ${integerOrDash(stats.fgcDelta)}`, tone: fgcTone}
        ];

        const grid = el('jvmKpiGrid');
        if (!grid) {
            return;
        }
        grid.innerHTML = '';
        items.forEach(item => grid.appendChild(createKpiItem(item)));
    }

    /**
     * 渲染图表区域。
     *
     * @param {Object|null} snapshot 当前快照
     * @param {Array<Object>} history 历史数据
     */
    renderCharts(snapshot, history) {
        const windowSize = Number.isFinite(this.chartWindowSize) && this.chartWindowSize > 0
            ? this.chartWindowSize
            : DEFAULT_CHART_WINDOW;
        const points = (history || []).slice(-windowSize);
        const derivedPoints = buildDerivedSeries(points);
        renderLineChart(el('jvmHeapChart'), points, [
            {field: 'eden', color: '#2f7fe0'},
            {field: 'old', color: '#e0902a'},
            {field: 'meta', color: '#13a27b'}
        ], {fixedMin: 0, fixedMax: 100, emptyText: '暂无 JVM 堆区使用率数据'});
        renderHeapStructureChart(el('jvmHeapStructChart'), resolveHeapDetailPoint(points, snapshot));

        renderGcEventChart(el('jvmCountChart'), derivedPoints);

        renderLineChart(el('jvmTimeChart'), points, [
            {field: 'ygct', color: '#315bd8'},
            {field: 'fgct', color: '#d27819'},
            {field: 'gct', color: '#0f9b73'}
        ], {fixedMin: 0, emptyText: '暂无 GC 耗时数据'});

        renderLineChart(el('jvmRateChart'), derivedPoints, [
            {field: 'ygcRate', color: '#2769d4'},
            {field: 'fgcRate', color: '#c85f20'}
        ], {fixedMin: 0, emptyText: '暂无 GC 频率数据'});

        renderLineChart(el('jvmDeltaTimeChart'), derivedPoints, [
            {field: 'ygctDelta', color: '#315bd8'},
            {field: 'fgctDelta', color: '#d27819'},
            {field: 'gctDelta', color: '#0f9b73'}
        ], {fixedMin: 0, emptyText: '暂无单轮 GC 耗时增量数据'});
    }

    /**
     * 渲染当前采样参数面板（仅展示最新采样，不累加列表）。
     *
     * @param {Object|null} snapshot 当前快照
     * @param {Array<Object>} history 历史数据
     */
    renderDetailTable(snapshot, history) {
        const panel = el('jvmSamplePanel');
        const metaEl = el('jvmDetailMeta');
        if (!panel) {
            return;
        }
        panel.innerHTML = '';

        const points = Array.isArray(history) ? history : [];
        const latest = points.length ? points[points.length - 1] : buildPointFromSnapshot(snapshot);
        let stats = createEmptyStats();
        if (points.length >= 2) {
            const previous = points[points.length - 2];
            stats = calculateDeltaStats(points[points.length - 1], previous);
        }

        if (!latest) {
            if (metaEl) {
                metaEl.textContent = '等待采样...';
            }
            const empty = document.createElement('div');
            empty.className = 'jvm-detail-empty';
            empty.textContent = '暂无采样数据';
            panel.appendChild(empty);
            return;
        }

        if (metaEl) {
            metaEl.textContent = `采样时间 ${formatDateTime(latest.time)} ｜ 图表缓存 ${points.length} 点`;
        }

        const grid = document.createElement('div');
        grid.className = 'jvm-sample-grid';
        const statusInfo = resolveGcStatus(latest.status);
        const heapUsage = resolveHeapUsagePercent(latest);
        grid.appendChild(createSampleMetric('JVM状态', statusInfo.text, latest.message || '--', statusInfo.level));
        grid.appendChild(createSampleMetric('PID', integerOrDash(latest.pid), '目标Java进程号', 'unknown'));
        grid.appendChild(createSampleMetric('Heap%', percentOrDash(heapUsage), mbPairOrDash(latest.heapUsedMb, latest.heapCapacityMb), percentTone(heapUsage)));
        grid.appendChild(createSampleMetric('Eden%', numberOrDash(latest.eden), mbPairOrDash(latest.edenUsedMb, latest.edenCapacityMb), percentTone(latest.eden)));
        grid.appendChild(createSampleMetric('Old%', numberOrDash(latest.old), mbPairOrDash(latest.oldUsedMb, latest.oldCapacityMb), percentTone(latest.old)));
        grid.appendChild(createSampleMetric('Meta%', numberOrDash(latest.meta), mbPairOrDash(latest.metaUsedMb, latest.metaCapacityMb), percentTone(latest.meta)));
        grid.appendChild(createSampleMetric('YGC', integerOrDash(latest.ygc), `Δ${integerOrDash(stats.ygcDelta)} ｜ ${numberOrDash(stats.ygcPerMin)}/min`, stats.ygcPerMin !== null && stats.ygcPerMin >= 6 ? 'warn' : 'ok'));
        grid.appendChild(createSampleMetric('FGC', integerOrDash(latest.fgc), `Δ${integerOrDash(stats.fgcDelta)} ｜ ${numberOrDash(stats.fgcPerMin)}/min`, stats.fgcPerMin !== null && stats.fgcPerMin >= 0.8 ? 'warn' : 'ok'));
        grid.appendChild(createSampleMetric('GCT(s)', numberOrDash(latest.gct), `Δ${numberOrDash(stats.gctDelta)} s`, stats.gctDelta !== null && stats.gctDelta > 0.6 ? 'warn' : 'ok'));
        panel.appendChild(grid);
    }

    /**
     * 设置状态文案。
     *
     * @param {string} text 文案
     * @param {'ok'|'warn'|'error'|'unknown'} tone 状态等级
     */
    setStatus(text, tone) {
        const statusEl = el('jvmStatusText');
        if (!statusEl) {
            return;
        }
        statusEl.textContent = String(text || '');
        statusEl.className = tone === 'error'
            ? 'jvm-status-error'
            : (tone === 'warn' ? 'jvm-status-warn' : (tone === 'ok' ? 'jvm-status-ok' : 'jvm-status-unknown'));
    }

    /**
     * 设置最后更新时间文案。
     *
     * @param {number} timestamp 时间戳
     */
    setLastUpdate(timestamp) {
        const updateEl = el('jvmLastUpdate');
        if (!updateEl) {
            return;
        }
        updateEl.textContent = `最后更新: ${formatDateTime(timestamp)}`;
    }
}

function createKpiItem(item) {
    const wrap = document.createElement('div');
    const tone = item && item.tone ? item.tone : 'unknown';
    wrap.className = `jvm-kpi-item ${tone}`;

    const label = document.createElement('div');
    label.className = 'jvm-kpi-label';
    label.textContent = item && item.label ? item.label : '--';
    wrap.appendChild(label);

    const value = document.createElement('div');
    value.className = 'jvm-kpi-value';
    value.textContent = item && item.value ? item.value : '--';
    wrap.appendChild(value);

    const tip = document.createElement('div');
    tip.className = 'jvm-kpi-tip';
    tip.textContent = item && item.tip ? item.tip : '--';
    wrap.appendChild(tip);
    return wrap;
}

function buildDerivedSeries(points) {
    const result = [];
    let previous = null;
    (points || []).forEach(point => {
        const current = point || {};
        const item = {
            time: current.time,
            ygcRate: null,
            fgcRate: null,
            ygcDelta: null,
            fgcDelta: null,
            ygctDelta: null,
            fgctDelta: null,
            gctDelta: null,
            gctDeltaMs: null
        };
        if (previous && Number(current.time) > Number(previous.time)) {
            const stats = calculateDeltaStats(current, previous);
            item.ygcRate = stats.ygcPerMin;
            item.fgcRate = stats.fgcPerMin;
            item.ygcDelta = stats.ygcDelta;
            item.fgcDelta = stats.fgcDelta;
            item.ygctDelta = stats.ygctDelta;
            item.fgctDelta = stats.fgctDelta;
            item.gctDelta = stats.gctDelta;
            item.gctDeltaMs = stats.gctDelta === null ? null : stats.gctDelta * 1000;
        }
        result.push(item);
        if (Number.isFinite(Number(current.time))) {
            previous = current;
        }
    });
    return result;
}

/**
 * 解析用于“堆详细占用”图的最新数据点。
 *
 * @param {Array<Object>} points 历史点位
 * @param {Object|null} snapshot 当前快照
 * @returns {Object|null} 详情点位
 */
function resolveHeapDetailPoint(points, snapshot) {
    const latest = points && points.length ? points[points.length - 1] : null;
    if (hasHeapDetailMetrics(latest)) {
        return latest;
    }
    if (!snapshot) {
        return null;
    }
    return {
        edenUsedMb: normalizeNumber(snapshot.jvmEdenUsedMb),
        edenCapacityMb: normalizeNumber(snapshot.jvmEdenCapacityMb),
        survivorUsedMb: normalizeNumber(snapshot.jvmSurvivorUsedMb),
        survivorCapacityMb: normalizeNumber(snapshot.jvmSurvivorCapacityMb),
        oldUsedMb: normalizeNumber(snapshot.jvmOldUsedMb),
        oldCapacityMb: normalizeNumber(snapshot.jvmOldCapacityMb),
        metaUsedMb: normalizeNumber(snapshot.jvmMetaUsedMb),
        metaCapacityMb: normalizeNumber(snapshot.jvmMetaCapacityMb),
        ccsUsedMb: normalizeNumber(snapshot.jvmCompressedClassUsedMb),
        ccsCapacityMb: normalizeNumber(snapshot.jvmCompressedClassCapacityMb)
    };
}

/**
 * 判断点位是否包含可渲染的堆详细容量数据。
 *
 * @param {Object|null} point 数据点
 * @returns {boolean} 是否可渲染
 */
function hasHeapDetailMetrics(point) {
    if (!point) {
        return false;
    }
    const capacities = [
        normalizeNumber(point.edenCapacityMb),
        normalizeNumber(point.survivorCapacityMb),
        normalizeNumber(point.oldCapacityMb),
        normalizeNumber(point.metaCapacityMb),
        normalizeNumber(point.ccsCapacityMb)
    ];
    return capacities.some(value => value !== null && value > 0);
}

/**
 * 渲染堆详细占用图（条形图）。
 *
 * @param {SVGElement|null} svg 图表节点
 * @param {Object|null} point 最新点位
 */
function renderHeapStructureChart(svg, point) {
    if (!svg) {
        return;
    }
    while (svg.firstChild) {
        svg.removeChild(svg.firstChild);
    }
    if (!hasHeapDetailMetrics(point)) {
        renderChartEmpty(svg, '暂无详细堆容量数据');
        return;
    }
    const bars = [
        {label: 'Eden', used: normalizeNumber(point.edenUsedMb), capacity: normalizeNumber(point.edenCapacityMb), color: '#2f7fe0'},
        {label: 'Survivor', used: normalizeNumber(point.survivorUsedMb), capacity: normalizeNumber(point.survivorCapacityMb), color: '#8c6ad9'},
        {label: 'Old', used: normalizeNumber(point.oldUsedMb), capacity: normalizeNumber(point.oldCapacityMb), color: '#e0902a'},
        {label: 'Meta', used: normalizeNumber(point.metaUsedMb), capacity: normalizeNumber(point.metaCapacityMb), color: '#13a27b'},
        {label: 'CCS', used: normalizeNumber(point.ccsUsedMb), capacity: normalizeNumber(point.ccsCapacityMb), color: '#2f9ea1'}
    ];
    const left = 94;
    const right = 170;
    const top = 24;
    const rowGap = 10;
    const rowHeight = 22;
    const plotWidth = CHART_WIDTH - left - right;
    const axisTop = top - 6;

    const axisStart = createSvg('text');
    axisStart.setAttribute('x', String(left));
    axisStart.setAttribute('y', String(axisTop));
    axisStart.setAttribute('text-anchor', 'start');
    axisStart.setAttribute('class', 'jvm-chart-axis-label');
    axisStart.textContent = '0%';
    svg.appendChild(axisStart);

    const axisEnd = createSvg('text');
    axisEnd.setAttribute('x', String(left + plotWidth));
    axisEnd.setAttribute('y', String(axisTop));
    axisEnd.setAttribute('text-anchor', 'end');
    axisEnd.setAttribute('class', 'jvm-chart-axis-label');
    axisEnd.textContent = '100%';
    svg.appendChild(axisEnd);

    bars.forEach((item, index) => {
        const y = top + index * (rowHeight + rowGap);
        const capacity = item.capacity;
        const used = item.used;
        const ratio = calcUsagePercent(used, capacity);

        const label = createSvg('text');
        label.setAttribute('x', '10');
        label.setAttribute('y', String(y + rowHeight - 5));
        label.setAttribute('class', 'jvm-heap-row-label');
        label.textContent = item.label;
        svg.appendChild(label);

        const bg = createSvg('rect');
        bg.setAttribute('x', String(left));
        bg.setAttribute('y', String(y));
        bg.setAttribute('width', String(plotWidth));
        bg.setAttribute('height', String(rowHeight));
        bg.setAttribute('rx', '8');
        bg.setAttribute('class', 'jvm-heap-row-bg');
        svg.appendChild(bg);

        if (ratio !== null) {
            const width = Math.max(2, Math.min(plotWidth, plotWidth * ratio / 100));
            const usedBar = createSvg('rect');
            usedBar.setAttribute('x', String(left));
            usedBar.setAttribute('y', String(y));
            usedBar.setAttribute('width', String(width));
            usedBar.setAttribute('height', String(rowHeight));
            usedBar.setAttribute('rx', '8');
            usedBar.setAttribute('style', `fill:${item.color};opacity:0.9;`);
            svg.appendChild(usedBar);
        }

        const text = createSvg('text');
        text.setAttribute('x', String(left + plotWidth + 10));
        text.setAttribute('y', String(y + rowHeight - 5));
        text.setAttribute('class', 'jvm-heap-row-value');
        text.textContent = `${mbPairOrDash(used, capacity)} (${percentOrDash(ratio)})`;
        svg.appendChild(text);
    });
}

/**
 * 渲染 GC 事件图（参考 JVisualVM）：YGC/FGC 增量柱状 + ΔGCT(ms) 折线。
 *
 * @param {SVGElement|null} svg 图表节点
 * @param {Array<Object>} points 历史点位（已派生增量字段）
 */
function renderGcEventChart(svg, points) {
    if (!svg) {
        return;
    }
    while (svg.firstChild) {
        svg.removeChild(svg.firstChild);
    }
    const rows = (points || []).filter(item => item && Number.isFinite(Number(item.time)));
    const hasCount = rows.some(item => normalizeNumber(item.ygcDelta) !== null || normalizeNumber(item.fgcDelta) !== null);
    const hasTime = rows.some(item => normalizeNumber(item.gctDeltaMs) !== null);
    if (!rows.length || (!hasCount && !hasTime)) {
        renderChartEmpty(svg, '暂无 GC 事件数据');
        return;
    }

    const plotWidth = CHART_WIDTH - CHART_PADDING.left - CHART_PADDING.right;
    const plotHeight = CHART_HEIGHT - CHART_PADDING.top - CHART_PADDING.bottom;
    const len = rows.length;
    const xFor = index => {
        if (len <= 1) {
            return CHART_PADDING.left + plotWidth / 2;
        }
        return CHART_PADDING.left + (index / (len - 1)) * plotWidth;
    };

    const maxCount = Math.max(1, ...rows.map(item => {
        const ygc = normalizeNumber(item.ygcDelta) || 0;
        const fgc = normalizeNumber(item.fgcDelta) || 0;
        return Math.max(ygc, fgc);
    }));
    const maxMs = Math.max(1, ...rows.map(item => normalizeNumber(item.gctDeltaMs) || 0));

    for (let i = 0; i <= 4; i++) {
        const ratio = i / 4;
        const y = CHART_PADDING.top + ratio * plotHeight;
        const line = createSvg('line');
        line.setAttribute('x1', String(CHART_PADDING.left));
        line.setAttribute('x2', String(CHART_PADDING.left + plotWidth));
        line.setAttribute('y1', String(y));
        line.setAttribute('y2', String(y));
        line.setAttribute('class', 'jvm-chart-grid-line');
        svg.appendChild(line);

        const leftLabel = createSvg('text');
        leftLabel.setAttribute('x', String(CHART_PADDING.left - 6));
        leftLabel.setAttribute('y', String(y + 4));
        leftLabel.setAttribute('text-anchor', 'end');
        leftLabel.setAttribute('class', 'jvm-chart-axis-label');
        leftLabel.textContent = formatAxisValue(maxCount - ratio * maxCount);
        svg.appendChild(leftLabel);

        const rightLabel = createSvg('text');
        rightLabel.setAttribute('x', String(CHART_PADDING.left + plotWidth + 6));
        rightLabel.setAttribute('y', String(y + 4));
        rightLabel.setAttribute('text-anchor', 'start');
        rightLabel.setAttribute('class', 'jvm-chart-axis-label');
        rightLabel.textContent = `${formatAxisValue(maxMs - ratio * maxMs)}ms`;
        svg.appendChild(rightLabel);
    }

    const labelIndexes = [0, Math.floor((len - 1) / 2), len - 1]
        .filter((value, index, array) => array.indexOf(value) === index);
    labelIndexes.forEach(index => {
        const x = xFor(index);
        const label = createSvg('text');
        label.setAttribute('x', String(x));
        label.setAttribute('y', String(CHART_HEIGHT - 8));
        label.setAttribute('text-anchor', index === 0 ? 'start' : (index === len - 1 ? 'end' : 'middle'));
        label.setAttribute('class', 'jvm-chart-axis-label');
        label.textContent = formatTime(rows[index].time);
        svg.appendChild(label);
    });

    const step = len <= 1 ? plotWidth : (plotWidth / (len - 1));
    const barWidth = Math.max(3, Math.min(14, step * 0.3));
    rows.forEach((item, index) => {
        const centerX = xFor(index);
        const ygcDelta = normalizeNumber(item.ygcDelta);
        const fgcDelta = normalizeNumber(item.fgcDelta);

        if (ygcDelta !== null && ygcDelta > 0) {
            const ygcHeight = Math.max(2, ygcDelta * plotHeight / maxCount);
            const ygcBar = createSvg('rect');
            ygcBar.setAttribute('x', String(centerX - barWidth - 1));
            ygcBar.setAttribute('y', String(CHART_PADDING.top + plotHeight - ygcHeight));
            ygcBar.setAttribute('width', String(barWidth));
            ygcBar.setAttribute('height', String(ygcHeight));
            ygcBar.setAttribute('rx', '2');
            ygcBar.setAttribute('class', 'jvm-chart-event-bar');
            ygcBar.setAttribute('style', 'fill:#2769d4;');
            svg.appendChild(ygcBar);
        }

        if (fgcDelta !== null && fgcDelta > 0) {
            const fgcHeight = Math.max(2, fgcDelta * plotHeight / maxCount);
            const fgcBar = createSvg('rect');
            fgcBar.setAttribute('x', String(centerX + 1));
            fgcBar.setAttribute('y', String(CHART_PADDING.top + plotHeight - fgcHeight));
            fgcBar.setAttribute('width', String(barWidth));
            fgcBar.setAttribute('height', String(fgcHeight));
            fgcBar.setAttribute('rx', '2');
            fgcBar.setAttribute('class', 'jvm-chart-event-bar');
            fgcBar.setAttribute('style', 'fill:#c85f20;');
            svg.appendChild(fgcBar);
        }
    });

    const linePoints = [];
    let lastCoord = null;
    rows.forEach((item, index) => {
        const value = normalizeNumber(item.gctDeltaMs);
        if (value === null) {
            return;
        }
        const x = xFor(index);
        const y = CHART_PADDING.top + (maxMs - value) * plotHeight / maxMs;
        linePoints.push(`${x},${y}`);
        lastCoord = {x, y};
    });
    if (linePoints.length) {
        const line = createSvg('polyline');
        line.setAttribute('points', linePoints.join(' '));
        line.setAttribute('class', 'jvm-chart-series');
        line.setAttribute('style', 'stroke:#0f9b73;stroke-dasharray:4 3;');
        svg.appendChild(line);

        if (lastCoord) {
            const dot = createSvg('circle');
            dot.setAttribute('cx', String(lastCoord.x));
            dot.setAttribute('cy', String(lastCoord.y));
            dot.setAttribute('class', 'jvm-chart-point');
            dot.setAttribute('style', 'fill:#0f9b73;');
            svg.appendChild(dot);
        }
    }
}

function renderLineChart(svg, points, seriesDefs, options) {
    if (!svg) {
        return;
    }
    while (svg.firstChild) {
        svg.removeChild(svg.firstChild);
    }
    const plotWidth = CHART_WIDTH - CHART_PADDING.left - CHART_PADDING.right;
    const plotHeight = CHART_HEIGHT - CHART_PADDING.top - CHART_PADDING.bottom;

    const values = [];
    (points || []).forEach(point => {
        (seriesDefs || []).forEach(series => {
            const value = normalizeNumber(point && series ? point[series.field] : null);
            if (value !== null) {
                values.push(value);
            }
        });
    });
    if (!values.length) {
        renderChartEmpty(svg, options && options.emptyText ? options.emptyText : '暂无数据');
        return;
    }

    let yMin = options && Number.isFinite(options.fixedMin) ? Number(options.fixedMin) : Math.min.apply(null, values);
    let yMax = options && Number.isFinite(options.fixedMax) ? Number(options.fixedMax) : Math.max.apply(null, values);
    if (yMax <= yMin) {
        yMax = yMin + 1;
    }
    if (!(options && Number.isFinite(options.fixedMax))) {
        const extra = (yMax - yMin) * 0.08;
        yMax += extra;
    }

    for (let i = 0; i <= 4; i++) {
        const ratio = i / 4;
        const y = CHART_PADDING.top + ratio * plotHeight;
        const line = createSvg('line');
        line.setAttribute('x1', String(CHART_PADDING.left));
        line.setAttribute('x2', String(CHART_PADDING.left + plotWidth));
        line.setAttribute('y1', String(y));
        line.setAttribute('y2', String(y));
        line.setAttribute('class', 'jvm-chart-grid-line');
        svg.appendChild(line);

        const label = createSvg('text');
        label.setAttribute('x', String(CHART_PADDING.left - 6));
        label.setAttribute('y', String(y + 4));
        label.setAttribute('text-anchor', 'end');
        label.setAttribute('class', 'jvm-chart-axis-label');
        label.textContent = String((yMax - ratio * (yMax - yMin)).toFixed(1));
        svg.appendChild(label);
    }

    const len = points.length;
    const labelIndexes = [0, Math.floor((len - 1) / 2), len - 1].filter((value, index, array) => array.indexOf(value) === index);
    labelIndexes.forEach(index => {
        const x = CHART_PADDING.left + (len <= 1 ? 0 : (index / (len - 1)) * plotWidth);
        const label = createSvg('text');
        label.setAttribute('x', String(x));
        label.setAttribute('y', String(CHART_HEIGHT - 8));
        label.setAttribute('text-anchor', index === 0 ? 'start' : (index === len - 1 ? 'end' : 'middle'));
        label.setAttribute('class', 'jvm-chart-axis-label');
        label.textContent = formatTime(points[index].time);
        svg.appendChild(label);
    });

    (seriesDefs || []).forEach(series => {
        const segment = [];
        let lastCoord = null;
        points.forEach((point, index) => {
            const value = normalizeNumber(point && series ? point[series.field] : null);
            if (value === null) {
                return;
            }
            const x = CHART_PADDING.left + (len <= 1 ? 0 : (index / (len - 1)) * plotWidth);
            const y = CHART_PADDING.top + (yMax - value) * plotHeight / (yMax - yMin);
            segment.push(`${x},${y}`);
            lastCoord = {x, y};
        });
        if (!segment.length) {
            return;
        }
        const polyline = createSvg('polyline');
        polyline.setAttribute('points', segment.join(' '));
        polyline.setAttribute('class', 'jvm-chart-series');
        polyline.setAttribute('style', `stroke:${series.color}`);
        svg.appendChild(polyline);
        if (lastCoord) {
            const dot = createSvg('circle');
            dot.setAttribute('cx', String(lastCoord.x));
            dot.setAttribute('cy', String(lastCoord.y));
            dot.setAttribute('class', 'jvm-chart-point');
            dot.setAttribute('style', `fill:${series.color}`);
            svg.appendChild(dot);
        }
    });
}

function renderChartEmpty(svg, text) {
    const message = createSvg('text');
    message.setAttribute('x', String(CHART_WIDTH / 2));
    message.setAttribute('y', String(CHART_HEIGHT / 2));
    message.setAttribute('text-anchor', 'middle');
    message.setAttribute('class', 'jvm-chart-empty');
    message.textContent = text;
    svg.appendChild(message);
}

function createSvg(tag) {
    return document.createElementNS('http://www.w3.org/2000/svg', tag);
}

function createSampleMetric(label, value, tip, tone) {
    const item = document.createElement('div');
    item.className = `jvm-sample-item ${tone || 'unknown'}`;
    const labelEl = document.createElement('span');
    labelEl.className = 'jvm-sample-label';
    labelEl.textContent = label;
    const valueEl = document.createElement('span');
    valueEl.className = 'jvm-sample-value';
    valueEl.textContent = value;
    const tipEl = document.createElement('span');
    tipEl.className = 'jvm-sample-tip';
    tipEl.textContent = tip || '--';
    item.appendChild(labelEl);
    item.appendChild(valueEl);
    item.appendChild(tipEl);
    return item;
}

function buildPointFromSnapshot(snapshot) {
    if (!snapshot) {
        return null;
    }
    return {
        time: Number(snapshot.updatedAt || Date.now()),
        status: String(snapshot.jvmGcStatus || '').trim().toLowerCase(),
        message: String(snapshot.jvmGcMessage || '').trim(),
        pid: normalizeNumber(snapshot.jvmPid),
        eden: normalizeNumber(snapshot.jvmEdenUsagePercent),
        old: normalizeNumber(snapshot.jvmOldUsagePercent),
        meta: normalizeNumber(snapshot.jvmMetaUsagePercent),
        edenUsedMb: normalizeNumber(snapshot.jvmEdenUsedMb),
        edenCapacityMb: normalizeNumber(snapshot.jvmEdenCapacityMb),
        oldUsedMb: normalizeNumber(snapshot.jvmOldUsedMb),
        oldCapacityMb: normalizeNumber(snapshot.jvmOldCapacityMb),
        metaUsedMb: normalizeNumber(snapshot.jvmMetaUsedMb),
        metaCapacityMb: normalizeNumber(snapshot.jvmMetaCapacityMb),
        heapUsedMb: normalizeNumber(snapshot.jvmHeapUsedMb),
        heapCapacityMb: normalizeNumber(snapshot.jvmHeapCapacityMb),
        heapUsage: normalizeNumber(snapshot.jvmHeapUsagePercent),
        ygc: normalizeNumber(snapshot.jvmYoungGcCount),
        fgc: normalizeNumber(snapshot.jvmFullGcCount),
        gct: normalizeNumber(snapshot.jvmTotalGcTimeSeconds)
    };
}

function calculateRates(history) {
    const points = (history || []).filter(item => item && Number.isFinite(item.time));
    if (points.length < 2) {
        return createEmptyStats();
    }
    const last = points[points.length - 1];
    let prev = null;
    for (let i = points.length - 2; i >= 0; i--) {
        if (points[i] && Number(points[i].time) < Number(last.time)) {
            prev = points[i];
            break;
        }
    }
    if (!prev) {
        return createEmptyStats();
    }
    return calculateDeltaStats(last, prev);
}

function createEmptyStats() {
    return {
        ygcPerMin: null,
        fgcPerMin: null,
        ygcDelta: null,
        fgcDelta: null,
        ygctDelta: null,
        fgctDelta: null,
        gctDelta: null
    };
}

function calculateDeltaStats(current, previous) {
    const empty = createEmptyStats();
    const currentTime = Number(current && current.time);
    const previousTime = Number(previous && previous.time);
    if (!Number.isFinite(currentTime) || !Number.isFinite(previousTime) || currentTime <= previousTime) {
        return empty;
    }
    const deltaSeconds = (currentTime - previousTime) / 1000;
    if (deltaSeconds <= 0) {
        return empty;
    }
    const ygcDelta = safeDelta(current && current.ygc, previous && previous.ygc);
    const fgcDelta = safeDelta(current && current.fgc, previous && previous.fgc);
    const ygctDelta = safeDelta(current && current.ygct, previous && previous.ygct);
    const fgctDelta = safeDelta(current && current.fgct, previous && previous.fgct);
    const gctDelta = safeDelta(current && current.gct, previous && previous.gct);
    return {
        ygcPerMin: ygcDelta === null ? null : ygcDelta * 60 / deltaSeconds,
        fgcPerMin: fgcDelta === null ? null : fgcDelta * 60 / deltaSeconds,
        ygcDelta: ygcDelta,
        fgcDelta: fgcDelta,
        ygctDelta: ygctDelta,
        fgctDelta: fgctDelta,
        gctDelta: gctDelta
    };
}

function safeDelta(current, previous) {
    const c = normalizeNumber(current);
    const p = normalizeNumber(previous);
    if (c === null || p === null) {
        return null;
    }
    return c >= p ? (c - p) : null;
}

function buildHistoryKey(project, env, service, instance) {
    return `${project}#${env}#${service}#${instance}`;
}

function normalizeServiceGroup(serviceKey) {
    const normalized = String(serviceKey || '').trim();
    if (!normalized) {
        return '';
    }
    const index = normalized.indexOf('@');
    if (index <= 0) {
        return normalized;
    }
    return normalized.substring(0, index);
}

function applySelectValue(selectEl, value) {
    const target = String(value || '').trim();
    if (!selectEl || !selectEl.options || !selectEl.options.length) {
        return;
    }
    if (target) {
        const hit = Array.from(selectEl.options).some(option => option.value === target);
        if (hit) {
            selectEl.value = target;
            return;
        }
    }
    selectEl.selectedIndex = 0;
}

function resolveGcStatus(status) {
    const normalized = String(status || '').trim().toLowerCase();
    if (normalized === 'ok') {
        return {text: '采集正常', level: 'ok'};
    }
    if (normalized === 'no_pid') {
        return {text: '未运行', level: 'unknown'};
    }
    if (normalized === 'no_jstat') {
        return {text: '缺少 jstat', level: 'warn'};
    }
    if (normalized === 'parse_error') {
        return {text: '解析失败', level: 'warn'};
    }
    if (normalized === 'error') {
        return {text: '采集异常', level: 'error'};
    }
    return {text: '待采集', level: 'unknown'};
}

function percentTone(value) {
    const number = normalizeNumber(value);
    if (number === null) {
        return 'unknown';
    }
    if (number >= 90) {
        return 'error';
    }
    if (number >= 75) {
        return 'warn';
    }
    return 'ok';
}

function normalizeNumber(value) {
    if (value === null || value === undefined) {
        return null;
    }
    const number = Number(value);
    if (!Number.isFinite(number)) {
        return null;
    }
    return number;
}

function integerOrDash(value) {
    const number = normalizeNumber(value);
    return number === null ? '--' : String(Math.round(number));
}

function numberOrDash(value) {
    const number = normalizeNumber(value);
    return number === null ? '--' : number.toFixed(2);
}

function formatAxisValue(value) {
    const number = normalizeNumber(value);
    if (number === null) {
        return '--';
    }
    if (number >= 100 || number === 0) {
        return String(Math.round(number));
    }
    if (number >= 10) {
        return number.toFixed(1);
    }
    return number.toFixed(2);
}

function percentOrDash(value) {
    const number = normalizeNumber(value);
    return number === null ? '--' : `${number.toFixed(2)}%`;
}

function calcUsagePercent(used, capacity) {
    const usedNumber = normalizeNumber(used);
    const capacityNumber = normalizeNumber(capacity);
    if (usedNumber === null || capacityNumber === null || capacityNumber <= 0) {
        return null;
    }
    return usedNumber * 100 / capacityNumber;
}

function resolveHeapUsagePercent(point) {
    if (!point) {
        return null;
    }
    const explicit = normalizeNumber(point.heapUsage);
    if (explicit !== null) {
        return explicit;
    }
    return calcUsagePercent(point.heapUsedMb, point.heapCapacityMb);
}

function mbPairOrDash(used, capacity) {
    const usedNumber = normalizeNumber(used);
    const capacityNumber = normalizeNumber(capacity);
    if (usedNumber === null || capacityNumber === null) {
        return '--';
    }
    return `${usedNumber.toFixed(1)} / ${capacityNumber.toFixed(1)} MB`;
}

function secondsOrDash(value) {
    const number = normalizeNumber(value);
    return number === null ? '--' : `${number.toFixed(3)}s`;
}

function formatTime(timestamp) {
    const date = new Date(Number(timestamp || Date.now()));
    const hh = String(date.getHours()).padStart(2, '0');
    const mm = String(date.getMinutes()).padStart(2, '0');
    const ss = String(date.getSeconds()).padStart(2, '0');
    return `${hh}:${mm}:${ss}`;
}

function formatDateTime(timestamp) {
    const date = new Date(Number(timestamp || Date.now()));
    const yyyy = String(date.getFullYear());
    const mm = String(date.getMonth() + 1).padStart(2, '0');
    const dd = String(date.getDate()).padStart(2, '0');
    return `${yyyy}-${mm}-${dd} ${formatTime(timestamp)}`;
}
