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

/**
 * 页面全局状态。
 */
const state = createState();

/**
 * 日志视图实例。
 */
const logView = new LogView(state, MAX_LINES);

window.addEventListener('load', init);

/**
 * 页面初始化。
 */
function init() {
    bindEvents();
    updateControls();
    loadServers();
}

/**
 * 绑定页面事件。
 */
function bindEvents() {
    el('env').addEventListener('change', loadServices);
    el('service').addEventListener('change', loadFiles);
    el('fileSearch').addEventListener('input', renderFileOptions);
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
    el('log').addEventListener('scroll', () => logView.updateBottomButton());
    document.addEventListener('keydown', onShortcut);

    ['lines', 'include', 'exclude'].forEach(id => {
        el(id).addEventListener('keydown', event => {
            if (event.key === 'Enter') {
                connect();
            }
        });
    });
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
        return;
    }

    const filtered = state.fileOptions.filter(path => matchFileKeyword(path, keyword));
    filtered.forEach(path => fileEl.add(new Option(fileName(path), path)));

    if (currentValue && filtered.indexOf(currentValue) >= 0) {
        fileEl.value = currentValue;
        return;
    }
    if (fileEl.options.length > 1) {
        fileEl.selectedIndex = 1;
    }
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

    const payload = {
        env: env,
        service: service || ALL,
        filePath: value('file'),
        lines: parseLines(value('lines')),
        includeKeyword: value('include'),
        excludeKeyword: value('exclude')
    };

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
    ['env', 'service', 'lines', 'include', 'exclude'].forEach(id => {
        el(id).disabled = locked;
    });
    el('btnStart').disabled = locked;
    el('btnStop').disabled = !locked;
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
    el('modeTip').textContent = aggregate ? '聚合模式' : '单服务模式';
    fileSearchEl.placeholder = aggregate ? '聚合模式下不支持按文件筛选' : '模糊匹配文件名';
}
