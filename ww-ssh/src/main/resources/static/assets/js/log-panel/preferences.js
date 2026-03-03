/**
 * 页面偏好存储键。
 */
const STORAGE_KEY = 'wwSshLogPanelPrefs.v1';

/**
 * 本地偏好存储器。
 * <p>
 * 用于记忆用户常用的环境、服务、行数与界面勾选状态，减少重复操作。
 * </p>
 */
export class PreferenceStore {

    /**
     * 构造方法。
     */
    constructor() {
        this.data = this.read();
    }

    /**
     * 读取本地存储。
     *
     * @returns {Object} 偏好对象
     */
    read() {
        try {
            const raw = window.localStorage.getItem(STORAGE_KEY);
            if (!raw) {
                return {};
            }
            const parsed = JSON.parse(raw);
            return parsed && typeof parsed === 'object' ? parsed : {};
        } catch (error) {
            return {};
        }
    }

    /**
     * 持久化当前偏好。
     */
    write() {
        try {
            window.localStorage.setItem(STORAGE_KEY, JSON.stringify(this.data));
        } catch (error) {
            // localStorage 可能被禁用，静默降级即可
        }
    }

    /**
     * 写入偏好项。
     *
     * @param {string} key 键
     * @param {*} value 值
     */
    set(key, value) {
        this.data[key] = value;
        this.write();
    }

    /**
     * 读取字符串偏好。
     *
     * @param {string} key 键
     * @param {string} fallback 默认值
     * @returns {string} 字符串
     */
    getString(key, fallback) {
        const value = this.data[key];
        if (value === null || value === undefined) {
            return fallback;
        }
        return String(value);
    }

    /**
     * 读取布尔偏好。
     *
     * @param {string} key 键
     * @param {boolean} fallback 默认值
     * @returns {boolean} 布尔值
     */
    getBoolean(key, fallback) {
        const value = this.data[key];
        if (value === null || value === undefined) {
            return fallback;
        }
        return !!value;
    }

    /**
     * 读取数值偏好。
     *
     * @param {string} key 键
     * @param {number} fallback 默认值
     * @returns {number} 数值
     */
    getNumber(key, fallback) {
        const value = Number(this.data[key]);
        if (Number.isNaN(value)) {
            return fallback;
        }
        return value;
    }
}
