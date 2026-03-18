package com.ww.app.ssh.model;

import lombok.Data;

/**
 * 主机指标活跃维度注册请求。
 * <p>
 * 前端在展示服务器指标或 JVM 监控前先登记当前正在浏览的
 * “项目 + 环境 + 服务”维度，后端定时任务仅刷新这些活跃维度的指标快照。
 * </p>
 */
@Data
public class MetricsActiveRegisterRequest {

    /**
     * 指标面板来源。
     */
    public static final String SOURCE_PANEL = "panel";

    /**
     * JVM 监控来源。
     */
    public static final String SOURCE_JVM = "jvm";

    /**
     * 项目名称。
     */
    private String project;

    /**
     * 环境名称。
     */
    private String env;

    /**
     * 服务名称。
     */
    private String service;

    /**
     * 注册来源。
     * <p>
     * 不同来源对应不同的后端刷新频率：
     * 1. panel：左侧主机指标面板；<br>
     * 2. jvm：JVM 监控视图。<br>
     * </p>
     */
    private String source;

    /**
     * 获取规范化后的项目值。
     *
     * @return 项目值，null 时返回空字符串
     */
    public String normalizedProject() {
        return trimToEmpty(project);
    }

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
     * 获取规范化后的来源值。
     *
     * @return 来源值，不合法时回退为 panel
     */
    public String normalizedSource() {
        String normalized = trimToEmpty(source).toLowerCase();
        if (SOURCE_JVM.equals(normalized)) {
            return SOURCE_JVM;
        }
        return SOURCE_PANEL;
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
