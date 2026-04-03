import {el} from './dom.js';

/**
 * 可聚焦元素选择器。
 */
const FOCUSABLE_SELECTOR = [
    'a[href]',
    'button:not([disabled])',
    'input:not([disabled])',
    'select:not([disabled])',
    'textarea:not([disabled])',
    '[tabindex]:not([tabindex="-1"])'
].join(',');

/**
 * 日志面板抽屉/弹层控制器。
 * <p>
 * 统一处理：
 * 1. 设置抽屉开关与焦点回退；
 * 2. 并发流弹层的打开、关闭、加载与渲染；
 * 3. Escape / Tab 焦点陷阱等可访问性交互。
 * </p>
 */
export class OverlayController {

    /**
     * 构造方法。
     *
     * @param {Object} options 配置项
     * @param {Function} options.persistSettingsOpen 持久化设置抽屉开关状态
     */
    constructor(options = {}) {
        this.persistSettingsOpen = typeof options.persistSettingsOpen === 'function'
            ? options.persistSettingsOpen
            : (() => {});
        this.concurrentStreamAccessEnabled = false;
        this.activeOverlayKey = '';
        this.overlayState = {
            settings: {
                triggerId: 'btnOpenSettings',
                rootId: 'settingsDrawer',
                maskId: 'settingsDrawerMask',
                bodyClass: 'settings-drawer-open',
                toggleHiddenClass: false,
                lastFocusedElement: null
            },
            concurrent: {
                triggerId: 'btnConcurrentStreams',
                rootId: 'concurrentStreamsDialog',
                maskId: 'concurrentStreamsMask',
                bodyClass: 'concurrent-stream-dialog-open',
                toggleHiddenClass: true,
                lastFocusedElement: null
            }
        };
        this.handleDocumentKeydown = this.handleDocumentKeydown.bind(this);
    }

    /**
     * 初始化控制器并绑定相关事件。
     */
    init() {
        this.bindEvents();
        document.addEventListener('keydown', this.handleDocumentKeydown, true);
    }

    /**
     * 绑定按钮与遮罩点击事件。
     */
    bindEvents() {
        const openSettingsButton = el('btnOpenSettings');
        if (openSettingsButton) {
            openSettingsButton.addEventListener('click', event => this.openSettingsDrawer(event.currentTarget));
        }
        const closeSettingsButton = el('btnCloseSettings');
        if (closeSettingsButton) {
            closeSettingsButton.addEventListener('click', () => this.closeSettingsDrawer(true));
        }
        const settingsMask = el('settingsDrawerMask');
        if (settingsMask) {
            settingsMask.addEventListener('click', () => this.closeSettingsDrawer(true));
        }
        const openConcurrentButton = el('btnConcurrentStreams');
        if (openConcurrentButton) {
            openConcurrentButton.addEventListener('click', event => this.openConcurrentStreamDialog(event.currentTarget));
        }
        const closeConcurrentButton = el('btnCloseConcurrentStreams');
        if (closeConcurrentButton) {
            closeConcurrentButton.addEventListener('click', () => this.closeConcurrentStreamDialog(true));
        }
        const concurrentMask = el('concurrentStreamsMask');
        if (concurrentMask) {
            concurrentMask.addEventListener('click', () => this.closeConcurrentStreamDialog(true));
        }
    }

    /**
     * 加载并发流概览入口权限。
     * <p>
     * 仅当当前访问来源属于运行节点本机 IP 时，才显示“流占用”按钮。
     * </p>
     */
    loadConcurrentStreamAccess() {
        const button = el('btnConcurrentStreams');
        if (!button) {
            return;
        }
        this.setConcurrentStreamButtonVisible(false);
        fetch('/api/stream-usage/access')
            .then(response => {
                if (!response.ok) {
                    throw new Error(`status:${response.status}`);
                }
                return response.json();
            })
            .then(payload => {
                this.concurrentStreamAccessEnabled = !!(payload && payload.enabled);
                this.setConcurrentStreamButtonVisible(this.concurrentStreamAccessEnabled);
            })
            .catch(() => {
                this.concurrentStreamAccessEnabled = false;
                this.setConcurrentStreamButtonVisible(false);
            });
    }

    /**
     * 打开设置抽屉。
     *
     * @param {HTMLElement|null} triggerEl 触发按钮
     */
    openSettingsDrawer(triggerEl) {
        this.setSettingsDrawerOpen(true, true, triggerEl, false);
    }

    /**
     * 关闭设置抽屉。
     *
     * @param {boolean} restoreFocus 是否恢复焦点
     */
    closeSettingsDrawer(restoreFocus = true) {
        this.setSettingsDrawerOpen(false, true, null, restoreFocus);
    }

    /**
     * 同步设置抽屉可见状态，不触发焦点恢复。
     *
     * @param {boolean} open 是否打开
     * @param {boolean} persist 是否持久化
     */
    syncSettingsDrawerState(open, persist) {
        this.setSettingsDrawerOpen(open, persist, null, false);
    }

    /**
     * 判断设置抽屉是否打开。
     *
     * @returns {boolean} true 表示已打开
     */
    isSettingsDrawerOpen() {
        return this.activeOverlayKey === 'settings' || document.body.classList.contains('settings-drawer-open');
    }

    /**
     * 打开并发流概览弹层。
     *
     * @param {HTMLElement|null} triggerEl 触发按钮
     */
    openConcurrentStreamDialog(triggerEl) {
        if (!this.concurrentStreamAccessEnabled) {
            return;
        }
        this.setSettingsDrawerOpen(false, false, null, false);
        this.setConcurrentStreamDialogOpen(true, triggerEl, false);
        this.renderConcurrentStreamLoading();
        this.fetchConcurrentStreamUsage();
    }

    /**
     * 关闭并发流概览弹层。
     *
     * @param {boolean} restoreFocus 是否恢复焦点
     */
    closeConcurrentStreamDialog(restoreFocus = true) {
        this.setConcurrentStreamDialogOpen(false, null, restoreFocus);
    }

    /**
     * 判断并发流概览弹层是否打开。
     *
     * @returns {boolean} true 表示已打开
     */
    isConcurrentStreamDialogOpen() {
        return this.activeOverlayKey === 'concurrent'
            || document.body.classList.contains('concurrent-stream-dialog-open');
    }

    /**
     * 处理全局键盘事件。
     * <p>
     * 弹层/抽屉打开期间，优先拦截 Escape 与 Tab，保证关闭逻辑和焦点陷阱可用。
     * </p>
     *
     * @param {KeyboardEvent} event 键盘事件
     */
    handleDocumentKeydown(event) {
        const current = this.getActiveOverlayState();
        if (!current) {
            return;
        }
        if (event.key === 'Escape') {
            event.preventDefault();
            event.stopPropagation();
            if (this.activeOverlayKey === 'concurrent') {
                this.closeConcurrentStreamDialog(true);
                return;
            }
            this.closeSettingsDrawer(true);
            return;
        }
        if (event.key !== 'Tab') {
            return;
        }
        const root = el(current.rootId);
        if (!root) {
            return;
        }
        const focusableElements = collectFocusableElements(root);
        if (!focusableElements.length) {
            event.preventDefault();
            root.focus();
            return;
        }
        const first = focusableElements[0];
        const last = focusableElements[focusableElements.length - 1];
        const activeElement = document.activeElement;
        if (event.shiftKey) {
            if (activeElement === first || !root.contains(activeElement)) {
                event.preventDefault();
                last.focus();
            }
            return;
        }
        if (activeElement === last) {
            event.preventDefault();
            first.focus();
        }
    }

    /**
     * 设置设置抽屉开关状态。
     *
     * @param {boolean} open 是否打开
     * @param {boolean} persist 是否持久化
     * @param {HTMLElement|null} triggerEl 触发按钮
     * @param {boolean} restoreFocus 是否恢复焦点
     */
    setSettingsDrawerOpen(open, persist, triggerEl, restoreFocus) {
        const visible = !!open;
        const state = this.overlayState.settings;
        if (visible && this.isConcurrentStreamDialogOpen()) {
            this.setConcurrentStreamDialogOpen(false, null, false);
        }
        this.setOverlayVisible(state, visible, triggerEl, restoreFocus);
        if (persist) {
            this.persistSettingsOpen(visible);
        }
    }

    /**
     * 设置并发流概览弹层开关状态。
     *
     * @param {boolean} open 是否打开
     * @param {HTMLElement|null} triggerEl 触发按钮
     * @param {boolean} restoreFocus 是否恢复焦点
     */
    setConcurrentStreamDialogOpen(open, triggerEl, restoreFocus) {
        this.setOverlayVisible(this.overlayState.concurrent, !!open, triggerEl, restoreFocus);
    }

    /**
     * 统一切换弹层/抽屉可见状态。
     *
     * @param {Object} state 弹层状态
     * @param {boolean} visible 是否可见
     * @param {HTMLElement|null} triggerEl 触发按钮
     * @param {boolean} restoreFocus 是否恢复焦点
     */
    setOverlayVisible(state, visible, triggerEl, restoreFocus) {
        const root = el(state.rootId);
        const mask = el(state.maskId);
        const trigger = el(state.triggerId);
        if (!root) {
            return;
        }
        if (visible) {
            state.lastFocusedElement = triggerEl || document.activeElement;
            this.activeOverlayKey = resolveOverlayKey(this.overlayState, state);
        } else if (this.activeOverlayKey === resolveOverlayKey(this.overlayState, state)) {
            this.activeOverlayKey = '';
        }
        document.body.classList.toggle(state.bodyClass, visible);
        if (mask) {
            mask.classList.toggle('hidden', !visible);
            mask.setAttribute('aria-hidden', visible ? 'false' : 'true');
        }
        if (state.toggleHiddenClass) {
            root.classList.toggle('hidden', !visible);
        }
        root.setAttribute('aria-hidden', visible ? 'false' : 'true');
        updateOverlayInertState(root, visible);
        if (trigger) {
            trigger.setAttribute('aria-expanded', visible ? 'true' : 'false');
        }
        if (visible) {
            focusOverlay(root);
            return;
        }
        if (restoreFocus) {
            restoreElementFocus(state.lastFocusedElement);
        }
    }

    /**
     * 设置并发流按钮显示状态。
     *
     * @param {boolean} visible 是否显示
     */
    setConcurrentStreamButtonVisible(visible) {
        const button = el('btnConcurrentStreams');
        if (!button) {
            return;
        }
        button.classList.toggle('hidden', !visible);
        button.setAttribute('aria-hidden', visible ? 'false' : 'true');
    }

    /**
     * 拉取当前流占用概览。
     */
    fetchConcurrentStreamUsage() {
        fetch('/api/stream-usage')
            .then(response => {
                if (response.status === 403) {
                    this.concurrentStreamAccessEnabled = false;
                    this.setConcurrentStreamButtonVisible(false);
                    throw new Error('当前访问来源不是运行节点本机 IP，无法查看流占用概览');
                }
                if (!response.ok) {
                    throw new Error(`请求失败(${response.status})`);
                }
                return response.json();
            })
            .then(snapshot => this.renderConcurrentStreamSnapshot(snapshot))
            .catch(error => {
                this.renderConcurrentStreamError(error && error.message
                    ? String(error.message)
                    : '流占用概览加载失败');
            });
    }

    /**
     * 渲染并发流概览加载态。
     */
    renderConcurrentStreamLoading() {
        const summaryEl = el('concurrentStreamsSummary');
        const groupsEl = el('concurrentStreamsGroups');
        if (summaryEl) {
            summaryEl.innerHTML = '';
        }
        if (groupsEl) {
            groupsEl.innerHTML = '<div class="stream-usage-loading">正在加载当前流占用情况...</div>';
        }
    }

    /**
     * 渲染并发流概览错误态。
     *
     * @param {string} message 错误信息
     */
    renderConcurrentStreamError(message) {
        const summaryEl = el('concurrentStreamsSummary');
        const groupsEl = el('concurrentStreamsGroups');
        if (summaryEl) {
            summaryEl.innerHTML = '';
        }
        if (groupsEl) {
            groupsEl.innerHTML = `<div class="stream-usage-error">${escapeHtml(message || '流占用概览加载失败')}</div>`;
        }
    }

    /**
     * 渲染并发流概览。
     *
     * @param {*} snapshot 流占用概览数据
     */
    renderConcurrentStreamSnapshot(snapshot) {
        const safeSnapshot = snapshot || {};
        this.renderConcurrentStreamSummary(safeSnapshot);
        this.renderConcurrentStreamGroups(Array.isArray(safeSnapshot.ipGroups) ? safeSnapshot.ipGroups : []);
    }

    /**
     * 渲染并发流概览头部摘要。
     *
     * @param {*} snapshot 流占用概览数据
     */
    renderConcurrentStreamSummary(snapshot) {
        const summaryEl = el('concurrentStreamsSummary');
        if (!summaryEl) {
            return;
        }
        const max = normalizeNonNegativeInteger(snapshot.maxConcurrentStreams);
        const active = normalizeNonNegativeInteger(snapshot.activeConcurrentStreams);
        const remaining = normalizeNonNegativeInteger(snapshot.remainingConcurrentStreams);
        const groupCount = normalizeNonNegativeInteger(snapshot.activeClientIpCount);
        const updatedAt = formatDateTime(snapshot.updatedAt);
        summaryEl.innerHTML = [
            buildConcurrentStreamSummaryCard('总上限', String(max), updatedAt ? `快照时间 ${updatedAt}` : ''),
            buildConcurrentStreamSummaryCard('已占用', String(active), `占比 ${max > 0 ? Math.round(active * 100 / max) : 0}%`),
            buildConcurrentStreamSummaryCard('剩余', String(remaining), '可继续分配给新的 tail 流'),
            buildConcurrentStreamSummaryCard('来源 IP', String(groupCount), '按客户端访问来源聚合')
        ].join('');
    }

    /**
     * 渲染按 IP 分组的流占用明细。
     *
     * @param {Array} groups IP 分组列表
     */
    renderConcurrentStreamGroups(groups) {
        const groupsEl = el('concurrentStreamsGroups');
        if (!groupsEl) {
            return;
        }
        if (!groups.length) {
            groupsEl.innerHTML = '<div class="stream-usage-empty">当前没有活跃的 tail 流占用。</div>';
            return;
        }
        groupsEl.innerHTML = groups.map(group => buildConcurrentStreamGroupHtml(group)).join('');
    }

    /**
     * 获取当前激活的弹层状态。
     *
     * @returns {Object|null} 当前激活状态
     */
    getActiveOverlayState() {
        return this.activeOverlayKey ? this.overlayState[this.activeOverlayKey] || null : null;
    }
}

/**
 * 从状态表中反查当前状态键。
 *
 * @param {Object} stateMap 状态表
 * @param {Object} currentState 当前状态对象
 * @returns {string} 状态键
 */
function resolveOverlayKey(stateMap, currentState) {
    const hit = Object.keys(stateMap || {}).find(key => stateMap[key] === currentState);
    return hit || '';
}

/**
 * 将焦点移动到弹层内首个合理位置。
 *
 * @param {HTMLElement} root 弹层根节点
 */
function focusOverlay(root) {
    if (!root) {
        return;
    }
    window.requestAnimationFrame(() => {
        const initialFocus = root.querySelector('[data-overlay-initial-focus]');
        if (isFocusable(initialFocus)) {
            initialFocus.focus();
            return;
        }
        const focusableElements = collectFocusableElements(root);
        if (focusableElements.length) {
            focusableElements[0].focus();
            return;
        }
        root.focus();
    });
}

/**
 * 同步抽屉/弹层的 inert 状态，避免关闭后仍可被键盘聚焦。
 *
 * @param {HTMLElement} root 抽屉/弹层根节点
 * @param {boolean} visible 是否可见
 */
function updateOverlayInertState(root, visible) {
    if (!root) {
        return;
    }
    root.inert = !visible;
    if (visible) {
        root.removeAttribute('inert');
        return;
    }
    root.setAttribute('inert', '');
}

/**
 * 恢复元素焦点。
 *
 * @param {HTMLElement|null} target 目标元素
 */
function restoreElementFocus(target) {
    if (!target || typeof target.focus !== 'function' || !document.contains(target)) {
        return;
    }
    window.requestAnimationFrame(() => target.focus());
}

/**
 * 收集弹层内可聚焦元素。
 *
 * @param {HTMLElement} root 弹层根节点
 * @returns {Array<HTMLElement>} 可聚焦元素集合
 */
function collectFocusableElements(root) {
    if (!root) {
        return [];
    }
    return Array.from(root.querySelectorAll(FOCUSABLE_SELECTOR))
        .filter(node => isFocusable(node));
}

/**
 * 判断元素是否可聚焦且当前可见。
 *
 * @param {*} node 目标节点
 * @returns {boolean} true 表示可聚焦
 */
function isFocusable(node) {
    if (!node || typeof node.focus !== 'function') {
        return false;
    }
    if (node.hasAttribute('disabled') || node.getAttribute('aria-hidden') === 'true') {
        return false;
    }
    return node.getClientRects().length > 0 || node === document.activeElement;
}

/**
 * 构建流占用摘要卡片 HTML。
 *
 * @param {string} label 摘要标题
 * @param {string} value 摘要值
 * @param {string} meta 摘要补充说明
 * @returns {string} 卡片 HTML
 */
function buildConcurrentStreamSummaryCard(label, value, meta) {
    return `<article class="stream-usage-summary-card">
        <span class="stream-usage-summary-label">${escapeHtml(label)}</span>
        <span class="stream-usage-summary-value">${escapeHtml(value)}</span>
        <span class="stream-usage-summary-meta">${escapeHtml(meta)}</span>
    </article>`;
}

/**
 * 构建单个 IP 分组区域 HTML。
 *
 * @param {*} group 单个 IP 分组
 * @returns {string} 分组 HTML
 */
function buildConcurrentStreamGroupHtml(group) {
    const streams = Array.isArray(group && group.streams) ? group.streams : [];
    const ip = group && group.clientIp ? String(group.clientIp) : 'unknown';
    const streamCount = normalizeNonNegativeInteger(group && group.streamCount);
    const sessionCount = normalizeNonNegativeInteger(group && group.sessionCount);
    const startedAt = formatDateTime(group && group.firstStartedAt);
    const rowsHtml = buildConcurrentStreamRowsHtml(streams);
    return `<section class="stream-usage-group">
        <div class="stream-usage-group-head">
            <div>
                <h4 class="stream-usage-group-title">${escapeHtml(ip)}</h4>
                <p class="stream-usage-group-meta">${streamCount} 条活跃流，${sessionCount} 个会话</p>
            </div>
            <span class="stream-usage-group-time">${escapeHtml(startedAt ? `最早占用 ${startedAt}` : '')}</span>
        </div>
        <div class="stream-usage-table-wrap">
            <table class="stream-usage-table">
                <thead>
                    <tr>
                        <th>会话</th>
                        <th>项目 / 环境 / 服务</th>
                        <th>主机</th>
                        <th>文件</th>
                        <th>模式</th>
                        <th>开始时间</th>
                    </tr>
                </thead>
                <tbody>${rowsHtml}</tbody>
            </table>
        </div>
    </section>`;
}

/**
 * 构建单个 IP 分组下的表格行 HTML。
 *
 * @param {Array} streams 单个 IP 分组下的活跃流列表
 * @returns {string} 表格行 HTML
 */
function buildConcurrentStreamRowsHtml(streams) {
    if (!Array.isArray(streams) || !streams.length) {
        return '';
    }
    const sessionGroupMap = new Map();
    streams.forEach(stream => {
        const sessionKey = stream && stream.sessionId ? String(stream.sessionId) : '-';
        if (!sessionGroupMap.has(sessionKey)) {
            sessionGroupMap.set(sessionKey, []);
        }
        sessionGroupMap.get(sessionKey).push(stream);
    });
    return Array.from(sessionGroupMap.entries())
        .map(([sessionId, sessionStreams]) => {
            return sessionStreams.map((stream, index) => {
                const sessionCellHtml = index === 0
                    ? `<td class="stream-usage-session stream-usage-session-cell" rowspan="${sessionStreams.length}">${escapeHtml(abbreviateText(sessionId, 18))}</td>`
                    : '';
                return buildConcurrentStreamRowHtml(stream, sessionCellHtml);
            }).join('');
        })
        .join('');
}

/**
 * 构建单条流明细行 HTML。
 *
 * @param {*} stream 单条流明细
 * @param {string} sessionCellHtml 会话列 HTML
 * @returns {string} 明细行 HTML
 */
function buildConcurrentStreamRowHtml(stream, sessionCellHtml) {
    const project = stream && stream.project ? String(stream.project) : '-';
    const env = stream && stream.env ? String(stream.env) : '-';
    const service = stream && stream.service ? String(stream.service) : '-';
    const host = stream && stream.host ? String(stream.host) : '-';
    const filePath = stream && stream.filePath ? String(stream.filePath) : '-';
    const readMode = stream && stream.readMode ? String(stream.readMode).toUpperCase() : '-';
    const startedAt = formatDateTime(stream && stream.startedAt);
    return `<tr>
        ${sessionCellHtml || ''}
        <td class="stream-usage-service-cell">
            <strong>${escapeHtml(service)}</strong>
            <span>${escapeHtml(`${project} / ${env}`)}</span>
        </td>
        <td>${escapeHtml(host)}</td>
        <td class="stream-usage-file">${escapeHtml(filePath)}</td>
        <td>${escapeHtml(readMode)}</td>
        <td>${escapeHtml(startedAt || '-')}</td>
    </tr>`;
}

/**
 * 将任意输入规整为非负整数。
 *
 * @param {*} value 原始输入
 * @returns {number} 非负整数
 */
function normalizeNonNegativeInteger(value) {
    const numeric = Number(value);
    if (!Number.isFinite(numeric) || numeric <= 0) {
        return 0;
    }
    return Math.round(numeric);
}

/**
 * 格式化时间戳。
 *
 * @param {*} timestamp 时间戳
 * @returns {string} 格式化后的时间文本
 */
function formatDateTime(timestamp) {
    const numeric = Number(timestamp);
    if (!Number.isFinite(numeric) || numeric <= 0) {
        return '';
    }
    try {
        return new Date(numeric).toLocaleString('zh-CN', {hour12: false});
    } catch (error) {
        return '';
    }
}

/**
 * 转义 HTML 文本。
 *
 * @param {*} value 原始文本
 * @returns {string} 转义后的文本
 */
function escapeHtml(value) {
    return String(value || '')
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
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
