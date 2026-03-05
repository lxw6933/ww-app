import {el, checked} from './dom.js';

/**
 * 日志视图管理器。
 * <p>
 * 负责日志渲染、行数统计、系统消息显示控制与滚动行为管理。
 * </p>
 */
export class LogView {

    /**
     * 构造方法。
     *
     * @param {Object} state 页面状态对象
     * @param {number} maxLines 最大日志行数
     */
    constructor(state, maxLines) {
        this.state = state;
        this.maxLines = maxLines;
        this.highlightKeywords = [];
        this.filterRules = [];
        this.levelFilter = 'ALL';
        this.deferredSearchTimer = null;
        this.deferMs = 300;
        this.pendingChunks = [];
        this.appendFlushTimer = null;
        this.appendFlushMs = 80;
        this.searchKeyword = '';
        this.searchMatches = [];
        this.searchCursor = -1;
        this.metricsRefreshTimer = null;
        this.metricsRefreshMs = 180;
    }

    /**
     * 设置状态栏文本。
     *
     * @param {string} text 状态文本
     * @param {string} color 文本颜色
     */
    setStatus(text, color) {
        const statusEl = el('status');
        statusEl.textContent = text;
        statusEl.style.color = color;
    }

    /**
     * 处理收到的 WebSocket 文本消息。
     *
     * @param {string} text 消息内容
     */
    onMessage(text) {
        if (this.state.paused) {
            this.state.buffer.push(text);
            if (this.state.buffer.length > 300) {
                this.state.buffer.shift();
            }
            return;
        }
        this.enqueueAppend(text);
    }

    /**
     * 将日志分块入队，并按短周期合并刷新，减少高频渲染抖动。
     *
     * @param {string} text 消息内容
     */
    enqueueAppend(text) {
        const chunk = String(text || '');
        if (!chunk) {
            return;
        }
        this.pendingChunks.push(chunk);
        if (this.appendFlushTimer) {
            return;
        }
        this.appendFlushTimer = window.setTimeout(() => {
            this.appendFlushTimer = null;
            this.flushPendingChunks();
        }, this.appendFlushMs);
    }

    /**
     * 合并并落地待渲染日志块。
     */
    flushPendingChunks() {
        if (!this.pendingChunks.length) {
            return;
        }
        const merged = this.pendingChunks.join('');
        this.pendingChunks = [];
        this.appendLines(merged);
    }

    /**
     * 切换暂停/继续状态。
     */
    togglePause() {
        this.state.paused = !this.state.paused;
        const pauseBtn = el('btnPause');
        pauseBtn.textContent = this.state.paused ? '继续' : '暂停';
        if (this.state.paused) {
            if (this.appendFlushTimer) {
                window.clearTimeout(this.appendFlushTimer);
                this.appendFlushTimer = null;
            }
            if (this.pendingChunks.length) {
                this.state.buffer.push(this.pendingChunks.join(''));
                this.pendingChunks = [];
                while (this.state.buffer.length > 300) {
                    this.state.buffer.shift();
                }
            }
            return;
        }
        if (!this.state.paused && this.state.buffer.length > 0) {
            this.appendLines(this.state.buffer.join(''));
            this.state.buffer = [];
        }
    }

    /**
     * 重置暂停状态，避免重新连接后仍处于暂停接收。
     */
    resetPause() {
        this.state.paused = false;
        this.state.buffer = [];
        el('btnPause').textContent = '暂停';
    }

    /**
     * 追加系统提示行。
     *
     * @param {string} text 提示内容
     */
    appendSystem(text) {
        this.appendLines(`[系统提示] ${text}\n`);
    }

    /**
     * 追加日志文本块。
     *
     * @param {string} text 文本内容
     */
    appendLines(text) {
        const logEl = el('log');
        const fragment = document.createDocumentFragment();
        let appendCount = 0;
        String(text || '').split(/\r?\n/).forEach(line => {
            if (!line) {
                return;
            }
            const lineEl = this.createLogLineElement(line);
            fragment.appendChild(lineEl);
            appendCount += 1;
        });
        if (!appendCount) {
            return;
        }
        logEl.appendChild(fragment);

        this.trimLines(logEl);
        if (this.searchKeyword) {
            this.scheduleSearchRefresh();
        }
        this.state.lineCount = logEl.children.length;
        this.scheduleMetricsRefresh();
        if (checked('autoScroll')) {
            this.scrollToBottom();
        } else {
            this.updateBottomButton();
        }
    }

    /**
     * 清空日志区域。
     */
    clearLogs() {
        const logEl = el('log');
        logEl.textContent = '';
        this.pendingChunks = [];
        this.cancelMetricsRefresh();
        if (this.appendFlushTimer) {
            window.clearTimeout(this.appendFlushTimer);
            this.appendFlushTimer = null;
        }
        if (this.deferredSearchTimer) {
            window.clearTimeout(this.deferredSearchTimer);
            this.deferredSearchTimer = null;
        }
        this.setEmptyTip(this.isSocketActive()
            ? '日志窗口已清空，等待新日志推送...'
            : '日志已清空。请点击“开始查看”重新监听。');
        this.state.buffer = [];
        this.searchMatches = [];
        this.searchCursor = -1;
        this.updateSearchCounter();
        this.state.lineCount = 0;
        this.renderLineCount();
        this.renderLevelStats();
        this.updateBottomButton();
    }

    /**
     * 插入手工换行标记。
     */
    appendManualBreak() {
        const logEl = el('log');
        const lineEl = this.createLogLineElement('---------------- 分隔线（便于区分前后日志） ----------------');
        // 手工分割线属于操作标记，需始终可见，不受系统消息/级别/规则筛选影响。
        lineEl.classList.add('line-system', 'line-manual-break');
        lineEl.dataset.level = 'SYSTEM';
        lineEl.classList.remove('line-level-hidden', 'line-filter-hidden', 'hidden');
        logEl.appendChild(lineEl);
        this.trimLines(logEl);
        if (this.searchKeyword) {
            this.scheduleSearchRefresh();
        }
        this.state.lineCount = logEl.children.length;
        this.renderLineCount();
        this.renderLevelStats();
        if (checked('autoScroll')) {
            this.scrollToBottom();
        } else {
            this.updateBottomButton();
        }
    }

    /**
     * 切换系统消息显示状态。
     */
    refreshSystemVisibility() {
        const show = checked('showSystem');
        document.querySelectorAll('#log .line-system-msg').forEach(lineEl => {
            if (show) {
                lineEl.classList.remove('hidden');
            } else {
                lineEl.classList.add('hidden');
            }
        });
        this.refreshSearchMatches(true, false);
        this.renderLineCount();
        this.renderLevelStats();
    }

    /**
     * 滚动到日志底部。
     */
    scrollToBottom() {
        const logEl = el('log');
        logEl.scrollTop = logEl.scrollHeight;
        this.updateBottomButton();
    }

    /**
     * 根据滚动位置更新“回到底部”按钮显示。
     */
    updateBottomButton() {
        const bottomBtn = el('btnBottom');
        if (checked('autoScroll')) {
            bottomBtn.style.display = 'none';
            return;
        }
        const logEl = el('log');
        const nearBottom = logEl.scrollTop + logEl.clientHeight >= logEl.scrollHeight - 20;
        bottomBtn.style.display = nearBottom ? 'none' : 'inline-block';
    }

    /**
     * 应用单行日志样式。
     *
     * @param {HTMLElement} lineEl 行节点
     * @param {string} line 文本内容
     */
    applyLineClass(lineEl, line) {
        const isSystem = line.indexOf('[系统提示]') >= 0;
        const level = this.detectLineLevel(line, isSystem);
        lineEl.dataset.level = level;
        if (isSystem) {
            lineEl.classList.add('line-system', 'line-system-msg');
            if (!checked('showSystem')) {
                lineEl.classList.add('hidden');
            }
        }
        if (level === 'ERROR') {
            lineEl.classList.add('line-error');
        } else if (level === 'WARN') {
            lineEl.classList.add('line-warn');
        }
        if (line.indexOf('连接') >= 0 && line.indexOf('失败') < 0) {
            lineEl.classList.add('line-ok');
        }
        this.applyLevelFilterForLine(lineEl);
    }

    /**
     * 创建日志行元素，内置复制按钮。
     *
     * @param {string} line 文本内容
     * @returns {HTMLDivElement} 日志行元素
     */
    createLogLineElement(line) {
        const lineEl = document.createElement('div');
        lineEl.className = 'log-line';
        lineEl.dataset.rawText = line;

        const textEl = document.createElement('span');
        textEl.className = 'line-text';
        this.renderLineText(textEl, line);

        const copyBtn = document.createElement('button');
        copyBtn.type = 'button';
        copyBtn.className = 'line-copy-btn';
        copyBtn.textContent = '复制';
        copyBtn.title = '复制本行';

        lineEl.appendChild(textEl);
        lineEl.appendChild(copyBtn);
        this.applyLineClass(lineEl, line);
        this.applyRuleFilterForLine(lineEl);
        return lineEl;
    }

    /**
     * 复制日志行文本并反馈按钮状态。
     *
     * @param {HTMLButtonElement} buttonEl 复制按钮
     * @param {string} text 日志文本
     */
    copyLineFromButton(buttonEl, text) {
        if (!buttonEl) {
            return;
        }
        this.copyText(text)
            .then(() => this.renderCopyButtonStatus(buttonEl, true))
            .catch(() => this.renderCopyButtonStatus(buttonEl, false));
    }

    /**
     * 复制文本到剪贴板（优先 Clipboard API）。
     *
     * @param {string} text 文本内容
     * @returns {Promise<void>} 异步结果
     */
    copyText(text) {
        const normalized = String(text || '');
        if (navigator.clipboard && window.isSecureContext) {
            return navigator.clipboard.writeText(normalized);
        }
        return new Promise((resolve, reject) => {
            try {
                const textarea = document.createElement('textarea');
                textarea.value = normalized;
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
                reject(new Error('copy command failed'));
            } catch (error) {
                reject(error);
            }
        });
    }

    /**
     * 渲染复制按钮结果状态。
     *
     * @param {HTMLButtonElement} buttonEl 按钮元素
     * @param {boolean} success 是否成功
     */
    renderCopyButtonStatus(buttonEl, success) {
        if (!buttonEl) {
            return;
        }
        const originalText = '复制';
        buttonEl.textContent = success ? '已复制' : '失败';
        buttonEl.classList.remove('done', 'error');
        buttonEl.classList.add(success ? 'done' : 'error');
        window.setTimeout(() => {
            buttonEl.textContent = originalText;
            buttonEl.classList.remove('done', 'error');
        }, 900);
    }

    /**
     * 日志行数裁剪，避免页面内存无限增长。
     *
     * @param {HTMLElement} logEl 日志容器
     */
    trimLines(logEl) {
        const overflow = logEl.children.length - this.maxLines;
        for (let index = 0; index < overflow; index += 1) {
            logEl.removeChild(logEl.firstChild);
        }
    }

    /**
     * 刷新行数显示。
     */
    renderLineCount() {
        const logEl = el('log');
        const total = logEl ? logEl.children.length : this.state.lineCount;
        if (!logEl) {
            el('lineCount').textContent = `${total} 行`;
            return;
        }
        // 常见路径：未启用级别筛选、未隐藏系统消息、无过滤规则时，可直接使用总行数，避免全量扫描。
        const canUseTotalDirectly = this.levelFilter === 'ALL'
            && checked('showSystem')
            && (!this.filterRules || this.filterRules.length === 0);
        const visible = canUseTotalDirectly
            ? total
            : this.countVisibleLines(logEl);
        this.state.lineCount = total;
        if (visible === total) {
            el('lineCount').textContent = `${visible} 行`;
            return;
        }
        el('lineCount').textContent = `${visible}/${total} 行`;
    }

    /**
     * 统计当前日志区可见行数。
     *
     * @param {HTMLElement} logEl 日志容器
     * @returns {number} 可见行数量
     */
    countVisibleLines(logEl) {
        if (!logEl || !logEl.children) {
            return 0;
        }
        let visible = 0;
        for (let index = 0; index < logEl.children.length; index += 1) {
            const lineEl = logEl.children[index];
            if (this.isLineVisible(lineEl)) {
                visible += 1;
            }
        }
        return visible;
    }

    /**
     * 设置日志空状态文案。
     *
     * @param {string} text 提示文案
     */
    setEmptyTip(text) {
        const logEl = el('log');
        if (!logEl) {
            return;
        }
        const tip = String(text || '').trim();
        logEl.setAttribute('data-empty-tip', tip || '暂无日志数据');
    }

    /**
     * 设置日志关键词高亮规则。
     *
     * @param {Array<{type:string,data:string}>} rules 过滤规则
     */
    setHighlightRules(rules) {
        this.filterRules = this.normalizeFilterRules(rules);
        const keywordSet = new Set();
        this.filterRules.forEach(rule => {
            const expression = rule && rule.data ? String(rule.data) : '';
            this.extractHighlightKeywords(expression).forEach(keyword => {
                keywordSet.add(keyword.toLowerCase());
            });
        });
        this.highlightKeywords = Array.from(keywordSet).sort((left, right) => right.length - left.length);
        this.refreshExistingHighlights();
    }

    /**
     * 从规则表达式中提取用于高亮的关键词。
     * <p>
     * 支持按 && 与 || 拆分，忽略空白词。
     * </p>
     *
     * @param {string} expression 规则表达式
     * @returns {Array<string>} 关键词数组
     */
    extractHighlightKeywords(expression) {
        return String(expression || '')
            .split(/(?:\|\||&&)/)
            .map(item => item.trim())
            .filter(Boolean);
    }

    /**
     * 规范化过滤规则，保留合法 include/exclude 条件。
     *
     * @param {Array<{type:string,data:string}>} rules 原始规则
     * @returns {Array<{type:string,data:string}>} 规范化规则
     */
    normalizeFilterRules(rules) {
        const normalized = [];
        (rules || []).forEach(rule => {
            const type = rule && rule.type ? String(rule.type).trim().toLowerCase() : '';
            const data = rule && rule.data ? String(rule.data).trim() : '';
            if (!data) {
                return;
            }
            if (type !== 'include' && type !== 'exclude') {
                return;
            }
            normalized.push({type: type, data: data});
        });
        return normalized;
    }

    /**
     * 按当前规则更新单行日志的可见性。
     *
     * @param {HTMLElement} lineEl 日志行节点
     */
    applyRuleFilterForLine(lineEl) {
        if (!lineEl) {
            return;
        }
        if (lineEl.classList.contains('line-system-msg') || lineEl.classList.contains('line-manual-break')) {
            lineEl.classList.remove('line-filter-hidden');
            return;
        }
        const rawText = lineEl.dataset && lineEl.dataset.rawText ? lineEl.dataset.rawText : '';
        const visible = this.matchesRuleChain(rawText);
        lineEl.classList.toggle('line-filter-hidden', !visible);
    }

    /**
     * 按规则链判断日志是否应显示。
     * <p>
     * 与后端保持一致：多 include 为 AND，多 exclude 为 OR；
     * 单条规则内部支持 && / ||（先与后或）。
     * </p>
     *
     * @param {string} line 日志文本
     * @returns {boolean} 是否显示
     */
    matchesRuleChain(line) {
        const text = String(line || '');
        if (!this.filterRules.length) {
            return true;
        }
        for (const rule of this.filterRules) {
            if (rule.type === 'include') {
                if (!this.matchesKeywordExpression(text, rule.data)) {
                    return false;
                }
                continue;
            }
            if (rule.type === 'exclude' && this.matchesKeywordExpression(text, rule.data)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 匹配关键字表达式（支持 && / ||）。
     *
     * @param {string} line 日志文本
     * @param {string} expression 规则表达式
     * @returns {boolean} 是否命中
     */
    matchesKeywordExpression(line, expression) {
        const groups = String(expression || '')
            .split('||')
            .map(item => item.trim())
            .filter(Boolean);
        if (!groups.length) {
            return false;
        }
        for (const group of groups) {
            if (this.matchesAndGroup(line, group)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 匹配 AND 分组：组内所有词都命中才成立。
     *
     * @param {string} line 日志文本
     * @param {string} group AND 分组文本
     * @returns {boolean} 是否命中
     */
    matchesAndGroup(line, group) {
        const terms = String(group || '')
            .split('&&')
            .map(item => item.trim())
            .filter(Boolean);
        if (!terms.length) {
            return false;
        }
        return terms.every(term => line.indexOf(term) >= 0);
    }

    /**
     * 刷新现有日志行的高亮展示。
     */
    refreshExistingHighlights() {
        const logEl = el('log');
        if (!logEl) {
            return;
        }
        logEl.querySelectorAll('.log-line').forEach(lineEl => {
            const textEl = lineEl.querySelector('.line-text');
            if (!textEl) {
                return;
            }
            const rawText = lineEl.dataset && lineEl.dataset.rawText ? lineEl.dataset.rawText : '';
            this.renderLineText(textEl, rawText);
            this.applyRuleFilterForLine(lineEl);
        });
        this.refreshSearchMatches(true, false);
        this.renderLineCount();
    }

    /**
     * 渲染日志文本并按关键词高亮。
     *
     * @param {HTMLElement} textEl 文本元素
     * @param {string} line 原始行文本
     */
    renderLineText(textEl, line) {
        const text = String(line || '');
        if (!this.highlightKeywords.length) {
            textEl.textContent = text;
            return;
        }
        const ranges = this.computeHighlightRanges(text);
        if (!ranges.length) {
            textEl.textContent = text;
            return;
        }

        textEl.textContent = '';
        const fragment = document.createDocumentFragment();
        let cursor = 0;
        ranges.forEach(range => {
            if (range.start > cursor) {
                fragment.appendChild(document.createTextNode(text.substring(cursor, range.start)));
            }
            const highlightEl = document.createElement('mark');
            highlightEl.className = 'line-highlight';
            highlightEl.textContent = text.substring(range.start, range.end);
            fragment.appendChild(highlightEl);
            cursor = range.end;
        });
        if (cursor < text.length) {
            fragment.appendChild(document.createTextNode(text.substring(cursor)));
        }
        textEl.appendChild(fragment);
    }

    /**
     * 计算关键词命中范围，并合并重叠区间。
     *
     * @param {string} line 行文本
     * @returns {Array<{start:number,end:number}>} 高亮区间
     */
    computeHighlightRanges(line) {
        const lowerLine = String(line || '').toLowerCase();
        const matches = [];
        this.highlightKeywords.forEach(keyword => {
            if (!keyword) {
                return;
            }
            let offset = 0;
            while (offset < lowerLine.length) {
                const index = lowerLine.indexOf(keyword, offset);
                if (index < 0) {
                    break;
                }
                matches.push({start: index, end: index + keyword.length});
                offset = index + Math.max(1, keyword.length);
            }
        });
        if (!matches.length) {
            return [];
        }

        matches.sort((left, right) => {
            if (left.start !== right.start) {
                return left.start - right.start;
            }
            return right.end - left.end;
        });

        const merged = [];
        matches.forEach(match => {
            const last = merged.length ? merged[merged.length - 1] : null;
            if (!last || match.start > last.end) {
                merged.push({start: match.start, end: match.end});
                return;
            }
            if (match.end > last.end) {
                last.end = match.end;
            }
        });
        return merged;
    }

    /**
     * 设置窗口内搜索关键词。
     *
     * @param {string} keyword 搜索关键词
     */
    setSearchKeyword(keyword) {
        this.searchKeyword = String(keyword || '').trim().toLowerCase();
        this.refreshSearchMatches(false, true);
    }

    /**
     * 设置日志级别过滤条件。
     *
     * @param {string} level 级别：ALL/INFO/WARN/ERROR
     */
    setLevelFilter(level) {
        const normalized = normalizeLevel(level);
        if (this.levelFilter === normalized) {
            return;
        }
        this.levelFilter = normalized;
        this.refreshLevelVisibility();
        this.refreshSearchMatches(true, false);
        this.renderLineCount();
    }

    /**
     * 跳转到下一个/上一个搜索命中。
     *
     * @param {boolean} reverse true 表示向上，false 表示向下
     */
    jumpSearch(reverse) {
        if (!this.searchMatches.length) {
            return;
        }
        if (this.searchCursor < 0) {
            this.searchCursor = reverse ? this.searchMatches.length - 1 : 0;
        } else {
            this.searchCursor = reverse ? this.searchCursor - 1 : this.searchCursor + 1;
            if (this.searchCursor < 0) {
                this.searchCursor = this.searchMatches.length - 1;
            }
            if (this.searchCursor >= this.searchMatches.length) {
                this.searchCursor = 0;
            }
        }
        this.paintSearchCurrent(true);
    }

    /**
     * 刷新搜索命中集合与样式。
     *
     * @param {boolean} keepCursor 是否尽量保留当前命中游标
     * @param {boolean} shouldScroll 是否滚动到当前命中
     */
    refreshSearchMatches(keepCursor, shouldScroll) {
        const logEl = el('log');
        if (!logEl) {
            return;
        }
        const lines = Array.from(logEl.querySelectorAll('.log-line'));
        const keyword = this.searchKeyword;
        const previousMatch = keepCursor && this.searchCursor >= 0 ? this.searchMatches[this.searchCursor] : null;
        this.searchMatches = [];

        lines.forEach(lineEl => {
            const raw = lineEl.dataset && lineEl.dataset.rawText ? lineEl.dataset.rawText : '';
            const hit = this.isLineVisible(lineEl) && keyword && raw.toLowerCase().indexOf(keyword) >= 0;
            lineEl.classList.toggle('line-search-hit', !!hit);
            lineEl.classList.remove('line-search-current');
            if (hit) {
                this.searchMatches.push(lineEl);
            }
        });

        if (!this.searchMatches.length) {
            this.searchCursor = -1;
            this.updateSearchCounter();
            return;
        }

        if (previousMatch) {
            const index = this.searchMatches.indexOf(previousMatch);
            this.searchCursor = index >= 0 ? index : 0;
        } else if (this.searchCursor >= this.searchMatches.length || this.searchCursor < 0) {
            this.searchCursor = 0;
        }
        this.paintSearchCurrent(shouldScroll);
    }

    /**
     * 渲染当前命中态并滚动到可视区。
     */
    paintSearchCurrent(shouldScroll) {
        this.searchMatches.forEach(lineEl => lineEl.classList.remove('line-search-current'));
        if (!this.searchMatches.length || this.searchCursor < 0 || this.searchCursor >= this.searchMatches.length) {
            this.updateSearchCounter();
            return;
        }
        const current = this.searchMatches[this.searchCursor];
        current.classList.add('line-search-current');
        if (shouldScroll) {
            current.scrollIntoView({block: 'center', behavior: 'smooth'});
        }
        this.updateSearchCounter();
    }

    /**
     * 更新搜索计数文本。
     */
    updateSearchCounter() {
        const counterEl = el('logSearchCount');
        if (!counterEl) {
            return;
        }
        if (!this.searchKeyword) {
            counterEl.textContent = '';
            counterEl.style.display = 'none';
            return;
        }
        counterEl.style.display = 'inline-flex';
        if (!this.searchMatches.length) {
            counterEl.textContent = '0 条';
            return;
        }
        counterEl.textContent = `${this.searchCursor + 1}/${this.searchMatches.length}`;
    }

    /**
     * 判断当前是否处于连接中/监听中状态。
     *
     * @returns {boolean} true 表示连接活跃
     */
    isSocketActive() {
        return !!(this.state.ws
            && (this.state.ws.readyState === WebSocket.OPEN || this.state.ws.readyState === WebSocket.CONNECTING));
    }

    /**
     * 异步调度搜索刷新，降低高频日志时的全量扫描压力。
     */
    scheduleSearchRefresh() {
        if (this.deferredSearchTimer) {
            return;
        }
        this.deferredSearchTimer = window.setTimeout(() => {
            this.deferredSearchTimer = null;
            this.refreshSearchMatches(true, false);
        }, this.deferMs);
    }

    /**
     * 延迟刷新行数与级别统计，降低高频日志写入时的全量扫描开销。
     */
    scheduleMetricsRefresh() {
        if (this.metricsRefreshTimer) {
            return;
        }
        this.metricsRefreshTimer = window.setTimeout(() => {
            this.metricsRefreshTimer = null;
            this.flushMetricsRefresh();
        }, this.metricsRefreshMs);
    }

    /**
     * 立即刷新行数与级别统计。
     */
    flushMetricsRefresh() {
        this.renderLineCount();
        this.renderLevelStats();
    }

    /**
     * 取消待执行的统计刷新任务。
     */
    cancelMetricsRefresh() {
        if (!this.metricsRefreshTimer) {
            return;
        }
        window.clearTimeout(this.metricsRefreshTimer);
        this.metricsRefreshTimer = null;
    }

    /**
     * 刷新级别过滤下的显示状态。
     */
    refreshLevelVisibility() {
        const filter = this.levelFilter;
        const rows = document.querySelectorAll('#log .log-line');
        rows.forEach(row => {
            this.applyLevelFilterForLine(row, filter);
        });
    }

    /**
     * 对单行应用级别过滤显示状态。
     *
     * @param {HTMLElement} row 行元素
     * @param {string} filter 过滤级别
     */
    applyLevelFilterForLine(row, filter) {
        if (!row) {
            return;
        }
        if (row.classList.contains('line-manual-break')) {
            row.classList.remove('line-level-hidden');
            return;
        }
        const level = row.dataset && row.dataset.level ? row.dataset.level : 'INFO';
        const visible = matchLevelFilter(filter || this.levelFilter, level);
        row.classList.toggle('line-level-hidden', !visible);
    }

    /**
     * 判断日志行当前是否可见（系统开关 + 级别过滤综合结果）。
     *
     * @param {HTMLElement} lineEl 日志行
     * @returns {boolean} true 表示可见
     */
    isLineVisible(lineEl) {
        if (!lineEl) {
            return false;
        }
        if (lineEl.classList.contains('line-level-hidden')) {
            return false;
        }
        if (lineEl.classList.contains('line-filter-hidden')) {
            return false;
        }
        return !lineEl.classList.contains('hidden');

    }

    /**
     * 推断日志级别。
     *
     * @param {string} line 日志文本
     * @param {boolean} isSystem 是否系统消息
     * @returns {string} 级别值
     */
    detectLineLevel(line, isSystem) {
        if (isSystem) {
            return 'SYSTEM';
        }
        const text = String(line || '');
        const lower = text.toLowerCase();
        if (lower.indexOf('error') >= 0
            || lower.indexOf('exception') >= 0
            || text.indexOf('异常') >= 0
            || text.indexOf('失败') >= 0) {
            return 'ERROR';
        }
        if (lower.indexOf('warn') >= 0
            || lower.indexOf('warning') >= 0
            || text.indexOf('告警') >= 0
            || text.indexOf('超时') >= 0) {
            return 'WARN';
        }
        return 'INFO';
    }

    /**
     * 刷新日志级别按钮中的统计数量。
     */
    renderLevelStats() {
        const stats = this.collectLevelStats();
        this.renderLevelButtonText('btnLevelAll', '全部', stats.ALL);
        this.renderLevelButtonText('btnLevelInfo', 'INFO', stats.INFO);
        this.renderLevelButtonText('btnLevelWarn', 'WARN', stats.WARN);
        this.renderLevelButtonText('btnLevelError', 'ERROR', stats.ERROR);
    }

    /**
     * 统计日志级别数量。
     *
     * @returns {{ALL:number,INFO:number,WARN:number,ERROR:number}} 统计结果
     */
    collectLevelStats() {
        const stats = {
            ALL: 0,
            INFO: 0,
            WARN: 0,
            ERROR: 0
        };
        const logEl = el('log');
        if (!logEl || !logEl.children) {
            return stats;
        }
        for (let index = 0; index < logEl.children.length; index += 1) {
            const lineEl = logEl.children[index];
            if (lineEl.classList.contains('hidden') || lineEl.classList.contains('line-filter-hidden')) {
                continue;
            }
            stats.ALL += 1;
            const level = lineEl.dataset && lineEl.dataset.level ? lineEl.dataset.level : 'INFO';
            if (level === 'ERROR') {
                stats.ERROR += 1;
                continue;
            }
            if (level === 'WARN') {
                stats.WARN += 1;
                continue;
            }
            stats.INFO += 1;
        }
        return stats;
    }

    /**
     * 渲染单个级别按钮文案。
     *
     * @param {string} buttonId 按钮 ID
     * @param {string} label 按钮基础文案
     * @param {number} count 统计数量
     */
    renderLevelButtonText(buttonId, label, count) {
        const button = el(buttonId);
        if (!button) {
            return;
        }
        button.textContent = `${label}(${count})`;
    }
}

/**
 * 规范化日志级别值。
 *
 * @param {string} level 级别值
 * @returns {string} 规范化结果
 */
function normalizeLevel(level) {
    const normalized = String(level || '').toUpperCase();
    if (normalized === 'INFO' || normalized === 'WARN' || normalized === 'ERROR') {
        return normalized;
    }
    return 'ALL';
}

/**
 * 判断级别是否满足过滤器。
 *
 * @param {string} filter 过滤条件
 * @param {string} level 行级别
 * @returns {boolean} true 表示命中
 */
function matchLevelFilter(filter, level) {
    if (filter === 'ALL') {
        return true;
    }
    if (level === 'SYSTEM') {
        return filter === 'INFO';
    }
    return level === filter;
}
