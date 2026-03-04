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
import {StatusBarController} from './status-bar.js';
import {MetricsPanelController} from './metrics-panel.js';
import {PreferenceStore} from './preferences.js';

/**
 * 页面全局状态。
 */
const state = createState();

/**
 * 日志视图实例。
 */
const logView = new LogView(state, MAX_LINES);

/**
 * 过滤链管理器。
 */
const filterChainManager = new FilterChainManager();

/**
 * 状态栏控制器。
 */
const statusBar = new StatusBarController();

/**
 * 本地偏好存储器。
 */
const preferenceStore = new PreferenceStore();

/**
 * 指标面板控制器。
 */
const metricsPanel = new MetricsPanelController({
    getEnv: () => value('env'),
    getService: () => value('service')
});

/**
 * 日志级别按钮映射。
 */
const LOG_LEVEL_BUTTON_IDS = {
    ALL: 'btnLevelAll',
    INFO: 'btnLevelInfo',
    WARN: 'btnLevelWarn',
    ERROR: 'btnLevelError'
};

/**
 * 分享链接恢复的状态。
 */
const sharedState = parseSharedStateFromUrl();

window.addEventListener('load', init);

/**
 * 页面初始化入口。
 */
function init() {
    filterChainManager.init();
    restoreLocalPreferences();
    bindEvents();
    statusBar.start();
    metricsPanel.init();
    updateControls();
    logView.setHighlightRules(filterChainManager.getRules());
    logView.setEmptyTip('尚未开始查看日志。请先选择环境和服务，然后点击“开始查看”。');
    logView.renderLevelStats();
    updateSettingsSummary();
    logView.appendSystem('操作提示：先选择环境和服务，再点击“开始查看”。');
    loadServers();
    window.addEventListener('beforeunload', beforeUnloadCleanup);
    onFilterRuleChanged();
}

/**
 * 页面卸载前清理资源。
 */
function beforeUnloadCleanup() {
    closeSocket(true);
    clearReconnectTimer();
    statusBar.stop();
    metricsPanel.stopPolling();
}

/**
 * 绑定页面事件。
 */
function bindEvents() {
    filterChainManager.bindAddAction();
    filterChainManager.bindEnterAction(connect);

    el('env').addEventListener('change', () => {
        preferenceStore.set('env', value('env'));
        loadServices();
    });
    el('service').addEventListener('change', () => {
        preferenceStore.set('service', value('service'));
        loadFiles();
        metricsPanel.refresh(false);
    });

    el('fileSearch').addEventListener('input', renderFileOptions);
    el('file').addEventListener('change', () => {
        preferenceStore.set('file', value('file'));
        updateSettingsSummary();
    });
    el('lines').addEventListener('change', () => preferenceStore.set('lines', parseLines(value('lines'))));

    el('autoScroll').addEventListener('change', () => {
        preferenceStore.set('autoScroll', checked('autoScroll'));
        if (checked('autoScroll')) {
            logView.scrollToBottom();
        }
        logView.updateBottomButton();
    });
    el('autoReconnect').addEventListener('change', () => preferenceStore.set('autoReconnect', checked('autoReconnect')));
    el('showSystem').addEventListener('change', () => {
        preferenceStore.set('showSystem', checked('showSystem'));
        logView.refreshSystemVisibility();
    });

    el('btnRefreshMetrics').addEventListener('click', () => metricsPanel.refresh(true));
    el('btnToggleMetrics').addEventListener('click', toggleMetricsPanel);
    el('btnModeQuick').addEventListener('click', () => setUiMode('quick'));
    el('btnModeExpert').addEventListener('click', () => setUiMode('expert'));
    el('btnStart').addEventListener('click', connect);
    el('btnStop').addEventListener('click', stop);
    el('btnPause').addEventListener('click', () => logView.togglePause());
    el('btnBreak').addEventListener('click', () => logView.appendManualBreak());
    el('btnClear').addEventListener('click', () => logView.clearLogs());
    el('btnImmersive').addEventListener('click', toggleImmersiveMode);
    el('btnBottom').addEventListener('click', () => logView.scrollToBottom());
    el('btnShareLink').addEventListener('click', copyShareLink);
    el('btnOpenSettings').addEventListener('click', openSettingsDrawer);
    el('btnCloseSettings').addEventListener('click', closeSettingsDrawer);
    el('settingsDrawerMask').addEventListener('click', closeSettingsDrawer);

    el('filterChain').addEventListener('input', onFilterRuleChanged);
    el('filterChain').addEventListener('change', onFilterRuleChanged);
    el('filterChain').addEventListener('click', () => window.setTimeout(onFilterRuleChanged, 0));
    el('filterChain').addEventListener('chain:changed', onFilterRuleChanged);
    el('btnAddFilter').addEventListener('click', () => window.setTimeout(onFilterRuleChanged, 0));

    el('logSearch').addEventListener('input', () => {
        preferenceStore.set('logSearch', value('logSearch'));
        logView.setSearchKeyword(value('logSearch'));
    });
    el('btnSearchNext').addEventListener('click', () => logView.jumpSearch(false));
    el('btnSearchPrev').addEventListener('click', () => logView.jumpSearch(true));
    el('btnSearchClear').addEventListener('click', clearLogSearch);
    Object.keys(LOG_LEVEL_BUTTON_IDS).forEach(level => {
        const buttonId = LOG_LEVEL_BUTTON_IDS[level];
        el(buttonId).addEventListener('click', () => setLogLevelFilter(level, true));
    });

    el('log').addEventListener('click', onLogAreaClick);
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
 * 清空窗口搜索关键词。
 */
function clearLogSearch() {
    el('logSearch').value = '';
    preferenceStore.set('logSearch', '');
    logView.setSearchKeyword('');
}

/**
 * 判断当前是否为专家模式。
 *
 * @returns {boolean} 是否专家模式
 */
function isExpertMode() {
    return document.body.getAttribute('data-ui-mode') === 'expert';
}

/**
 * 获取当前模式下生效的过滤规则。
 *
 * @returns {Array<{type:string,data:string}>} 规则列表
 */
function getActiveFilterRules() {
    if (!isExpertMode()) {
        return [];
    }
    return filterChainManager.getRules();
}

/**
 * 暂存专家模式过滤规则，避免切换快速模式后丢失。
 */
function stashExpertFilterRules() {
    const rules = filterChainManager.getRules();
    preferenceStore.set('expertFilterRules', JSON.stringify(rules));
}

/**
 * 恢复专家模式过滤规则。
 */
function restoreExpertFilterRules() {
    const raw = preferenceStore.getString('expertFilterRules', '');
    if (!raw) {
        filterChainManager.setRules([]);
        return;
    }
    try {
        const parsed = JSON.parse(raw);
        filterChainManager.setRules(Array.isArray(parsed) ? parsed : []);
    } catch (error) {
        // 忽略异常缓存，保持页面可用
        filterChainManager.setRules([]);
    }
}

/**
 * 复制链接按钮反馈。
 *
 * @param {string} text 按钮文案
 * @param {string} tone 反馈类型
 */
function flashShareButton(text, tone) {
    const button = el('btnShareLink');
    if (!button) {
        return;
    }
    const baseText = button.getAttribute('data-base-text') || button.textContent || '⧉';
    button.setAttribute('data-base-text', baseText);
    if (state.shareFeedbackTimer) {
        window.clearTimeout(state.shareFeedbackTimer);
        state.shareFeedbackTimer = null;
    }
    button.textContent = text;
    button.classList.toggle('success', tone === 'success');
    button.classList.toggle('error', tone === 'error');
    state.shareFeedbackTimer = window.setTimeout(() => {
        button.textContent = baseText;
        button.classList.remove('success');
        button.classList.remove('error');
        state.shareFeedbackTimer = null;
    }, 1400);
}

/**
 * 复制当前视图对应的分享链接。
 */
function copyShareLink() {
    const link = buildShareLink();
    copyText(link)
        .then(() => flashShareButton('✓', 'success'))
        .catch(() => showManualCopyHint(link));
}

/**
 * 复制失败时展示可手动复制的完整链接。
 *
 * @param {string} link 分享链接
 */
function showManualCopyHint(link) {
    const text = String(link || '');
    flashShareButton('!', 'error');
    window.prompt('自动复制失败，请手动复制链接', text);
}

/**
 * 构建分享链接（包含关键筛选参数）。
 *
 * @returns {string} 分享链接
 */
function buildShareLink() {
    const url = new URL(window.location.href);
    const params = url.searchParams;
    params.set('share', '1');
    params.set('env', value('env'));
    params.set('service', value('service'));
    params.set('file', value('file'));
    params.set('lines', String(parseLines(value('lines'))));
    params.set('level', preferenceStore.getString('logLevelFilter', 'ALL'));
    params.set('autoScroll', checked('autoScroll') ? '1' : '0');
    params.set('autoReconnect', checked('autoReconnect') ? '1' : '0');
    params.set('showSystem', checked('showSystem') ? '1' : '0');
    params.set('uiMode', document.body.getAttribute('data-ui-mode') || 'quick');
    params.set('logSearch', value('logSearch'));
    params.set('rules', JSON.stringify(getActiveFilterRules()));
    url.search = params.toString();
    return url.toString();
}

/**
 * 文本复制（优先 Clipboard API）。
 *
 * @param {string} text 文本
 * @returns {Promise<void>} 执行结果
 */
function copyText(text) {
    const content = String(text || '');
    if (navigator.clipboard && window.isSecureContext) {
        return navigator.clipboard.writeText(content);
    }
    return new Promise((resolve, reject) => {
        try {
            const textarea = document.createElement('textarea');
            textarea.value = content;
            textarea.setAttribute('readonly', 'readonly');
            textarea.style.position = 'fixed';
            textarea.style.left = '-9999px';
            document.body.appendChild(textarea);
            textarea.focus();
            textarea.select();
            const success = document.execCommand('copy');
            document.body.removeChild(textarea);
            if (success) {
                resolve();
                return;
            }
            reject(new Error('copy failed'));
        } catch (error) {
            reject(error);
        }
    });
}

/**
 * 日志区域点击事件（复制按钮）。
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
 * 从本地恢复可记忆的偏好项。
 */
function restoreLocalPreferences() {
    el('lines').value = String(getSharedNumber('lines', preferenceStore.getNumber('lines', 200)));
    el('autoScroll').checked = getSharedBoolean('autoScroll', preferenceStore.getBoolean('autoScroll', true));
    el('autoReconnect').checked = getSharedBoolean('autoReconnect', preferenceStore.getBoolean('autoReconnect', true));
    el('showSystem').checked = getSharedBoolean('showSystem', preferenceStore.getBoolean('showSystem', true));
    setSettingsDrawerOpen(preferenceStore.getBoolean('settingsOpen', false), false);
    setImmersiveMode(preferenceStore.getBoolean('immersiveMode', false), false);
    el('logSearch').value = getSharedString('logSearch', preferenceStore.getString('logSearch', ''));
    applyUiModeByState();
    applySavedMetricsCollapse();
    applyFilterRulesByState();
    applyLogLevelByState();
    logView.setSearchKeyword(value('logSearch'));
}

/**
 * 应用保存的界面模式。
 */
function applyUiModeByState() {
    const uiMode = getSharedString('uiMode', preferenceStore.getString('uiMode', 'quick'));
    setUiMode(uiMode === 'expert' ? 'expert' : 'quick');
}

/**
 * 应用保存的指标面板收起状态。
 */
function applySavedMetricsCollapse() {
    const collapsed = preferenceStore.getBoolean('metricsCollapsed', true);
    document.body.classList.toggle('metrics-collapsed', collapsed);
    renderMetricsToggleButton();
}

/**
 * 应用保存的过滤规则。
 */
function applyFilterRulesByState() {
    if (!isExpertMode()) {
        filterChainManager.setRules([]);
        preferenceStore.set('filterRules', '[]');
        return;
    }
    const raw = getSharedString('rules', preferenceStore.getString('filterRules', ''));
    if (!raw) {
        return;
    }
    try {
        const parsed = JSON.parse(raw);
        if (Array.isArray(parsed)) {
            filterChainManager.setRules(parsed);
        }
    } catch (error) {
        // 无效缓存规则不影响页面正常使用
    }
}

/**
 * 应用保存的日志级别过滤条件。
 */
function applyLogLevelByState() {
    const level = getSharedString('level', preferenceStore.getString('logLevelFilter', 'ALL'));
    setLogLevelFilter(level, false);
}

/**
 * 切换指标面板展开/收起。
 */
function toggleMetricsPanel() {
    const collapsed = !document.body.classList.contains('metrics-collapsed');
    document.body.classList.toggle('metrics-collapsed', collapsed);
    preferenceStore.set('metricsCollapsed', collapsed);
    renderMetricsToggleButton();
}

/**
 * 渲染指标面板切换按钮文案。
 */
function renderMetricsToggleButton() {
    const collapsed = document.body.classList.contains('metrics-collapsed');
    const toggleButton = el('btnToggleMetrics');
    toggleButton.textContent = collapsed ? '❯' : '❮';
    toggleButton.title = collapsed ? '展开面板' : '收起面板';
    toggleButton.setAttribute('aria-label', collapsed ? '展开面板' : '收起面板');
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
            Object.keys(state.config).forEach(env => envEl.add(new Option(env, env)));
            applySavedSelectValue(envEl, consumeSharedString('env', preferenceStore.getString('env', '')));
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
    applySavedSelectValue(serviceEl, consumeSharedString('service', preferenceStore.getString('service', '')));
    loadFiles();
    metricsPanel.refresh(false);
}

/**
 * 加载文件列表。
 */
function loadFiles() {
    const env = value('env');
    const service = value('service');
    el('fileSearch').value = '';
    state.fileOptions = [];

    if (!env || !service || isAggregateSelected(env, service)) {
        renderFileOptions();
        updateFileMode();
        return;
    }

    fetch(`/api/config/files?env=${encodeURIComponent(env)}&service=${encodeURIComponent(service)}`)
        .then(response => response.ok ? response.json() : [])
        .then(list => {
            state.fileOptions = Array.isArray(list) ? sortFileOptions(list) : [];
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
 * 渲染文件下拉列表（支持模糊筛选）。
 */
function renderFileOptions() {
    const env = value('env');
    const service = value('service');
    const aggregate = isAggregateSelected(env, service);
    const fileEl = el('file');
    const keyword = value('fileSearch').trim().toLowerCase();
    const savedFile = consumeSharedString('file', preferenceStore.getString('file', ''));
    const currentValue = fileEl.value;

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

    if (savedFile && filtered.indexOf(savedFile) >= 0) {
        fileEl.value = savedFile;
    } else if (currentValue && filtered.indexOf(currentValue) >= 0) {
        fileEl.value = currentValue;
    } else if (fileEl.options.length > 1) {
        fileEl.selectedIndex = 1;
    }
    preferenceStore.set('file', fileEl.value || '');
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

    const filterRules = getActiveFilterRules();
    const payload = {
        env: env,
        service: service || ALL,
        filePath: value('file'),
        lines: parseLines(value('lines')),
        includeKeyword: '',
        excludeKeyword: '',
        filterRules: filterRules
    };

    statusBar.resetLastLogTime();
    statusBar.setReconnectSeconds(null);
    logView.setHighlightRules(filterRules);
    logView.setEmptyTip('连接中，等待日志数据...');
    closeSettingsDrawer();
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
        statusBar.markLogReceived();
        logView.onMessage(event.data);
    };

    ws.onclose = () => {
        if (token !== state.wsToken) {
            return;
        }
        state.ws = null;
        logView.setStatus('已断开', '#999');
        logView.setEmptyTip('连接已断开。可开启自动重连或手动点击“开始查看”。');
        statusBar.setReconnectSeconds(null);
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
    statusBar.setReconnectSeconds(null);
    logView.setStatus('已停止', '#999');
    updateControls();
}

/**
 * 自动重连调度。
 */
function scheduleReconnect() {
    clearReconnectTimer();
    let seconds = 3;
    statusBar.setReconnectSeconds(seconds);
    logView.setStatus(`连接断开，${seconds} 秒后重连`, 'var(--warn)');
    state.reconnectTimer = window.setInterval(() => {
        seconds -= 1;
        statusBar.setReconnectSeconds(seconds > 0 ? seconds : null);
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
        window.clearInterval(state.reconnectTimer);
        state.reconnectTimer = null;
    }
    statusBar.setReconnectSeconds(null);
}

/**
 * 关闭当前 WebSocket。
 *
 * @param {boolean} manualStop 是否人工停止
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
 * 设置页面模式（快速/专家）。
 *
 * @param {string} mode 模式值
 */
function setUiMode(mode) {
    const normalized = mode === 'expert' ? 'expert' : 'quick';
    document.body.setAttribute('data-ui-mode', normalized);
    el('btnModeQuick').classList.toggle('active', normalized === 'quick');
    el('btnModeQuick').classList.toggle('secondary', normalized !== 'quick');
    el('btnModeExpert').classList.toggle('active', normalized === 'expert');
    el('btnModeExpert').classList.toggle('secondary', normalized !== 'expert');
    if (normalized === 'expert') {
        restoreExpertFilterRules();
        logView.setHighlightRules(filterChainManager.getRules());
        setSettingsDrawerOpen(true, true);
    } else {
        stashExpertFilterRules();
        filterChainManager.setRules([]);
        logView.setHighlightRules([]);
        preferenceStore.set('filterRules', '[]');
        setSettingsDrawerOpen(false, true);
    }
    preferenceStore.set('uiMode', normalized);
}

/**
 * 键盘快捷键处理。
 *
 * @param {KeyboardEvent} event 键盘事件
 */
function onShortcut(event) {
    if (event.key === 'F9') {
        event.preventDefault();
        toggleImmersiveMode();
        return;
    }
    if (event.key === 'Escape' && isSettingsDrawerOpen()) {
        event.preventDefault();
        closeSettingsDrawer();
        return;
    }
    if (shouldIgnoreGlobalShortcut(event)) {
        return;
    }
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
 * 输入态下忽略全局快捷键，避免误触发连接控制动作。
 *
 * @param {KeyboardEvent} event 键盘事件
 * @returns {boolean} 是否忽略
 */
function shouldIgnoreGlobalShortcut(event) {
    const target = event && event.target;
    if (!target || typeof target !== 'object') {
        return false;
    }
    if (target.isContentEditable) {
        return true;
    }
    const tagName = String(target.tagName || '').toLowerCase();
    return tagName === 'input' || tagName === 'textarea' || tagName === 'select';
}

/**
 * 切换沉浸模式，最大化日志窗口可视区域。
 */
function toggleImmersiveMode() {
    setImmersiveMode(!document.body.classList.contains('immersive-mode'), true);
}

/**
 * 设置沉浸模式状态。
 *
 * @param {boolean} enabled 是否启用
 * @param {boolean} persist 是否持久化
 */
function setImmersiveMode(enabled, persist) {
    const active = !!enabled;
    document.body.classList.toggle('immersive-mode', active);
    const button = el('btnImmersive');
    if (button) {
        button.classList.toggle('active', active);
        button.textContent = active ? '⤡' : '⛶';
        button.title = active ? '退出沉浸模式' : '进入沉浸模式';
    }
    if (active) {
        closeSettingsDrawer();
    }
    if (persist) {
        preferenceStore.set('immersiveMode', active);
    }
}

/**
 * 打开设置抽屉。
 */
function openSettingsDrawer() {
    setSettingsDrawerOpen(true, true);
}

/**
 * 关闭设置抽屉。
 */
function closeSettingsDrawer() {
    setSettingsDrawerOpen(false, true);
}

/**
 * 判断设置抽屉是否打开。
 *
 * @returns {boolean} 是否打开
 */
function isSettingsDrawerOpen() {
    return document.body.classList.contains('settings-drawer-open');
}

/**
 * 设置抽屉开关状态。
 *
 * @param {boolean} open 是否打开
 * @param {boolean} persist 是否持久化
 */
function setSettingsDrawerOpen(open, persist) {
    const visible = !!open;
    document.body.classList.toggle('settings-drawer-open', visible);
    const drawer = el('settingsDrawer');
    if (drawer) {
        drawer.setAttribute('aria-hidden', visible ? 'false' : 'true');
    }
    if (persist) {
        preferenceStore.set('settingsOpen', visible);
    }
}

/**
 * 判断连接是否活跃。
 *
 * @returns {boolean} 是否活跃
 */
function isSocketActive() {
    return !!(state.ws && (state.ws.readyState === WebSocket.OPEN || state.ws.readyState === WebSocket.CONNECTING));
}

/**
 * 刷新控件可用状态。
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
 * 刷新文件区域模式与文案。
 */
function updateFileMode() {
    const env = value('env');
    const service = value('service');
    const aggregate = isAggregateSelected(env, service);
    const locked = isSocketActive();
    const fileSearchEl = el('fileSearch');
    el('file').disabled = locked || aggregate;
    fileSearchEl.disabled = locked || aggregate;
    statusBar.setModeTip(aggregate);
    fileSearchEl.placeholder = aggregate ? '全部服务查看时不支持文件名筛选' : '输入文件名关键字进行筛选';
    updateSettingsSummary();
}

/**
 * 设置日志级别过滤并刷新按钮样式。
 *
 * @param {string} level 级别值
 * @param {boolean} save 是否持久化
 */
function setLogLevelFilter(level, save) {
    const normalized = normalizeLogLevel(level);
    logView.setLevelFilter(normalized);
    Object.keys(LOG_LEVEL_BUTTON_IDS).forEach(key => {
        const button = el(LOG_LEVEL_BUTTON_IDS[key]);
        button.classList.toggle('active', key === normalized);
    });
    if (save) {
        preferenceStore.set('logLevelFilter', normalized);
    }
}

/**
 * 过滤规则变更处理。
 */
function onFilterRuleChanged() {
    const rules = filterChainManager.getRules();
    logView.setHighlightRules(rules);
    preferenceStore.set('filterRules', JSON.stringify(rules));
    updateSettingsSummary();
    renderActiveFilterExpression();
}

/**
 * 渲染实时日志窗口下方的过滤表达式摘要。
 * <p>
 * 仅在存在有效过滤规则时展示；快速模式下始终隐藏。
 * </p>
 */
function renderActiveFilterExpression() {
    const expressionEl = el('activeFilterExpr');
    if (!expressionEl) {
        return;
    }
    const rules = getActiveFilterRules();
    if (!rules.length) {
        expressionEl.classList.add('hidden');
        expressionEl.textContent = '';
        return;
    }
    expressionEl.textContent = `过滤表达式：${buildFilterExpressionText(rules)}`;
    expressionEl.classList.remove('hidden');
}

/**
 * 将过滤规则列表拼装为可读表达式文本。
 *
 * @param {Array<{type:string,data:string}>} rules 规则列表
 * @returns {string} 过滤表达式
 */
function buildFilterExpressionText(rules) {
    return rules
        .map(rule => {
            const type = rule && rule.type ? String(rule.type).trim().toLowerCase() : '';
            const data = rule && rule.data ? String(rule.data).trim() : '';
            if (!data) {
                return '';
            }
            if (type === 'exclude') {
                return `NOT (${data})`;
            }
            return `(${data})`;
        })
        .filter(Boolean)
        .join(' AND ');
}

/**
 * 规范化日志级别值。
 *
 * @param {string} level 原级别
 * @returns {string} 规范化级别
 */
function normalizeLogLevel(level) {
    const normalized = String(level || '').toUpperCase();
    if (normalized === 'INFO' || normalized === 'WARN' || normalized === 'ERROR') {
        return normalized;
    }
    return 'ALL';
}

/**
 * 从分享状态中读取字符串，若不存在则使用回退值。
 *
 * @param {string} key 参数名
 * @param {string} fallback 回退值
 * @returns {string} 字符串
 */
function getSharedString(key, fallback) {
    if (!sharedState || sharedState[key] === null || sharedState[key] === undefined) {
        return fallback;
    }
    const value = String(sharedState[key]);
    if (!value.trim()) {
        return fallback;
    }
    return value;
}

/**
 * 从分享状态中读取布尔值。
 *
 * @param {string} key 参数名
 * @param {boolean} fallback 回退值
 * @returns {boolean} 布尔值
 */
function getSharedBoolean(key, fallback) {
    if (!sharedState || sharedState[key] === null || sharedState[key] === undefined) {
        return fallback;
    }
    const raw = String(sharedState[key]).trim().toLowerCase();
    if (raw === '1' || raw === 'true') {
        return true;
    }
    if (raw === '0' || raw === 'false') {
        return false;
    }
    return fallback;
}

/**
 * 从分享状态中读取数字。
 *
 * @param {string} key 参数名
 * @param {number} fallback 回退值
 * @returns {number} 数值
 */
function getSharedNumber(key, fallback) {
    if (!sharedState || sharedState[key] === null || sharedState[key] === undefined) {
        return fallback;
    }
    const raw = String(sharedState[key]).trim();
    if (!raw) {
        return fallback;
    }
    const number = Number(raw);
    if (Number.isNaN(number)) {
        return fallback;
    }
    return number;
}

/**
 * 获取并消费一次性分享参数。
 *
 * @param {string} key 参数名
 * @param {string} fallback 回退值
 * @returns {string} 参数值
 */
function consumeSharedString(key, fallback) {
    if (!sharedState || sharedState[key] === null || sharedState[key] === undefined) {
        return fallback;
    }
    const value = String(sharedState[key]);
    delete sharedState[key];
    if (!value.trim()) {
        return fallback;
    }
    return value;
}

/**
 * 解析 URL 中的分享状态参数。
 *
 * @returns {Object|null} 分享状态
 */
function parseSharedStateFromUrl() {
    const params = new URLSearchParams(window.location.search || '');
    if (params.get('share') !== '1') {
        return null;
    }
    return {
        env: params.get('env') || '',
        service: params.get('service') || '',
        file: params.get('file') || '',
        lines: params.get('lines') || '',
        level: params.get('level') || '',
        autoScroll: params.get('autoScroll') || '',
        autoReconnect: params.get('autoReconnect') || '',
        showSystem: params.get('showSystem') || '',
        uiMode: params.get('uiMode') || '',
        logSearch: params.get('logSearch') || '',
        rules: params.get('rules') || ''
    };
}

/**
 * 更新“更多设置”摘要。
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
    const mode = isAggregateSelected(value('env'), value('service')) ? '全部服务' : '单服务';
    summaryEl.textContent = `文件:${shortFile} | ${mode}`;
}

/**
 * 下拉框优先应用缓存值，否则选中第一个。
 *
 * @param {HTMLSelectElement} selectEl 下拉框
 * @param {string} savedValue 缓存值
 */
function applySavedSelectValue(selectEl, savedValue) {
    if (!selectEl || !selectEl.options || selectEl.options.length === 0) {
        return;
    }
    if (savedValue) {
        const hit = Array.from(selectEl.options).some(option => option.value === savedValue);
        if (hit) {
            selectEl.value = savedValue;
            return;
        }
    }
    selectEl.selectedIndex = 0;
}

/**
 * 长文本摘要截断。
 *
 * @param {string} text 原始文本
 * @param {number} maxLength 最大长度
 * @returns {string} 截断文本
 */
function abbreviateText(text, maxLength) {
    const normalized = String(text || '');
    if (normalized.length <= maxLength) {
        return normalized;
    }
    return `${normalized.substring(0, maxLength - 1)}...`;
}
