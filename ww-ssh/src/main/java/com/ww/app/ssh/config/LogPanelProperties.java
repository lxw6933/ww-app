package com.ww.app.ssh.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 日志面板配置属性。
 * <p>
 * 该配置支持两种结构：
 * 1. 新结构：项目 -> 环境 -> 服务 -> 节点；
 * 2. 兼容结构：环境 -> 服务 -> 节点（会自动归并到默认项目）。
 * </p>
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "app")
public class LogPanelProperties {

    /**
     * 兼容旧结构时使用的默认项目名。
     */
    public static final String DEFAULT_PROJECT = "default";

    /**
     * 兼容旧版配置：环境 -> 服务 -> 节点。
     * <p>
     * 当未配置 {@link #projects} 时，系统会将该映射自动归并到
     * {@link #DEFAULT_PROJECT} 项目下，保证旧配置可继续运行。
     * </p>
     */
    private Map<String, Map<String, ServerNode>> servers = new LinkedHashMap<>();

    /**
     * 推荐配置结构：项目 -> 环境 -> 服务 -> 节点。
     * <p>
     * 第一层 key：项目名（如 ww-mall）；<br>
     * 第二层 key：环境名（如 test/uat）；<br>
     * 第三层 key：服务名（如 mall-basic）；<br>
     * value：SSH 节点连接信息及日志目录。
     * </p>
     */
    private Map<String, Map<String, Map<String, ServerNode>>> projects = new LinkedHashMap<>();

    /**
     * 单个服务节点的 SSH 与日志配置。
     */
    @Data
    public static class ServerNode {

        /**
         * SSH 主机地址。
         */
        private String host;

        /**
         * SSH 端口，默认 22。
         */
        private Integer port = 22;

        /**
         * SSH 登录用户名。
         */
        private String username;

        /**
         * SSH 登录密码。
         */
        private String password;

        /**
         * 私钥文件路径（可选）。
         */
        private String privateKeyPath;

        /**
         * 私钥口令（可选）。
         */
        private String privateKeyPassphrase;

        /**
         * 首选认证方式（可选）。
         */
        private String preferredAuthentications;

        /**
         * 连接超时时间（毫秒），默认 8000。
         */
        private Integer connectTimeoutMs = 8000;

        /**
         * 服务默认日志目录或日志文件路径。
         */
        private String logPath;

        /**
         * 实例启停管理命令配置（可选）。
         * <p>
         * 当配置该字段后，前端可对该实例执行“启动/重启/停止”操作。
         * 支持两类格式：
         * 1. 脚本路径：如 {@code /data/app/server.sh}；
         * 2. 命令前缀：如 {@code sh server.sh} 或 {@code bash /data/app/server.sh}。
         * 系统会在尾部自动追加动作参数（start/restart/stop/status）执行。
         * </p>
         */
        private String manageCommandFile;
    }
}
