import {ALL, MAX_LINES, createState} from './state.js';
import {el, value, checked} from './dom.js';
import {
    collectProjects,
    collectEnvs,
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
    getProject: () => value('project'),
    getEnv: () => value('env'),
    getService: () => value('service'),
    operateInstance: (instanceService, action) => operateInstanceLifecycle(instanceService, action)
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
 * 读取模式：tail（实时监听）。
 */
const READ_MODE_TAIL = 'tail';

/**
 * 读取模式：cat（一次性快照）。
 */
const READ_MODE_CAT = 'cat';

/**
 * 界面密度：舒适。
 */
const UI_DENSITY_COMFORTABLE = 'comfortable';

/**
 * cat 降级 WebSocket：空闲判定超时（毫秒）。
 * <p>
 * 过短会导致“首条日志尚未到达就提前结束”，从而误判未命中。
 * </p>
 */
const CAT_WS_IDLE_TIMEOUT_MS = 1200;

/**
 * cat 降级 WebSocket：硬超时（毫秒）。
 */
const CAT_WS_HARD_TIMEOUT_MS = 6000;

/**
 * URL 分享参数中的文件键名。
 */
const SHARED_FILE_KEY = 'file';

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
    logView.setEmptyTip('尚未开始查看日志。请先选择项目、环境和服务，然后点击“开始查看”。');
    logView.renderLevelStats();
    updateSettingsSummary();
    logView.appendSystem('操作提示：先选择项目、环境和服务，再点击“开始查看”。');
    loadServers();
    window.addEventListener('beforeunload', beforeUnloadCleanup);
    onFilterRuleChanged();
}

/**
 * 页面卸载前清理资源。
 */
function beforeUnloadCleanup() {
    clearTailRefreshTimer();
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

    el('project').addEventListener('change', () => {
        preferenceStore.set('project', value('project'));
        loadEnvs();
    });
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
        scheduleTailSubscriptionRefresh();
    });
    el('lines').addEventListener('change', () => {
        preferenceStore.set('lines', parseLines(value('lines')));
        scheduleTailSubscriptionRefresh();
    });

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

    el('btnToggleMetrics').addEventListener('click', toggleMetricsPanel);
    el('btnModeQuick').addEventListener('click', () => setUiMode('quick'));
    el('btnModeExpert').addEventListener('click', () => setUiMode('expert'));
    el('btnReadTail').addEventListener('click', () => setReadMode(READ_MODE_TAIL, true));
    el('btnReadCat').addEventListener('click', () => setReadMode(READ_MODE_CAT, true));
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
    const baseText = button.getAttribute('data-base-text') || button.textContent || '⎘';
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
    params.set('project', value('project'));
    params.set('env', value('env'));
    params.set('service', value('service'));
    params.set('file', value('file'));
    params.set('lines', String(parseLines(value('lines'))));
    params.set('level', preferenceStore.getString('logLevelFilter', 'ALL'));
    params.set('autoScroll', checked('autoScroll') ? '1' : '0');
    params.set('autoReconnect', checked('autoReconnect') ? '1' : '0');
    params.set('showSystem', checked('showSystem') ? '1' : '0');
    params.set('uiMode', document.body.getAttribute('data-ui-mode') || 'quick');
    params.set('uiDensity', getUiDensity());
    params.set('readMode', getReadMode());
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
 * 执行实例启停运维动作。
 *
 * @param {string} instanceService 实例服务键
 * @param {string} action 操作动作（start/restart/stop）
 * @returns {Promise<Object>} 操作结果
 */
function operateInstanceLifecycle(instanceService, action) {
    const project = value('project');
    const env = value('env');
    const service = String(instanceService || '').trim();
    const normalizedAction = String(action || '').trim().toLowerCase();
    if (!project || !env || !service || !normalizedAction) {
        return Promise.reject(new Error('实例运维参数不完整'));
    }
    return fetch('/api/instance/operate', {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify({
            project: project,
            env: env,
            service: service,
            action: normalizedAction
        })
    })
        .then(async response => {
            let data = null;
            try {
                data = await response.json();
            } catch (error) {
                // 忽略 JSON 解析异常，后续走统一错误提示
            }
            if (!response.ok) {
                const message = data && data.message ? data.message : `请求失败(${response.status})`;
                if (data && data.output) {
                    appendInstanceOutputToLog(service, normalizedAction, data.output);
                }
                throw new Error(message);
            }
            if (!data || !data.success) {
                if (data && data.output) {
                    appendInstanceOutputToLog(service, normalizedAction, data.output);
                }
                throw new Error((data && data.message) ? data.message : '实例操作失败');
            }
            logView.appendSystem(`实例操作完成：${service} -> ${normalizedAction}`);
            appendInstanceOutputToLog(service, normalizedAction, data.output);
            return data;
        });
}

/**
 * 将实例操作输出回显到日志窗口，便于快速定位执行问题。
 *
 * @param {string} service 实例服务键
 * @param {string} action 操作动作
 * @param {string} output 输出文本
 */
function appendInstanceOutputToLog(service, action, output) {
    const text = String(output || '').trim();
    if (!text) {
        return;
    }
    const rows = text.split(/\r?\n/);
    const keep = rows.slice(0, 20);
    const suffix = rows.length > keep.length ? `\n...(共 ${rows.length} 行，已截断)` : '';
    logView.appendSystem(`[实例输出] ${service} ${action}\n${keep.join('\n')}${suffix}`);
}

/**
 * 从本地恢复可记忆的偏好项。
 */
function restoreLocalPreferences() {
    el('lines').value = String(getSharedNumber('lines', preferenceStore.getNumber('lines', 200)));
    el('autoScroll').checked = getSharedBoolean('autoScroll', preferenceStore.getBoolean('autoScroll', true));
    el('autoReconnect').checked = getSharedBoolean('autoReconnect', preferenceStore.getBoolean('autoReconnect', true));
    el('showSystem').checked = getSharedBoolean('showSystem', preferenceStore.getBoolean('showSystem', true));
    setSettingsDrawerOpen(false, false);
    setImmersiveMode(preferenceStore.getBoolean('immersiveMode', false), false);
    el('logSearch').value = getSharedString('logSearch', preferenceStore.getString('logSearch', ''));
    applyReadModeByState();
    applyUiModeByState();
    applyDefaultUiDensity();
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
 * 应用默认界面密度（舒适）。
 */
function applyDefaultUiDensity() {
    setUiDensity(UI_DENSITY_COMFORTABLE, false);
}

/**
 * 应用保存的读取模式（tail/cat）。
 */
function applyReadModeByState() {
    const readMode = getSharedString('readMode', preferenceStore.getString('readMode', READ_MODE_TAIL));
    setReadMode(readMode, false);
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
    toggleButton.textContent = collapsed ? '▸' : '◂';
    toggleButton.title = collapsed ? '展开面板' : '收起面板';
    toggleButton.setAttribute('aria-label', collapsed ? '展开面板' : '收起面板');
}

/**
 * 加载项目/环境/服务配置。
 */
function loadServers() {
    fetch('/api/config/servers')
        .then(response => response.json())
        .then(data => {
            state.config = data || {};
            const projectEl = el('project');
            projectEl.innerHTML = '';
            collectProjects(state.config).forEach(project => projectEl.add(new Option(project, project)));
            applySavedSelectValue(projectEl, consumeSharedString('project', preferenceStore.getString('project', '')));
            loadEnvs();
        })
        .catch(() => logView.setStatus('配置加载失败', 'var(--error)'));
}

/**
 * 加载环境列表。
 */
function loadEnvs() {
    const project = value('project');
    const envEl = el('env');
    envEl.innerHTML = '';
    collectEnvs(state.config, project).forEach(env => envEl.add(new Option(env, env)));
    applySavedSelectValue(envEl, consumeSharedString('env', preferenceStore.getString('env', '')));
    loadServices();
}

/**
 * 加载服务列表。
 */
function loadServices() {
    const project = value('project');
    const env = value('env');
    const serviceEl = el('service');
    serviceEl.innerHTML = '';
    collectServices(state.config, project, env).forEach(service => serviceEl.add(new Option(service, service)));
    serviceEl.add(new Option('全部服务', ALL));
    applySavedSelectValue(serviceEl, consumeSharedString('service', preferenceStore.getString('service', '')));
    loadFiles();
    metricsPanel.refresh(false);
}

/**
 * 加载文件列表。
 */
function loadFiles() {
    const project = value('project');
    const env = value('env');
    const service = value('service');
    el('fileSearch').value = '';
    state.fileOptions = [];

    if (!project || !env || !service || isAggregateSelected(project, env, service)) {
        renderFileOptions();
        updateFileMode();
        return;
    }

    fetch(`/api/config/files?project=${encodeURIComponent(project)}&env=${encodeURIComponent(env)}&service=${encodeURIComponent(service)}`)
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
    const project = value('project');
    const env = value('env');
    const service = value('service');
    const aggregate = isAggregateSelected(project, env, service);
    const fileEl = el('file');
    const keyword = value('fileSearch').trim().toLowerCase();
    const sharedFile = consumeSharedString(SHARED_FILE_KEY, '');
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

    if (sharedFile && filtered.indexOf(sharedFile) >= 0) {
        fileEl.value = sharedFile;
    } else if (currentValue && filtered.indexOf(currentValue) >= 0) {
        fileEl.value = currentValue;
    } else {
        // 默认选择“使用后端默认”，由后端每次连接时解析最新日志文件。
        fileEl.selectedIndex = 0;
    }
    preferenceStore.set('file', fileEl.value || '');
    updateSettingsSummary();
}

/**
 * 启动日志监听。
 */
function connect() {
    const project = value('project');
    const env = value('env');
    const service = value('service');
    if (!project || !env || !service) {
        logView.appendSystem('请先选择项目、环境和服务');
        return;
    }
    if (isCatMode()) {
        requestCatSnapshot();
        return;
    }
    connectTail();
}

/**
 * 构建日志读取请求体（WebSocket 与 cat 接口共用）。
 *
 * @returns {{project:string,env:string,service:string,filePath:string,lines:number,includeKeyword:string,excludeKeyword:string,filterRules:Array}} 请求体
 */
function buildLogRequestPayload() {
    return {
        project: value('project'),
        env: value('env'),
        service: value('service') || ALL,
        filePath: value('file'),
        lines: parseLines(value('lines')),
        includeKeyword: '',
        excludeKeyword: '',
        filterRules: getActiveFilterRules()
    };
}

/**
 * 启动 tail 实时监听。
 */
function connectTail() {
    const payload = buildLogRequestPayload();
    statusBar.resetLastLogTime();
    statusBar.setReconnectSeconds(null);
    logView.setHighlightRules(payload.filterRules || []);
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
        if (!state.manualStop && checked('autoReconnect') && isTailMode()) {
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
 * 通过 cat 模式读取一次性日志快照。
 */
function requestCatSnapshot() {
    if (state.catLoading) {
        return;
    }
    const payload = buildLogRequestPayload();
    state.catLoading = true;
    clearReconnectTimer();
    closeSocket(true);
    closeSettingsDrawer();
    logView.resetPause();
    logView.clearLogs();
    logView.setEmptyTip('快照读取中，请稍候...');
    logView.setStatus('快照读取中...', 'var(--warn)');
    updateControls();

    return fetch('/api/log/cat', {
        method: 'POST',
        headers: {'Content-Type': 'application/json'},
        body: JSON.stringify(payload)
    })
        .then(async response => {
            if (!response.ok) {
                const text = await response.text();
                const error = new Error(text || `状态码 ${response.status}`);
                error.status = response.status;
                throw error;
            }
            return response.json();
        })
        .then(payload => {
            const rows = resolveCatSnapshotRows(payload);
            if (rows.length) {
                logView.appendLines(`${rows.join('\n')}\n`);
                logView.setStatus('快照完成', 'var(--ok)');
                logView.setEmptyTip('快照读取完成，可调整条件后再次读取。');
                return;
            }
            logView.setStatus('快照完成', 'var(--ok)');
            logView.setEmptyTip(buildCatNoHitTip());
        })
        .catch(error => {
            if (error && (error.status === 404 || error.status === 405)) {
                return requestCatSnapshotViaWebSocket(payload);
            }
            logView.setStatus('快照失败', 'var(--error)');
            logView.appendSystem(`快照读取失败: ${error.message || '网络异常'}`);
        })
        .finally(() => {
            state.catLoading = false;
            updateControls();
        });
}

/**
 * 当后端未提供 cat 接口时，降级为短时 tail 快照读取。
 *
 * @param {{project:string,env:string,service:string,filePath:string,lines:number,includeKeyword:string,excludeKeyword:string,filterRules:Array}} payload 请求体
 * @returns {Promise<void>} 执行结果
 */
function requestCatSnapshotViaWebSocket(payload) {
    return new Promise(resolve => {
        logView.appendSystem('后端未提供 cat 接口，已降级为 tail 快照');
        const protocol = location.protocol === 'https:' ? 'wss' : 'ws';
        const ws = new WebSocket(`${protocol}://${location.host}/log-stream`);
        let receivedLogLine = false;
        let closed = false;
        let idleTimer = null;
        let hardTimer = null;

        const finish = () => {
            if (closed) {
                return;
            }
            closed = true;
            if (idleTimer) {
                window.clearTimeout(idleTimer);
                idleTimer = null;
            }
            if (hardTimer) {
                window.clearTimeout(hardTimer);
                hardTimer = null;
            }
            if (ws.readyState === WebSocket.OPEN || ws.readyState === WebSocket.CONNECTING) {
                ws.close();
            }
            logView.setStatus('快照完成', 'var(--ok)');
            if (!receivedLogLine) {
                logView.setEmptyTip(buildCatNoHitTip());
            } else {
                logView.setEmptyTip('快照读取完成，可调整条件后再次读取。');
            }
            resolve();
        };

        const refreshIdleTimer = () => {
            if (idleTimer) {
                window.clearTimeout(idleTimer);
            }
            idleTimer = window.setTimeout(() => finish(), CAT_WS_IDLE_TIMEOUT_MS);
        };

        ws.onopen = () => {
            try {
                ws.send(JSON.stringify(payload));
            } catch (error) {
                logView.setStatus('快照失败', 'var(--error)');
                logView.appendSystem('快照请求发送失败');
                finish();
                return;
            }
            hardTimer = window.setTimeout(() => finish(), CAT_WS_HARD_TIMEOUT_MS);
            refreshIdleTimer();
        };

        ws.onmessage = event => {
            if (hasBusinessLogLine(event.data)) {
                receivedLogLine = true;
            }
            statusBar.markLogReceived();
            logView.onMessage(event.data);
            refreshIdleTimer();
        };

        ws.onerror = () => {
            logView.setStatus('快照失败', 'var(--error)');
            finish();
        };

        ws.onclose = () => {
            finish();
        };
    });
}

/**
 * 判断 WebSocket 文本块中是否存在真实业务日志行（排除系统提示）。
 *
 * @param {string} chunk 文本块
 * @returns {boolean} true 表示存在业务日志
 */
function hasBusinessLogLine(chunk) {
    return String(chunk || '')
        .split(/\r?\n/)
        .some(line => {
            const text = String(line || '').trim();
            return !!text && text.indexOf('[系统提示]') !== 0;
        });
}

/**
 * 解析 cat 接口返回，兼容数组与常见对象包装结构。
 * <p>
 * 兼容场景：
 * 1. 直接返回数组：{@code ["line1","line2"]}；<br>
 * 2. 包装返回：{@code {data:[...]}} / {@code {rows:[...]}} 等。
 * </p>
 *
 * @param {*} payload 接口返回体
 * @returns {string[]} 日志行列表
 */
function resolveCatSnapshotRows(payload) {
    const businessError = detectCatSnapshotBusinessError(payload);
    if (businessError) {
        throw new Error(businessError);
    }
    const directRows = normalizeSnapshotRowCollection(payload);
    if (directRows.length || Array.isArray(payload)) {
        return directRows;
    }
    if (!payload || typeof payload !== 'object') {
        return [];
    }
    const firstLevelKeys = ['data', 'rows', 'result', 'records', 'list', 'content'];
    for (const key of firstLevelKeys) {
        const rows = normalizeSnapshotRowCollection(payload[key]);
        if (rows.length) {
            return rows;
        }
    }
    const data = payload.data;
    if (data && typeof data === 'object' && !Array.isArray(data)) {
        const nestedKeys = ['rows', 'list', 'records', 'content', 'result', 'data'];
        for (const key of nestedKeys) {
            const rows = normalizeSnapshotRowCollection(data[key]);
            if (rows.length) {
                return rows;
            }
        }
    }
    return [];
}

/**
 * 识别 cat 业务返回中的失败语义，避免把失败对象误判为空结果。
 *
 * @param {*} payload 接口返回体
 * @returns {string} 失败消息（空串表示无业务失败）
 */
function detectCatSnapshotBusinessError(payload) {
    if (!payload || typeof payload !== 'object' || Array.isArray(payload)) {
        return '';
    }
    if (payload.success === false) {
        return payload.message ? String(payload.message) : '快照读取失败';
    }
    if (payload.code === undefined || payload.code === null) {
        return '';
    }
    const normalized = String(payload.code).trim();
    if (!normalized || normalized === '0' || normalized === '200') {
        return '';
    }
    return payload.message
        ? String(payload.message)
        : `快照读取失败（code=${normalized}）`;
}

/**
 * 将数组/文本等候选值规范化为日志行数组。
 *
 * @param {*} value 候选值
 * @returns {string[]} 日志行数组
 */
function normalizeSnapshotRowCollection(value) {
    if (Array.isArray(value)) {
        return value
            .map(item => normalizeSnapshotRow(item))
            .filter(item => item !== null);
    }
    if (typeof value === 'string') {
        return String(value)
            .split(/\r?\n/)
            .map(item => item.trim())
            .filter(Boolean);
    }
    return [];
}

/**
 * 将单条返回项规范化为字符串日志行。
 *
 * @param {*} value 单条返回项
 * @returns {string|null} 规范化结果
 */
function normalizeSnapshotRow(value) {
    if (value === null || value === undefined) {
        return null;
    }
    if (typeof value === 'string') {
        return value;
    }
    if (typeof value === 'number' || typeof value === 'boolean') {
        return String(value);
    }
    if (typeof value === 'object') {
        const textFields = ['line', 'content', 'message', 'msg', 'log'];
        for (const key of textFields) {
            if (typeof value[key] === 'string' && value[key].trim()) {
                return value[key];
            }
        }
        try {
            return JSON.stringify(value);
        } catch (error) {
            return String(value);
        }
    }
    return String(value);
}

/**
 * 构建 cat 模式未命中提示文案。
 *
 * @returns {string} 提示文案
 */
function buildCatNoHitTip() {
    const rules = getActiveFilterRules();
    if (rules && rules.length) {
        return '按当前过滤条件未命中日志，可调整条件后再次读取。';
    }
    return '当前快照窗口未读取到日志，可增大行数或稍后重试。';
}

/**
 * 停止日志监听。
 */
function stop() {
    clearTailRefreshTimer();
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
 * 清理 tail 条件热更新定时器。
 */
function clearTailRefreshTimer() {
    if (state.tailRefreshTimer) {
        window.clearTimeout(state.tailRefreshTimer);
        state.tailRefreshTimer = null;
    }
}

/**
 * 在 tail 监听中延迟刷新订阅条件。
 * <p>
 * 用于“修改过滤/文件/行数后无需停止，自动生效”。
 * </p>
 */
function scheduleTailSubscriptionRefresh() {
    if (!isTailMode() || !isSocketActive()) {
        return;
    }
    if (state.tailRefreshTimer) {
        window.clearTimeout(state.tailRefreshTimer);
    }
    state.tailRefreshTimer = window.setTimeout(() => {
        state.tailRefreshTimer = null;
        refreshTailSubscriptionNow();
    }, 220);
}

/**
 * 立即将当前条件重发给后端，触发 tail 订阅重建。
 */
function refreshTailSubscriptionNow() {
    if (!isTailMode() || !isSocketActive() || !state.ws || state.ws.readyState !== WebSocket.OPEN) {
        return;
    }
    const payload = buildLogRequestPayload();
    statusBar.resetLastLogTime();
    statusBar.setReconnectSeconds(null);
    logView.setHighlightRules(payload.filterRules || []);
    logView.setStatus('监听中（条件已更新）', 'var(--ok)');
    try {
        state.ws.send(JSON.stringify(payload));
    } catch (error) {
        logView.appendSystem('条件更新失败，请手动重连');
    }
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
 * 获取当前界面密度。
 *
 * @returns {string} 密度值（compact/comfortable）
 */
function getUiDensity() {
    return document.body.getAttribute('data-density') || UI_DENSITY_COMFORTABLE;
}

/**
 * 设置界面密度（当前仅保留舒适模式）。
 *
 * @param {string} mode 密度值
 * @param {boolean} persist 是否持久化
 */
function setUiDensity(mode, persist) {
    const normalized = mode === UI_DENSITY_COMFORTABLE ? UI_DENSITY_COMFORTABLE : UI_DENSITY_COMFORTABLE;
    document.body.setAttribute('data-density', normalized);
    if (persist) {
        preferenceStore.set('uiDensity', normalized);
    }
}

/**
 * 获取当前读取模式。
 *
 * @returns {string} 模式值（tail/cat）
 */
function getReadMode() {
    const mode = document.body.getAttribute('data-read-mode');
    return mode === READ_MODE_CAT ? READ_MODE_CAT : READ_MODE_TAIL;
}

/**
 * 判断当前是否为 tail 模式。
 *
 * @returns {boolean} true 表示 tail
 */
function isTailMode() {
    return getReadMode() === READ_MODE_TAIL;
}

/**
 * 判断当前是否为 cat 模式。
 *
 * @returns {boolean} true 表示 cat
 */
function isCatMode() {
    return getReadMode() === READ_MODE_CAT;
}

/**
 * 设置读取模式并更新界面状态。
 *
 * @param {string} mode 目标模式
 * @param {boolean} persist 是否持久化
 */
function setReadMode(mode, persist) {
    const normalized = mode === READ_MODE_CAT ? READ_MODE_CAT : READ_MODE_TAIL;
    document.body.setAttribute('data-read-mode', normalized);
    renderReadModeButtons();
    if (normalized === READ_MODE_CAT) {
        clearReconnectTimer();
        if (isSocketActive()) {
            stop();
        }
    }
    if (persist) {
        preferenceStore.set('readMode', normalized);
    }
    updateControls();
}

/**
 * 刷新读取模式按钮与开始按钮文案。
 */
function renderReadModeButtons() {
    const tailMode = isTailMode();
    const tailButton = el('btnReadTail');
    const catButton = el('btnReadCat');
    const startButton = el('btnStart');
    const stopButton = el('btnStop');
    if (tailButton) {
        tailButton.classList.toggle('active', tailMode);
    }
    if (catButton) {
        catButton.classList.toggle('active', !tailMode);
    }
    if (startButton) {
        startButton.textContent = tailMode ? '⏵' : '⟳';
        startButton.title = tailMode ? '开始实时监听（tail）' : '读取一次快照（cat）';
        startButton.setAttribute('aria-label', tailMode ? '开始实时监听' : '读取一次快照');
    }
    if (stopButton) {
        stopButton.title = tailMode ? '结束查看' : '停止实时监听';
    }
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
        button.textContent = active ? '⤡' : '⤢';
        button.title = active ? '退出沉浸模式' : '进入沉浸模式';
        button.setAttribute('aria-label', active ? '退出沉浸模式' : '进入沉浸模式');
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
    const wsActive = isSocketActive();
    const tailMode = isTailMode();
    const tailLocked = wsActive && tailMode;
    const catBusy = !!state.catLoading;
    statusBar.setLatencyEnabled(tailLocked);
    el('project').disabled = tailLocked || catBusy;
    el('env').disabled = tailLocked || catBusy;
    el('service').disabled = tailLocked || catBusy;
    el('lines').disabled = catBusy;
    el('btnReadTail').disabled = catBusy;
    el('btnReadCat').disabled = catBusy;
    el('btnStart').disabled = catBusy || tailLocked;
    el('btnStop').disabled = !tailLocked;
    el('btnPause').disabled = !tailLocked;
    filterChainManager.setDisabled(catBusy);
    renderReadModeButtons();
    updateFileMode();
}

/**
 * 刷新文件区域模式与文案。
 */
function updateFileMode() {
    const project = value('project');
    const env = value('env');
    const service = value('service');
    const aggregate = isAggregateSelected(project, env, service);
    const catBusy = !!state.catLoading;
    const fileSearchEl = el('fileSearch');
    el('file').disabled = catBusy || aggregate;
    fileSearchEl.disabled = catBusy || aggregate;
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
    scheduleTailSubscriptionRefresh();
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
        project: params.get('project') || '',
        env: params.get('env') || '',
        service: params.get('service') || '',
        file: params.get('file') || '',
        lines: params.get('lines') || '',
        level: params.get('level') || '',
        autoScroll: params.get('autoScroll') || '',
        autoReconnect: params.get('autoReconnect') || '',
        showSystem: params.get('showSystem') || '',
        uiMode: params.get('uiMode') || '',
        uiDensity: params.get('uiDensity') || '',
        readMode: params.get('readMode') || '',
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
    const mode = isAggregateSelected(value('project'), value('env'), value('service')) ? '全部服务' : '单服务';
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
