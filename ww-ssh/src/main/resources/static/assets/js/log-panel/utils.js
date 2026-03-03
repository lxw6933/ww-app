import {ALL} from './state.js';

/**
 * 解析回看行数并限制边界。
 *
 * @param {string} raw 输入值
 * @returns {number} 行数
 */
export function parseLines(raw) {
    const value = parseInt(raw || '200', 10);
    if (isNaN(value)) {
        return 200;
    }
    if (value < 10) {
        return 10;
    }
    if (value > 5000) {
        return 5000;
    }
    return value;
}

/**
 * 提取路径中的文件名。
 *
 * @param {string} path 文件路径
 * @returns {string} 文件名
 */
export function fileName(path) {
    const normalized = String(path || '').replace(/\\/g, '/');
    const index = normalized.lastIndexOf('/');
    return index < 0 ? normalized : normalized.substring(index + 1);
}

/**
 * 判断是否为聚合模式。
 *
 * @param {string} env 环境（保留参数仅用于兼容现有调用）
 * @param {string} service 服务
 * @returns {boolean} true 表示聚合模式
 */
export function isAggregateSelected(env, service) {
    return service === ALL;
}

/**
 * 汇总可用服务列表。
 *
 * @param {Object} config 配置对象
 * @param {string} env 当前环境
 * @returns {Array<string>} 服务列表
 */
export function collectServices(config, env) {
    if (env === ALL) {
        const serviceSet = new Set();
        Object.keys(config || {}).forEach(envName => {
            const services = config[envName] || {};
            Object.keys(services).forEach(service => serviceSet.add(service));
        });
        return Array.from(serviceSet).sort();
    }
    return Object.keys((config || {})[env] || {});
}

/**
 * 判断文件是否命中搜索关键字。
 *
 * @param {string} path 文件路径
 * @param {string} keyword 搜索关键字（已小写）
 * @returns {boolean} true 表示命中
 */
export function matchFileKeyword(path, keyword) {
    if (!keyword) {
        return true;
    }
    const normalizedPath = String(path || '').toLowerCase();
    const normalizedName = fileName(path).toLowerCase();
    return normalizedPath.indexOf(keyword) >= 0 || normalizedName.indexOf(keyword) >= 0;
}

/**
 * 按“info 优先 + 名称倒序”排序文件列表。
 *
 * @param {Array<string>} list 原列表
 * @returns {Array<string>} 排序后列表
 */
export function sortFileOptions(list) {
    return list.slice().sort((left, right) => {
        const leftPriority = filePriority(left);
        const rightPriority = filePriority(right);
        if (leftPriority !== rightPriority) {
            return leftPriority - rightPriority;
        }
        return String(right).localeCompare(String(left));
    });
}

/**
 * 计算文件优先级（数字越小优先级越高）。
 *
 * @param {string} path 文件路径
 * @returns {number} 优先级
 */
function filePriority(path) {
    const normalized = fileName(path).toLowerCase();
    return normalized.indexOf('info') >= 0 ? 0 : 1;
}
