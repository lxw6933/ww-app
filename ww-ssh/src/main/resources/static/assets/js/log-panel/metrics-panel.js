import {el} from './dom.js';

/**
 * 高风险动作二次确认有效期（毫秒）。
 */
const OP_CONFIRM_EXPIRE_MS = 4200;

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
        this.timer = null;
        this.token = 0;
        this.lastMetrics = [];
        this.operationStates = new Map();
        this.operationTips = new Map();
        this.operationConfirmStates = new Map();
        this.loading = false;
        this.activeRequestController = null;
        this.activeQueryKey = '';
        this.requestInFlight = false;
    }

    /**
     * 初始化指标面板。
     */
    init() {
        this.renderEmpty('请选择项目、环境与服务后查看服务器指标');
        this.startPolling();
    }

    /**
     * 启动轮询任务。
     */
    startPolling() {
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
        this.cancelActiveRequest();
        this.setLoading(false);
    }

    /**
     * 刷新指标数据。
     *
     * @param {boolean} manual 是否手动刷新
     */
    refresh(manual) {
        const project = this.getProject();
        const env = this.getEnv();
        const service = this.getService();
        if (!project || !env || !service) {
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

        fetch(`/api/metrics/hosts?project=${encodeURIComponent(project)}&env=${encodeURIComponent(env)}&service=${encodeURIComponent(service)}`, options)
            .then(response => {
                if (!response.ok) {
                    throw new Error(`状态码 ${response.status}`);
                }
                return response.json();
            })
            .then(list => {
                if (currentToken !== this.token) {
                    return;
                }
                this.renderMetrics(Array.isArray(list) ? list : []);
                this.setLoading(false);
            })
            .catch(error => {
                if (currentToken !== this.token) {
                    return;
                }
                if (error && error.name === 'AbortError') {
                    return;
                }
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
     * 渲染空态。
     *
     * @param {string} text 文案
     */
    renderEmpty(text) {
        this.setLoading(false);
        this.lastMetrics = [];
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
    renderMetrics(list) {
        this.lastMetrics = Array.isArray(list) ? list.slice() : [];
        const listEl = el('metricsList');
        listEl.innerHTML = '';
        if (!list || list.length === 0) {
            this.renderEmpty('未查询到实例指标，请检查服务配置');
            return;
        }

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
            statusText: '采集正常',
            healthyServers: healthyServers,
            totalServers: totalServers,
            unhealthyServers: unhealthyServers,
            avgCpu: avgCpu,
            avgMem: avgMem,
            avgDisk: avgDisk,
            timeText: formatTime(Date.now())
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
        mainEl.appendChild(createInstanceStatusBlock(item));
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
        if (!service) {
            return barEl;
        }

        if (this.openJvmMonitor) {
            const monitorBtn = document.createElement('button');
            monitorBtn.type = 'button';
            monitorBtn.className = 'secondary metric-op-btn';
            monitorBtn.title = '打开 JVM 监控';
            setOperationButtonVisual(monitorBtn, 'JVM', 'icon-monitor');
            monitorBtn.addEventListener('click', () => this.openJvmMonitor(service));
            barEl.appendChild(monitorBtn);
        }

        if (!item || !item.canManage || !this.operateInstance) {
            const noteEl = document.createElement('span');
            noteEl.className = 'metric-op-note';
            noteEl.textContent = '未配置运维脚本';
            barEl.appendChild(noteEl);
            return barEl;
        }

        const actionList = [
            {action: 'start', label: '启动', title: '启动实例', style: 'start', icon: 'icon-play'},
            {action: 'restart', label: '重启', title: '重启实例', style: 'restart', icon: 'icon-restart'},
            {action: 'stop', label: '停止', title: '停止实例', style: 'stop', icon: 'icon-stop'}
        ];
        const statusInfo = resolveInstanceStatus(item && item.instanceStatus);
        const pendingAction = this.getPendingAction(service);
        actionList.forEach(option => {
            const buttonEl = document.createElement('button');
            buttonEl.type = 'button';
            buttonEl.className = `secondary metric-op-btn ${option.style}`;
            buttonEl.title = option.title;
            setOperationButtonVisual(buttonEl, option.label, option.icon);
            const disableReason = resolveActionDisableReason(statusInfo.level, option.action, pendingAction);
            if (disableReason) {
                buttonEl.disabled = true;
                buttonEl.title = `${option.title}（${disableReason}）`;
            } else {
                buttonEl.disabled = false;
            }
            if (pendingAction && pendingAction === option.action) {
                buttonEl.classList.add('is-pending');
                setOperationButtonVisual(buttonEl, '执行中', 'icon-loading');
            }
            buttonEl.addEventListener('click', () => this.handleOperateClick(service, option.action, option.title));
            barEl.appendChild(buttonEl);
        });

        if (pendingAction) {
            const pendingTipEl = document.createElement('span');
            pendingTipEl.className = 'metric-op-tip pending';
            pendingTipEl.textContent = `${actionText(pendingAction)}中...`;
            barEl.appendChild(pendingTipEl);
        } else {
            const tip = this.getOperationTip(service);
            if (!tip) {
                return barEl;
            }
            const tipEl = document.createElement('span');
            if (tip.tone === 'error') {
                tipEl.className = 'metric-op-tip error';
            } else if (tip.tone === 'warn') {
                tipEl.className = 'metric-op-tip pending';
            } else {
                tipEl.className = 'metric-op-tip success';
            }
            tipEl.textContent = tip.text;
            barEl.appendChild(tipEl);
        }
        return barEl;
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
    if (statusLevel === 'running' && action === 'start') {
        return '当前实例已运行';
    }
    if (statusLevel === 'stopped' && action === 'stop') {
        return '当前实例已停止';
    }
    if (statusLevel === 'unconfigured') {
        return '未配置运维脚本';
    }
    return '当前状态不支持该动作';
}

/**
 * 判断动作在当前状态下是否可执行。
 *
 * @param {string} statusLevel 实例状态级别
 * @param {string} action 动作
 * @returns {boolean} true 表示可执行
 */
function isActionAllowed(statusLevel, action) {
    if (statusLevel === 'unconfigured') {
        return false;
    }
    if (statusLevel === 'running' && action === 'start') {
        return false;
    }
    if (statusLevel === 'stopped' && action === 'stop') {
        return false;
    }
    return action === 'start' || action === 'restart' || action === 'stop';
}

/**
 * 动作文案映射。
 *
 * @param {string} action 动作
 * @returns {string} 文案
 */
function actionText(action) {
    if (action === 'start') {
        return '启动';
    }
    if (action === 'restart') {
        return '重启';
    }
    if (action === 'stop') {
        return '停止';
    }
    return '执行';
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
 * 创建系统负载展示块。
 *
 * @param {Object} item 指标对象
 * @returns {HTMLDivElement} 节点
 */
function createLoadBlock(item) {
    const loadBlockEl = document.createElement('div');
    loadBlockEl.className = 'metric-load-block';

    const loadLevel = loadLevelClass(item && item.load1m);
    const loadTitleEl = document.createElement('span');
    loadTitleEl.className = 'metric-load-label';
    loadTitleEl.textContent = '系统负载(1/5/15m)';

    const loadValueEl = document.createElement('span');
    loadValueEl.className = loadLevel ? `metric-load-value ${loadLevel}` : 'metric-load-value';
    loadValueEl.textContent = formatLoad(item);

    loadBlockEl.appendChild(loadTitleEl);
    loadBlockEl.appendChild(loadValueEl);
    return loadBlockEl;
}

/**
 * 创建实例状态展示块。
 *
 * @param {Object} item 指标对象
 * @returns {HTMLDivElement} 节点
 */
function createInstanceStatusBlock(item) {
    const statusInfo = resolveInstanceStatus(item && item.instanceStatus);
    const detailRaw = item && item.instanceStatusDetail ? String(item.instanceStatusDetail).trim() : '';

    const blockEl = document.createElement('div');
    blockEl.className = 'metric-instance-block';

    const tagEl = document.createElement('span');
    tagEl.className = `metric-instance-tag ${statusInfo.level}`;
    tagEl.textContent = `实例: ${statusInfo.text}`;
    blockEl.appendChild(tagEl);

    if (detailRaw) {
        const detailEl = document.createElement('span');
        detailEl.className = 'metric-instance-detail';
        detailEl.title = detailRaw;
        detailEl.textContent = detailRaw.length > 120 ? `${detailRaw.substring(0, 120)}...` : detailRaw;
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
    if (status === 'running') {
        return {text: '运行中', level: 'running'};
    }
    if (status === 'stopped') {
        return {text: '已停止', level: 'stopped'};
    }
    if (status === 'unconfigured') {
        return {text: '未配置', level: 'unconfigured'};
    }
    return {text: '未知', level: 'unknown'};
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
