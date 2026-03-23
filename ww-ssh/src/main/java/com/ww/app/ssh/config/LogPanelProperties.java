package com.ww.app.ssh.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
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
     * 目标类型：Java 应用服务。
     */
    public static final String TARGET_TYPE_APP = "app";

    /**
     * 目标类型：nginx。
     */
    public static final String TARGET_TYPE_NGINX = "nginx";

    /**
     * 单实例允许同时承载的实时日志流上限。
     * <p>
     * 每个被订阅的实例日志会占用 1 条实时流，
     * 例如“全部服务”订阅会按命中的实例数叠加占用。
     * 该值越大，对本机线程与 SSH 长连接的占用越高。
     * </p>
     */
    private Integer maxConcurrentStreams = 48;

    /**
     * 兼容旧版配置：环境 -> 服务 -> 节点。
     * <p>
     * 当未配置 {@link #projects} 时，系统会将该映射自动归并到
     * {@link #DEFAULT_PROJECT} 项目下，保证旧配置可继续运行。
     * </p>
     */
    private Map<String, Map<String, ServerNode>> servers = new LinkedHashMap<>();

    /**
     * 环境级共享中间件后台配置。
     * <p>
     * 推荐结构：{@code app.middlewares.<project>.<env>.<middlewareCode>}。<br>
     * 当同一环境下多个实例共用同一套中间件后台时，应优先使用该配置，避免在每个服务节点下重复维护。<br>
     * 例如：test 环境下共用的 Nacos、RabbitMQ、XXL-JOB 等后台入口。
     * </p>
     */
    private Map<String, Map<String, Map<String, MiddlewareConsole>>> middlewares = new LinkedHashMap<>();

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
         * 目标类型。
         * <p>
         * 当前支持：
         * 1. app：普通 Java 应用服务，默认支持 JVM 监控；<br>
         * 2. nginx：nginx 日志目标，不展示 JVM 监控入口。<br>
         * </p>
         */
        private String targetType = TARGET_TYPE_APP;

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

        /**
         * 当前实例挂载的中间件后台配置。
         * <p>
         * key 建议使用中间件编码，例如 {@code nacos}、{@code rabbitmq}、
         * {@code xxl-job}、{@code soul}；<br>
         * value 为该实例对应的后台访问地址与账号信息。<br>
         * 该配置主要用于测试环境下快速跳转到中间件后台，不参与 SSH 运维逻辑。
         * </p>
         */
        private Map<String, MiddlewareConsole> middlewares = new LinkedHashMap<>();

        /**
         * 获取规范化后的目标类型。
         *
         * @return 目标类型
         */
        public String normalizedTargetType() {
            String normalized = targetType == null ? "" : targetType.trim().toLowerCase();
            if (TARGET_TYPE_NGINX.equals(normalized)) {
                return TARGET_TYPE_NGINX;
            }
            return TARGET_TYPE_APP;
        }

        /**
         * 判断当前目标是否支持 JVM 监控。
         *
         * @return true 表示支持 JVM 监控
         */
        public boolean supportsJvmMonitor() {
            return TARGET_TYPE_APP.equals(normalizedTargetType());
        }

        /**
         * 判断当前实例是否已配置可用的中间件后台入口。
         *
         * @return true 表示至少存在一个启用的中间件后台配置
         */
        public boolean hasEnabledMiddlewares() {
            return middlewareCount() > 0;
        }

        /**
         * 统计当前实例已启用的中间件后台数量。
         *
         * @return 已启用的中间件后台数量
         */
        public int middlewareCount() {
            if (middlewares == null || middlewares.isEmpty()) {
                return 0;
            }
            int count = 0;
            for (MiddlewareConsole console : middlewares.values()) {
                if (console != null && console.isConsoleEnabled()) {
                    count++;
                }
            }
            return count;
        }

        /**
         * 获取按配置顺序排列的中间件后台项。
         * <p>
         * 该方法会过滤掉空配置与显式禁用的配置项，供接口层直接消费。
         * </p>
         *
         * @return 可用的中间件后台列表
         */
        public List<Map.Entry<String, MiddlewareConsole>> enabledMiddlewares() {
            List<Map.Entry<String, MiddlewareConsole>> result = new ArrayList<>();
            if (middlewares == null || middlewares.isEmpty()) {
                return result;
            }
            for (Map.Entry<String, MiddlewareConsole> entry : middlewares.entrySet()) {
                MiddlewareConsole console = entry.getValue();
                if (console == null || !console.isConsoleEnabled()) {
                    continue;
                }
                result.add(entry);
            }
            return result;
        }
    }

    /**
     * 单个中间件后台配置。
     */
    @Data
    public static class MiddlewareConsole {

        /**
         * 中间件展示名称。
         * <p>
         * 若未配置，则前端回退为配置 key。
         * </p>
         */
        private String name;

        /**
         * 中间件后台访问地址。
         */
        private String url;

        /**
         * 登录账号。
         */
        private String username;

        /**
         * 登录密码。
         */
        private String password;

        /**
         * 排序值，数值越小越靠前。
         */
        private Integer sort = 0;

        /**
         * 是否启用该中间件入口。
         */
        private Boolean enabled = true;

        /**
         * 判断当前中间件入口是否启用。
         *
         * @return true 表示启用
         */
        public boolean isConsoleEnabled() {
            return enabled == null || enabled;
        }
    }
}
