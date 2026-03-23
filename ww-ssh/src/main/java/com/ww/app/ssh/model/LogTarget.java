package com.ww.app.ssh.model;

import com.ww.app.ssh.config.LogPanelProperties;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 日志订阅目标。
 * <p>
 * 将“项目 + 环境 + 服务 + 节点配置”打包为单一对象，
 * 供 SSH 日志读取服务消费。
 * </p>
 */
@Data
@AllArgsConstructor
public class LogTarget {

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
     * SSH 节点配置。
     */
    private LogPanelProperties.ServerNode serverNode;

    /**
     * 获取“项目/环境/服务”的展示标识。
     *
     * @return 形如 projectA/test/mall-basic 的标识
     */
    public String displayName() {
        return project + "/" + env + "/" + service;
    }

    /**
     * 获取目标类型。
     *
     * @return 目标类型
     */
    public String targetType() {
        if (serverNode == null) {
            return LogPanelProperties.TARGET_TYPE_APP;
        }
        return serverNode.normalizedTargetType();
    }

    /**
     * 判断当前目标是否支持 JVM 监控。
     *
     * @return true 表示支持 JVM 监控
     */
    public boolean supportsJvmMonitor() {
        return serverNode != null && serverNode.supportsJvmMonitor();
    }
}
