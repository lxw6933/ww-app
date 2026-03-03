package com.ww.app.ssh.model;

import com.ww.app.ssh.config.LogPanelProperties;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 日志订阅目标。
 * <p>
 * 将“环境 + 服务 + 节点配置”打包为单一对象，供 SSH 日志读取服务消费。
 * </p>
 */
@Data
@AllArgsConstructor
public class LogTarget {

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
     * 获取“环境/服务”的展示标识。
     *
     * @return 形如 TEST-1/mall-basic 的标识
     */
    public String displayName() {
        return env + "/" + service;
    }
}
