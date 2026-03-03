/**
 * 全部环境/服务的占位值。
 */
export const ALL = '__ALL__';

/**
 * 页面最大日志行数。
 */
export const MAX_LINES = 5000;

/**
 * 创建页面运行态。
 *
 * @returns {{config: Object, ws: WebSocket|null, wsToken: number, paused: boolean, buffer: Array, reconnectTimer: number|null, manualStop: boolean, lineCount: number, fileOptions: Array}}
 */
export function createState() {
    return {
        config: {},
        ws: null,
        wsToken: 0,
        paused: false,
        buffer: [],
        reconnectTimer: null,
        manualStop: false,
        lineCount: 0,
        fileOptions: []
    };
}
