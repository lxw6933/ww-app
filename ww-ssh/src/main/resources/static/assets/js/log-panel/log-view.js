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
        pauseBtn.textContent = this.state.paused ? '继续' : '暂停';
        if (!this.state.paused && this.state.buffer.length > 0) {
            this.appendLines(this.state.buffer.join(''));
            this.state.buffer = [];
        }
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
            const lineEl = document.createElement('div');
            lineEl.textContent = line;
            this.applyLineClass(lineEl, line);
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
        el('log').textContent = '';
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
        const lineEl = document.createElement('div');
        lineEl.textContent = '---------------- 手动换行 ----------------';
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
        if (line.indexOf('ERROR') >= 0 || line.indexOf('Exception') >= 0) {
            lineEl.classList.add('line-error');
        }
        if (line.indexOf('连接') >= 0 && line.indexOf('失败') < 0) {
            lineEl.classList.add('line-ok');
        }
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
}
