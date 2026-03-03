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
        this.appendLines(text);
    }

    /**
     * 切换暂停/继续状态。
     */
    togglePause() {
        this.state.paused = !this.state.paused;
        const pauseBtn = el('btnPause');
        pauseBtn.textContent = this.state.paused ? '继续接收' : '暂停接收';
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
        el('btnPause').textContent = '暂停接收';
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
        String(text || '').split(/\r?\n/).forEach(line => {
            if (!line) {
                return;
            }
            const lineEl = this.createLogLineElement(line);
            logEl.appendChild(lineEl);
        });

        this.trimLines(logEl);
        this.state.lineCount = logEl.children.length;
        this.renderLineCount();
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
        this.setEmptyTip(this.isSocketActive()
            ? '日志窗口已清空，等待新日志推送...'
            : '日志已清空。请点击“开始查看”重新监听。');
        this.state.buffer = [];
        this.state.lineCount = 0;
        this.renderLineCount();
        this.updateBottomButton();
    }

    /**
     * 插入手工换行标记。
     */
    appendManualBreak() {
        const logEl = el('log');
        const lineEl = this.createLogLineElement('---------------- 分隔线（便于区分前后日志） ----------------');
        lineEl.classList.add('line-system');
        logEl.appendChild(lineEl);
        this.trimLines(logEl);
        this.state.lineCount = logEl.children.length;
        this.renderLineCount();
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
        if (isSystem) {
            lineEl.classList.add('line-system', 'line-system-msg');
            if (!checked('showSystem')) {
                lineEl.classList.add('hidden');
            }
        }
        if (line.indexOf('ERROR') >= 0 || line.indexOf('Exception') >= 0 || line.indexOf('异常') >= 0) {
            lineEl.classList.add('line-error');
        }
        if (line.indexOf('连接') >= 0 && line.indexOf('失败') < 0) {
            lineEl.classList.add('line-ok');
        }
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
        while (logEl.children.length > this.maxLines) {
            logEl.removeChild(logEl.firstChild);
        }
    }

    /**
     * 刷新行数显示。
     */
    renderLineCount() {
        el('lineCount').textContent = `${this.state.lineCount} 行`;
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
        const keywordSet = new Set();
        (rules || []).forEach(rule => {
            const keyword = rule && rule.data ? String(rule.data).trim().toLowerCase() : '';
            if (!keyword) {
                return;
            }
            keywordSet.add(keyword);
        });
        this.highlightKeywords = Array.from(keywordSet).sort((left, right) => right.length - left.length);
        this.refreshExistingHighlights();
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
        });
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
     * 判断当前是否处于连接中/监听中状态。
     *
     * @returns {boolean} true 表示连接活跃
     */
    isSocketActive() {
        return !!(this.state.ws
            && (this.state.ws.readyState === WebSocket.OPEN || this.state.ws.readyState === WebSocket.CONNECTING));
    }
}
