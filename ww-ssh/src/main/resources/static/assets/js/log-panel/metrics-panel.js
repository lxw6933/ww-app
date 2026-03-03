import {el} from './dom.js';

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
     * @param {Function} options.getEnv 获取当前环境
     * @param {Function} options.getService 获取当前服务
     */
    constructor(options) {
        this.getEnv = options.getEnv;
        this.getService = options.getService;
        this.refreshMs = 15000;
        this.timer = null;
        this.token = 0;
    }

    /**
     * 初始化指标面板。
     */
    init() {
        this.renderEmpty('请选择环境与服务后查看服务器指标');
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
    }

    /**
     * 刷新指标数据。
     *
     * @param {boolean} manual 是否手动刷新
     */
    refresh(manual) {
        const env = this.getEnv();
        const service = this.getService();
        if (!env || !service) {
            this.renderEmpty('请选择环境与服务后查看服务器指标');
            return;
        }

        if (manual) {
            this.renderSummary({
                statusText: '刷新中',
                online: null,
                total: null,
                errorCount: null,
                avgCpu: null,
                avgMem: null,
                timeText: formatTime(Date.now())
            });
        }

        const currentToken = ++this.token;
        fetch(`/api/metrics/hosts?env=${encodeURIComponent(env)}&service=${encodeURIComponent(service)}`)
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
            })
            .catch(error => {
                if (currentToken !== this.token) {
                    return;
                }
                this.renderError(`指标加载失败：${error.message || '网络异常'}`);
            });
    }

    /**
     * 渲染空态。
     *
     * @param {string} text 文案
     */
    renderEmpty(text) {
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
        const listEl = el('metricsList');
        listEl.innerHTML = '';
        if (!list || list.length === 0) {
            this.renderEmpty('未查询到实例指标，请检查服务配置');
            return;
        }

        const total = list.length;
        const okList = list.filter(item => item && item.status === 'ok');
        const errorCount = total - okList.length;
        const avgCpu = average(okList.map(item => item.cpuUsagePercent));
        const avgMem = average(okList.map(item => item.memoryUsagePercent));

        this.renderSummary({
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
            titleEl.title = `${serviceText} @ ${hostText}`;

            const serviceNameEl = document.createElement('span');
            serviceNameEl.className = 'metric-service-name';
            serviceNameEl.textContent = serviceText;
            const hostNameEl = document.createElement('span');
            hostNameEl.className = 'metric-host-name';
            hostNameEl.textContent = `@ ${hostText}`;
            titleEl.appendChild(serviceNameEl);
            titleEl.appendChild(hostNameEl);

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

    const timeEl = document.createElement('span');
    timeEl.className = 'metric-load-time';
    timeEl.textContent = `采集时间 ${formatTime(item && item.updatedAt ? item.updatedAt : Date.now())}`;

    loadBlockEl.appendChild(loadTitleEl);
    loadBlockEl.appendChild(loadValueEl);
    loadBlockEl.appendChild(timeEl);
    return loadBlockEl;
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
        return '内存 --/--';
    }
    const used = formatStorage(item.memoryUsedMb);
    const total = formatStorage(item.memoryTotalMb);
    return `内存 ${used}/${total}`;
}

/**
 * 自动格式化容量单位（MB/GB）。
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
 * 风险文案映射。
 *
 * @param {string} level 级别
 * @returns {string} 文案
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
 * @returns {string} 时间文本
 */
function formatTime(timestamp) {
    const date = new Date(timestamp);
    const hh = String(date.getHours()).padStart(2, '0');
    const mm = String(date.getMinutes()).padStart(2, '0');
    const ss = String(date.getSeconds()).padStart(2, '0');
    return `${hh}:${mm}:${ss}`;
}
