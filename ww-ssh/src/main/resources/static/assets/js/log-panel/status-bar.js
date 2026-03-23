import {el} from './dom.js';

/**
 * 状态栏控制器。
 * <p>
 * 负责维护“最后日志时间、延迟、重连倒计时”并定时刷新展示。
 * </p>
 */
export class StatusBarController {

    /**
     * 构造方法。
     */
    constructor() {
        this.lastLogTimestamp = null;
        this.reconnectSeconds = null;
        this.latencyEnabled = false;
        this.timer = null;
        this.tickMs = 1000;
    }

    /**
     * 启动状态栏定时刷新。
     */
    start() {
        this.stop();
        this.render();
        this.timer = window.setInterval(() => this.render(), this.tickMs);
    }

    /**
     * 停止状态栏定时刷新。
     */
    stop() {
        if (this.timer) {
            window.clearInterval(this.timer);
            this.timer = null;
        }
    }

    /**
     * 记录收到日志的时间。
     */
    markLogReceived() {
        this.lastLogTimestamp = Date.now();
        this.render();
    }

    /**
     * 重置最近日志时间。
     */
    resetLastLogTime() {
        this.lastLogTimestamp = null;
        this.render();
    }

    /**
     * 设置重连倒计时。
     *
     * @param {number|null} seconds 秒数
     */
    setReconnectSeconds(seconds) {
        this.reconnectSeconds = (seconds === null || seconds === undefined) ? null : Number(seconds);
        this.render();
    }

    /**
     * 设置是否启用延迟计算展示。
     * <p>
     * 仅在 tail 实时监听连接活跃时启用，其他状态统一显示“--”。
     * </p>
     *
     * @param {boolean} enabled 是否启用延迟计算
     */
    setLatencyEnabled(enabled) {
        this.latencyEnabled = !!enabled;
        this.renderLatency();
    }

    /**
     * 更新查看模式文案。
     *
     * @param {boolean} aggregate 是否为聚合模式
     * @param {string} aggregateLabel 聚合模式文案
     * @param {string} singleLabel 单项模式文案
     */
    setModeTip(aggregate, aggregateLabel, singleLabel) {
        const modeTipEl = el('modeTip');
        if (!modeTipEl) {
            return;
        }
        const aggregateText = aggregateLabel || '全部目标查看';
        const singleText = singleLabel || '单目标查看';
        modeTipEl.textContent = aggregate ? `当前：${aggregateText}` : `当前：${singleText}`;
    }

    /**
     * 执行状态栏渲染。
     */
    render() {
        this.renderLastLogTime();
        this.renderLatency();
        this.renderReconnect();
    }

    /**
     * 渲染最后日志时间。
     */
    renderLastLogTime() {
        const target = el('lastLogTime');
        if (!target) {
            return;
        }
        if (!this.latencyEnabled || !this.lastLogTimestamp) {
            target.textContent = '最后日志: --';
            return;
        }
        target.textContent = `最后日志: ${formatTime(this.lastLogTimestamp)}`;
    }

    /**
     * 渲染延迟文本。
     */
    renderLatency() {
        const target = el('wsLatency');
        if (!target) {
            return;
        }
        if (!this.latencyEnabled || !this.lastLogTimestamp) {
            target.textContent = '延迟: --';
            return;
        }
        const diffSec = Math.max(0, Math.floor((Date.now() - this.lastLogTimestamp) / 1000));
        target.textContent = `延迟: ${diffSec}s`;
    }

    /**
     * 渲染重连倒计时。
     */
    renderReconnect() {
        const target = el('reconnectIn');
        if (!target) {
            return;
        }
        if (this.reconnectSeconds === null || this.reconnectSeconds === undefined) {
            target.textContent = '重连: --';
            return;
        }
        target.textContent = `重连: ${this.reconnectSeconds}s`;
    }
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
