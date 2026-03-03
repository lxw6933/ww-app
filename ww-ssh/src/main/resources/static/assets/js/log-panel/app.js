import {ALL, MAX_LINES, createState} from './state.js';
import {el, value, checked} from './dom.js';
import {
    collectServices,
    isAggregateSelected,
    matchFileKeyword,
    parseLines,
    sortFileOptions,
    fileName
} from './utils.js';
import {LogView} from './log-view.js';
import {FilterChainManager} from './filter-chain.js';

/**
 * 页面全局状态。
 */
const state = createState();

/**
 * 日志视图实例。
 */
const logView = new LogView(state, MAX_LINES);

/**
 * 过滤链管理器实例。
 */
const filterChainManager = new FilterChainManager();

/**
 * 主机指标刷新间隔（毫秒）。
 */
const METRICS_REFRESH_MS = 15000;

window.addEventListener('load', init);

/**
 * 页面初始化。
 */
function init() {
    filterChainManager.init();
    bindEvents();
    updateControls();
    initMetricsPanel();
    logView.setHighlightRules(filterChainManager.getRules());
    logView.setEmptyTip('尚未开始查看日志。请先选择环境和服务，然后点击“开始查看”。');
    updateSettingsSummary();
    logView.appendSystem('操作提示：先选择环境和服务，再点击“开始查看”。');
    loadServers();
}

/**
 * 绑定页面事件。
 */
function bindEvents() {
    filterChainManager.bindAddAction();
    filterChainManager.bindEnterAction(connect);
    el('env').addEventListener('change', loadServices);
    el('service').addEventListener('change', () => {
        loadFiles();
        refreshMetrics();
    });
    el('fileSearch').addEventListener('input', renderFileOptions);
    el('file').addEventListener('change', updateSettingsSummary);
    el('btnRefreshMetrics').addEventListener('click', () => refreshMetrics(true));
    el('btnStart').addEventListener('click', connect);
    el('btnStop').addEventListener('click', stop);
    el('btnPause').addEventListener('click', () => logView.togglePause());
    el('btnBreak').addEventListener('click', () => logView.appendManualBreak());
    el('btnClear').addEventListener('click', () => logView.clearLogs());
    el('btnBottom').addEventListener('click', () => logView.scrollToBottom());
    el('autoScroll').addEventListener('change', () => {
        if (checked('autoScroll')) {
            logView.scrollToBottom();
        }
        logView.updateBottomButton();
    });
    el('showSystem').addEventListener('change', () => logView.refreshSystemVisibility());
    el('filterChain').addEventListener('input', onFilterRuleChanged);
    el('filterChain').addEventListener('change', onFilterRuleChanged);
    el('filterChain').addEventListener('click', () => setTimeout(onFilterRuleChanged, 0));
    el('btnAddFilter').addEventListener('click', () => setTimeout(onFilterRuleChanged, 0));
    el('log').addEventListener('click', event => onLogAreaClick(event));
    el('log').addEventListener('scroll', () => logView.updateBottomButton());
    document.addEventListener('keydown', onShortcut);

    ['lines'].forEach(id => {
        el(id).addEventListener('keydown', event => {
            if (event.key === 'Enter') {
                connect();
            }
        });
    });
}

/**
 * 日志区域点击事件处理。
 *
 * @param {MouseEvent} event 鼠标事件
 */
function onLogAreaClick(event) {
    const target = event.target;
    if (!target || !target.classList || !target.classList.contains('line-copy-btn')) {
        return;
    }
    const row = target.closest('.log-line');
    const rawText = row && row.dataset ? row.dataset.rawText : '';
    logView.copyLineFromButton(target, rawText || '');
}

/**
 * 加载环境列表。
 */
function loadServers() {
    fetch('/api/config/servers')
        .then(response => response.json())
        .then(data => {
            state.config = data || {};
            const envEl = el('env');
            envEl.innerHTML = '';
            Object.keys(state.config).forEach(env => {
                envEl.add(new Option(env, env));
            });
            if (envEl.options.length > 0) {
                envEl.selectedIndex = 0;
            }
            loadServices();
        })
        .catch(() => logView.setStatus('配置加载失败', 'var(--error)'));
}

/**
 * 加载服务列表。
 */
function loadServices() {
    const env = value('env');
    const serviceEl = el('service');
    serviceEl.innerHTML = '';
    collectServices(state.config, env).forEach(service => serviceEl.add(new Option(service, service)));
    serviceEl.add(new Option('全部服务', ALL));
    if (serviceEl.options.length > 0) {
        serviceEl.selectedIndex = 0;
    }
    loadFiles();
    refreshMetrics();
}

/**
 * 加载日志文件列表。
 */
function loadFiles() {
    const env = value('env');
    const service = value('service');
    el('fileSearch').value = '';
    state.fileOptions = [];

    if (!env || !service) {
        renderFileOptions();
        updateFileMode();
        return;
    }
    if (isAggregateSelected(env, service)) {
        renderFileOptions();
        updateFileMode();
        return;
    }

    fetch(`/api/config/files?env=${encodeURIComponent(env)}&service=${encodeURIComponent(service)}`)
        .then(response => response.ok ? response.json() : [])
        .then(list => {
            if (!Array.isArray(list)) {
                state.fileOptions = [];
                renderFileOptions();
                updateFileMode();
                return;
            }
            state.fileOptions = sortFileOptions(list);
            renderFileOptions();
            updateFileMode();
        })
        .catch(() => {
            logView.appendSystem('日志文件列表加载失败');
            state.fileOptions = [];
            renderFileOptions();
            updateFileMode();
        });
}

/**
 * 渲染日志文件下拉框。
 */
function renderFileOptions() {
    const env = value('env');
    const service = value('service');
    const aggregate = isAggregateSelected(env, service);
    const fileEl = el('file');
    const currentValue = fileEl.value;
    const keyword = value('fileSearch').trim().toLowerCase();
    fileEl.innerHTML = '';
    fileEl.add(new Option('使用后端默认', ''));

    if (aggregate) {
        fileEl.add(new Option('聚合模式下使用各服务默认日志', ''));
        fileEl.value = '';
        updateSettingsSummary();
        return;
    }

    const filtered = state.fileOptions.filter(path => matchFileKeyword(path, keyword));
    filtered.forEach(path => fileEl.add(new Option(fileName(path), path)));

    if (currentValue && filtered.indexOf(currentValue) >= 0) {
        fileEl.value = currentValue;
        updateSettingsSummary();
        return;
    }
    if (fileEl.options.length > 1) {
        fileEl.selectedIndex = 1;
    }
    updateSettingsSummary();
}

/**
 * 启动日志监听。
 */
function connect() {
    const env = value('env');
    const service = value('service');
    if (!env || !service) {
        logView.appendSystem('请先选择环境和服务');
        return;
    }

    const filterRules = filterChainManager.getRules();
    const payload = {
        env: env,
        service: service || ALL,
        filePath: value('file'),
        lines: parseLines(value('lines')),
        includeKeyword: '',
        excludeKeyword: '',
        filterRules: filterRules
    };

    logView.setHighlightRules(filterRules);
    logView.setEmptyTip('连接中，等待日志数据...');
    const settingsFoldEl = el('settingsFold');
    if (settingsFoldEl) {
        settingsFoldEl.open = false;
    }
    logView.resetPause();
    closeSocket(true);
    clearReconnectTimer();
    logView.setStatus('连接中...', 'var(--warn)');

    const protocol = location.protocol === 'https:' ? 'wss' : 'ws';
    const ws = new WebSocket(`${protocol}://${location.host}/log-stream`);
    const token = ++state.wsToken;
    state.ws = ws;
    state.manualStop = false;
    updateControls();

    ws.onopen = () => {
        if (token !== state.wsToken || state.ws !== ws) {
            return;
        }
        logView.setStatus('监听中', 'var(--ok)');
        logView.setEmptyTip('监听中，暂未收到新日志...');
        updateControls();
        ws.send(JSON.stringify(payload));
    };

    ws.onmessage = event => {
        if (token !== state.wsToken || state.ws !== ws) {
            return;
        }
        logView.onMessage(event.data);
    };

    ws.onclose = () => {
        if (token !== state.wsToken) {
            return;
        }
        state.ws = null;
        logView.setStatus('已断开', '#999');
        logView.setEmptyTip('连接已断开。可开启自动重连或手动点击“开始查看”。');
        updateControls();
        if (!state.manualStop && checked('autoReconnect')) {
            scheduleReconnect();
        }
    };

    ws.onerror = () => {
        if (token !== state.wsToken || state.ws !== ws) {
            return;
        }
        logView.setStatus('连接异常', 'var(--error)');
        updateControls();
    };
}

/**
 * 停止日志监听。
 */
function stop() {
    clearReconnectTimer();
    closeSocket(true);
    logView.resetPause();
    logView.setEmptyTip('已停止监听。点击“开始查看”可重新连接。');
    logView.setStatus('已停止', '#999');
    updateControls();
}

/**
 * 安排自动重连倒计时。
 */
function scheduleReconnect() {
    clearReconnectTimer();
    let seconds = 3;
    logView.setStatus(`连接断开，${seconds} 秒后重连`, 'var(--warn)');
    state.reconnectTimer = setInterval(() => {
        seconds -= 1;
        if (seconds <= 0) {
            clearReconnectTimer();
            connect();
            return;
        }
        logView.setStatus(`连接断开，${seconds} 秒后重连`, 'var(--warn)');
    }, 1000);
}

/**
 * 清理重连计时器。
 */
function clearReconnectTimer() {
    if (state.reconnectTimer) {
        clearInterval(state.reconnectTimer);
        state.reconnectTimer = null;
    }
}

/**
 * 关闭当前 WebSocket。
 *
 * @param {boolean} manualStop 是否为人工停止
 */
function closeSocket(manualStop) {
    state.manualStop = manualStop;
    const ws = state.ws;
    state.ws = null;
    if (ws && (ws.readyState === WebSocket.OPEN || ws.readyState === WebSocket.CONNECTING)) {
        ws.close();
    }
    updateControls();
}

/**
 * 键盘快捷键处理。
 *
 * @param {KeyboardEvent} event 键盘事件
 */
function onShortcut(event) {
    if (event.ctrlKey && event.key === 'Enter') {
        event.preventDefault();
        connect();
        return;
    }
    if (event.altKey && event.key === 'Enter') {
        event.preventDefault();
        logView.appendManualBreak();
        return;
    }
    if (event.key === 'Escape') {
        event.preventDefault();
        stop();
    }
}

/**
 * 判断 WebSocket 是否处于活跃状态。
 *
 * @returns {boolean} true 表示连接中/已连接
 */
function isSocketActive() {
    return !!(state.ws && (state.ws.readyState === WebSocket.OPEN || state.ws.readyState === WebSocket.CONNECTING));
}

/**
 * 刷新页面可操作状态。
 */
function updateControls() {
    const locked = isSocketActive();
    ['env', 'service', 'lines'].forEach(id => {
        el(id).disabled = locked;
    });
    el('btnStart').disabled = locked;
    el('btnStop').disabled = !locked;
    filterChainManager.setDisabled(locked);
    updateFileMode();
}

/**
 * 刷新文件选择区状态。
 */
function updateFileMode() {
    const env = value('env');
    const service = value('service');
    const aggregate = isAggregateSelected(env, service);
    const locked = isSocketActive();
    const fileSearchEl = el('fileSearch');
    el('file').disabled = locked || aggregate;
    fileSearchEl.disabled = locked || aggregate;
    el('modeTip').textContent = aggregate ? '当前：全部服务查看' : '当前：单服务查看';
    fileSearchEl.placeholder = aggregate ? '全部服务查看时不支持文件名筛选' : '输入文件名关键字进行筛选';
    updateSettingsSummary();
}

/**
 * 过滤规则变化时同步高亮和设置摘要。
 */
function onFilterRuleChanged() {
    const rules = filterChainManager.getRules();
    logView.setHighlightRules(rules);
    updateSettingsSummary();
}

/**
 * 初始化主机指标面板。
 */
function initMetricsPanel() {
    renderMetricEmpty('请选择环境与服务后查看服务器指标');
    startMetricsPolling();
    window.addEventListener('beforeunload', stopMetricsPolling);
}

/**
 * 启动指标自动刷新任务。
 */
function startMetricsPolling() {
    stopMetricsPolling();
    state.metricsTimer = setInterval(() => refreshMetrics(false), METRICS_REFRESH_MS);
}

/**
 * 停止指标自动刷新任务。
 */
function stopMetricsPolling() {
    if (state.metricsTimer) {
        clearInterval(state.metricsTimer);
        state.metricsTimer = null;
    }
}

/**
 * 刷新左侧主机指标。
 *
 * @param {boolean} manual 是否为手工刷新
 */
function refreshMetrics(manual) {
    const env = value('env');
    const service = value('service');
    if (!env || !service) {
        renderMetricEmpty('请选择环境与服务后查看服务器指标');
        return;
    }

    if (manual) {
        renderMetricsSummary({
            statusText: '刷新中',
            online: null,
            total: null,
            errorCount: null,
            avgCpu: null,
            avgMem: null,
            timeText: formatTime(Date.now())
        });
    }

    const token = ++state.metricsToken;
    fetch(`/api/metrics/hosts?env=${encodeURIComponent(env)}&service=${encodeURIComponent(service)}`)
        .then(response => {
            if (!response.ok) {
                throw new Error(`状态码 ${response.status}`);
            }
            return response.json();
        })
        .then(list => {
            if (token !== state.metricsToken) {
                return;
            }
            renderMetrics(Array.isArray(list) ? list : []);
        })
        .catch(error => {
            if (token !== state.metricsToken) {
                return;
            }
            renderMetricError(`指标加载失败：${error.message || '网络异常'}`);
        });
}

/**
 * 渲染指标空态。
 *
 * @param {string} text 提示文本
 */
function renderMetricEmpty(text) {
    renderMetricsSummary({
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
 * 渲染指标错误态。
 *
 * @param {string} text 错误信息
 */
function renderMetricError(text) {
    renderMetricsSummary({
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
 * 渲染主机指标卡片列表。
 *
 * @param {Array<Object>} list 指标列表
 */
function renderMetrics(list) {
    const listEl = el('metricsList');
    listEl.innerHTML = '';
    if (!list || list.length === 0) {
        renderMetricEmpty('未查询到实例指标，请检查服务配置');
        return;
    }

    const total = list.length;
    const okList = list.filter(item => item && item.status === 'ok');
    const errorCount = total - okList.length;
    const avgCpu = average(okList.map(item => item.cpuUsagePercent));
    const avgMem = average(okList.map(item => item.memoryUsagePercent));
    renderMetricsSummary({
        statusText: '正常',
        online: okList.length,
        total: total,
        errorCount: errorCount,
        avgCpu: avgCpu,
        avgMem: avgMem,
        timeText: formatTime(Date.now())
    });

    list.forEach(item => {
        const cardEl = document.createElement('div');
        const ok = item && item.status === 'ok';
        cardEl.className = ok ? 'metric-card' : 'metric-card error';

        const headEl = document.createElement('div');
        headEl.className = 'metric-head';

        const titleEl = document.createElement('p');
        titleEl.className = 'metric-title';
        const serviceText = item && item.service ? item.service : '未知服务';
        const hostText = item && item.host ? item.host : '未知主机';
        const titleText = `${serviceText} @ ${hostText}`;
        titleEl.textContent = titleText;
        titleEl.title = titleText;

        const tagEl = document.createElement('span');
        tagEl.className = ok ? 'metric-tag' : 'metric-tag error';
        tagEl.textContent = ok ? '正常' : '异常';

        headEl.appendChild(titleEl);
        headEl.appendChild(tagEl);
        cardEl.appendChild(headEl);

        const kpiGridEl = document.createElement('div');
        kpiGridEl.className = 'metric-kpi-grid';
        kpiGridEl.appendChild(createUsageVisual(
            'CPU 使用率',
            item && item.cpuUsagePercent,
            formatPercent(item && item.cpuUsagePercent)
        ));
        kpiGridEl.appendChild(createUsageVisual(
            '内存使用率',
            item && item.memoryUsagePercent,
            formatPercent(item && item.memoryUsagePercent),
            formatMemoryRange(item)
        ));
        cardEl.appendChild(kpiGridEl);
        cardEl.appendChild(createLoadBlock(item));

        if (!ok) {
            const messageEl = document.createElement('p');
            messageEl.className = 'metric-message';
            messageEl.textContent = item && item.message ? item.message : '采集失败';
            cardEl.appendChild(messageEl);
        }
        listEl.appendChild(cardEl);
    });
}

/**
 * 创建使用率可视化块（大数字 + 进度条）。
 *
 * @param {string} label 指标名称
 * @param {number} percent 百分比值
 * @param {string} valueText 展示值
 * @param {string} extraText 附加信息
 * @returns {HTMLDivElement} 可视化节点
 */
function createUsageVisual(label, percent, valueText, extraText) {
    const wrapperEl = document.createElement('div');
    wrapperEl.className = 'metric-kpi';

    const headEl = document.createElement('div');
    headEl.className = 'metric-kpi-head';

    const labelEl = document.createElement('span');
    labelEl.className = 'metric-kpi-label';
    labelEl.textContent = label;

    const level = levelClass(percent);
    const valueEl = document.createElement('span');
    valueEl.className = level ? `metric-kpi-value ${level}` : 'metric-kpi-value';
    valueEl.textContent = valueText;

    headEl.appendChild(labelEl);
    headEl.appendChild(valueEl);
    wrapperEl.appendChild(headEl);

    const barEl = document.createElement('div');
    barEl.className = 'metric-progress';
    const fillEl = document.createElement('span');
    fillEl.className = level ? `metric-progress-fill ${level}` : 'metric-progress-fill';
    fillEl.style.width = `${clampPercent(percent)}%`;
    barEl.appendChild(fillEl);
    wrapperEl.appendChild(barEl);

    const metaEl = document.createElement('div');
    metaEl.className = 'metric-kpi-meta';
    if (extraText) {
        const subEl = document.createElement('p');
        subEl.className = 'metric-kpi-sub';
        subEl.textContent = extraText;
        metaEl.appendChild(subEl);
    }
    const levelEl = document.createElement('span');
    levelEl.className = level ? `metric-level-tip ${level}` : 'metric-level-tip';
    levelEl.textContent = riskText(level);
    metaEl.appendChild(levelEl);
    wrapperEl.appendChild(metaEl);
    return wrapperEl;
}

/**
 * 创建负载展示块。
 *
 * @param {Object} item 指标对象
 * @returns {HTMLDivElement} 负载节点
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

    const timeEl = document.createElement('span');
    timeEl.className = 'metric-load-time';
    timeEl.textContent = `采集时间 ${formatTime(item && item.updatedAt ? item.updatedAt : Date.now())}`;

    loadBlockEl.appendChild(loadTitleEl);
    loadBlockEl.appendChild(loadValueEl);
    loadBlockEl.appendChild(timeEl);
    return loadBlockEl;
}

/**
 * 格式化百分比。
 *
 * @param {number} value 数值
 * @returns {string} 格式化文本
 */
function formatPercent(value) {
    if (value === null || value === undefined || Number.isNaN(Number(value))) {
        return '--';
    }
    return `${Number(value).toFixed(1)}%`;
}

/**
 * 格式化内存展示文本。
 *
 * @param {Object} item 指标对象
 * @returns {string} 文本
 */
function formatMemoryRange(item) {
    if (!item) {
        return '内存 --/--';
    }
    const used = formatStorage(item.memoryUsedMb);
    const total = formatStorage(item.memoryTotalMb);
    return `内存 ${used}/${total}`;
}

/**
 * 按容量自动格式化为 MB/GB。
 *
 * @param {number} valueMb 容量（MB）
 * @returns {string} 格式化文本
 */
function formatStorage(valueMb) {
    if (valueMb === null || valueMb === undefined || Number.isNaN(Number(valueMb))) {
        return '--';
    }
    const mb = Number(valueMb);
    if (mb < 1024) {
        return `${Math.round(mb)}MB`;
    }
    const gb = mb / 1024;
    const precision = gb >= 10 ? 0 : 1;
    return `${gb.toFixed(precision)}GB`;
}

/**
 * 格式化负载文本。
 *
 * @param {Object} item 指标对象
 * @returns {string} 负载展示文本
 */
function formatLoad(item) {
    if (!item) {
        return '--';
    }
    const load1m = numberOrDash(item.load1m);
    const load5m = numberOrDash(item.load5m);
    const load15m = numberOrDash(item.load15m);
    return `${load1m}/${load5m}/${load15m}`;
}

/**
 * 计算均值。
 *
 * @param {Array<number>} list 数值列表
 * @returns {number|null} 平均值
 */
function average(list) {
    const values = (list || []).filter(value => value !== null && value !== undefined && !Number.isNaN(Number(value)));
    if (values.length === 0) {
        return null;
    }
    const total = values.reduce((sum, current) => sum + Number(current), 0);
    return total / values.length;
}

/**
 * 将数字格式化为固定小数，空值返回 --。
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
 * 判断百分比指标值级别。
 *
 * @param {number} value 指标数值
 * @returns {string} 级别样式
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
 * 判断负载级别。
 *
 * @param {number} value 1 分钟负载
 * @returns {string} 级别样式
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
 * 百分比收敛到 0~100，用于图形宽度与圆环展示。
 *
 * @param {number} value 原始百分比
 * @returns {number} 收敛后的百分比
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
 * 按指标级别返回风险文案。
 *
 * @param {string} level 风险级别
 * @returns {string} 风险提示
 */
function riskText(level) {
    if (level === 'error') {
        return '风险: 高';
    }
    if (level === 'warn') {
        return '风险: 中';
    }
    return '风险: 低';
}

/**
 * 格式化时间（HH:mm:ss）。
 *
 * @param {number} timestamp 毫秒时间戳
 * @returns {string} 时间字符串
 */
function formatTime(timestamp) {
    const date = new Date(timestamp);
    const hh = String(date.getHours()).padStart(2, '0');
    const mm = String(date.getMinutes()).padStart(2, '0');
    const ss = String(date.getSeconds()).padStart(2, '0');
    return `${hh}:${mm}:${ss}`;
}

/**
 * 渲染左侧指标摘要（结构化卡片）。
 *
 * @param {Object} options 摘要参数
 * @param {string} options.statusText 状态文本
 * @param {number|null} options.online 在线台数
 * @param {number|null} options.total 总台数
 * @param {number|null} options.errorCount 异常台数
 * @param {number|null} options.avgCpu 平均 CPU
 * @param {number|null} options.avgMem 平均内存
 * @param {string} options.timeText 更新时间文本
 */
function renderMetricsSummary(options) {
    const summaryEl = el('metricsSummary');
    if (!summaryEl) {
        return;
    }
    const data = options || {};
    summaryEl.innerHTML = '';
    summaryEl.appendChild(createSummaryItem('状态', data.statusText || '--'));
    summaryEl.appendChild(createSummaryItem(
        '在线实例',
        (data.online === null || data.online === undefined || data.total === null || data.total === undefined)
            ? '--'
            : `${data.online}/${data.total} 台`,
        'strong'
    ));
    summaryEl.appendChild(createSummaryItem(
        '异常实例',
        (data.errorCount === null || data.errorCount === undefined) ? '--' : `${data.errorCount} 台`
    ));
    summaryEl.appendChild(createSummaryItem('平均CPU', formatPercent(data.avgCpu)));
    summaryEl.appendChild(createSummaryItem('平均内存', formatPercent(data.avgMem)));
    summaryEl.appendChild(createSummaryItem('更新时间', data.timeText || '--'));
}

/**
 * 创建摘要项节点。
 *
 * @param {string} label 标签
 * @param {string} value 数值
 * @param {string} valueLevel 数值样式等级
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
 * 更新“更多设置”摘要文案。
 */
function updateSettingsSummary() {
    const summaryEl = el('settingsSummary');
    if (!summaryEl) {
        return;
    }
    const fileEl = el('file');
    const selectedLabel = fileEl && fileEl.options && fileEl.selectedIndex >= 0
        ? fileEl.options[fileEl.selectedIndex].text
        : '默认文件';
    const shortFile = abbreviateText(selectedLabel || '默认文件', 16);
    const filterCount = filterChainManager.getRules().length;
    const mode = isAggregateSelected(value('env'), value('service')) ? '全部服务' : '单服务';
    summaryEl.textContent = `文件:${shortFile} | 过滤:${filterCount}条 | ${mode}`;
}

/**
 * 文本截断，避免摘要过长导致布局撑开。
 *
 * @param {string} text 原文本
 * @param {number} maxLength 最大长度
 * @returns {string} 截断后的文本
 */
function abbreviateText(text, maxLength) {
    const normalized = String(text || '');
    if (normalized.length <= maxLength) {
        return normalized;
    }
    return `${normalized.substring(0, maxLength - 1)}...`;
}
