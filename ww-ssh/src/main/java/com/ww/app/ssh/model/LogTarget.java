package com.ww.app.ssh.model;

import com.ww.app.ssh.config.LogPanelProperties;
import lombok.Data;

/**
 * 日志订阅目标。
 * <p>
 * 将“项目 + 环境 + 服务 + 节点配置”打包为单一对象，
 * 供 SSH 日志读取服务消费。
 * </p>
 */
@Data
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
     * 当前项目环境下可用的中间件后台数量。
     * <p>
     * 该值来源于环境级共享配置，用于前端在实例卡片上决定是否展示“中间件”入口。
     * </p>
     */
    private Integer middlewareCount;

    /**
     * 构造方法。
     *
     * @param project 项目名称
     * @param env 环境名称
     * @param service 服务名称
     * @param serverNode 节点配置
     */
    public LogTarget(String project, String env, String service, LogPanelProperties.ServerNode serverNode) {
        this(project, env, service, serverNode, 0);
    }

    /**
     * 构造方法。
     *
     * @param project 项目名称
     * @param env 环境名称
     * @param service 服务名称
     * @param serverNode 节点配置
     * @param middlewareCount 环境中间件数量
     */
    public LogTarget(String project,
                     String env,
                     String service,
                     LogPanelProperties.ServerNode serverNode,
                     Integer middlewareCount) {
        this.project = project;
        this.env = env;
        this.service = service;
        this.serverNode = serverNode;
        this.middlewareCount = middlewareCount == null ? 0 : middlewareCount;
    }

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

    /**
     * 判断当前环境是否已配置中间件后台入口。
     *
     * @return true 表示已配置
     */
    public boolean supportsMiddleware() {
        return middlewareCount() > 0;
    }

    /**
     * 获取当前环境启用的中间件后台数量。
     *
     * @return 数量
     */
    public int middlewareCount() {
        return middlewareCount == null ? 0 : Math.max(middlewareCount, 0);
    }
}
