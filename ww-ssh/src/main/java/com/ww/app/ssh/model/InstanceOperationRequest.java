package com.ww.app.ssh.model;

import lombok.Data;

/**
 * 实例运维操作请求。
 * <p>
 * 用于描述“对哪个环境下的哪个实例执行何种动作”。
 * </p>
 */
@Data
public class InstanceOperationRequest {

    /**
     * 启动动作。
     */
    public static final String ACTION_START = "start";

    /**
     * 重启动作。
     */
    public static final String ACTION_RESTART = "restart";

    /**
     * 停止动作。
     */
    public static final String ACTION_STOP = "stop";

    /**
     * 目标环境。
     */
    private String env;

    /**
     * 目标实例服务键（需精确匹配配置 key）。
     */
    private String service;

    /**
     * 操作动作：start/restart/stop。
     */
    private String action;

    /**
     * 获取规范化后的环境值。
     *
     * @return 环境值
     */
    public String normalizedEnv() {
        return trimToEmpty(env);
    }

    /**
     * 获取规范化后的服务值。
     *
     * @return 服务值
     */
    public String normalizedService() {
        return trimToEmpty(service);
    }

    /**
     * 获取规范化后的动作值。
     *
     * @return 动作值（start/restart/stop），不合法返回空字符串
     */
    public String normalizedAction() {
        String normalized = trimToEmpty(action).toLowerCase();
        if (ACTION_START.equals(normalized)
                || ACTION_RESTART.equals(normalized)
                || ACTION_STOP.equals(normalized)) {
            return normalized;
        }
        return "";
    }

    /**
     * 字符串去空格并兜底空值。
     *
     * @param source 原始字符串
     * @return 非 null 字符串
     */
    private String trimToEmpty(String source) {
        return source == null ? "" : source.trim();
    }
}
