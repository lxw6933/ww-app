package com.ww.app.web.config.sentinel;

import com.alibaba.csp.sentinel.cluster.ClusterStateManager;
import com.alibaba.csp.sentinel.cluster.client.config.ClusterClientAssignConfig;
import com.alibaba.csp.sentinel.cluster.client.config.ClusterClientConfig;
import com.alibaba.csp.sentinel.cluster.client.config.ClusterClientConfigManager;
import com.alibaba.csp.sentinel.cluster.flow.rule.ClusterFlowRuleManager;
import com.alibaba.csp.sentinel.cluster.flow.rule.ClusterParamFlowRuleManager;
import com.alibaba.csp.sentinel.cluster.server.config.ClusterServerConfigManager;
import com.alibaba.csp.sentinel.cluster.server.config.ServerTransportConfig;
import com.alibaba.csp.sentinel.datasource.ReadableDataSource;
import com.alibaba.csp.sentinel.datasource.nacos.NacosDataSource;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowRule;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.alibaba.nacos.api.PropertyKeyConst;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Properties;
import java.util.Set;

/**
 * @author ww
 * @create 2024-08-13- 15:02
 * @description: sentinel cluster Embedded mode
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "ww.sentinel.cluster.enabled", havingValue = "true", matchIfMissing = true)
public class SentinelClusterConfiguration {

    @Value("${ww.sentinel.cluster.nacos.host}")
    private String nacosHost;
    @Value("${ww.sentinel.cluster.nacos.namespace}")
    private String nacosNamespace;
    @Value("${ww.sentinel.cluster.nacos.username}")
    private String nacosUsername;
    @Value("${ww.sentinel.cluster.nacos.password}")
    private String nacosPassword;

    private final static String FLOW_POSTFIX = "-sentinel-flow.json";

    private final static String PARAM_FLOW_POSTFIX = "-sentinel-param-flow.json";

    private final static String SENTINEL_GROUP = "SENTINEL_RULE";

    private final static String SENTINEL_CLUSTER_GROUP = "SENTINEL_CLUSTER_GROUP";

    /**
     * 配置 Sentinel 集群 server 端管理的 namespace 集合，即 server 负责哪些业务命名空间的流控
     * [
     *   "ww-order",
     *   "ww-seckill"
     * ]
     */
    @Value("${ww.sentinel.cluster.server.namespace-set:sentinel-cluster-namespace-set.json}")
    private String sentinelNamespaceSet;

    /**
     * 配置 Sentinel 集群 server 端自身的参数，如 server 监听的端口、token server 的最大连接数等
     * {
     *   "port": 18700,
     *   "idleSeconds": 600,
     *   "bindIp": "0.0.0.0"
     * }
     */
    @Value("${ww.sentinel.cluster.server.cluster-server:sentinel-cluster-server-config.json}")
    private String serverTransportConfig;

    /**
     * 配置 Sentinel 集群 client 端与 server 通信的参数，如请求超时时间、重试次数等
     * {
     *   "requestTimeout": 2000,      // client请求server的超时时间（毫秒）
     *   "idleSeconds": 600           // client空闲多少秒后自动断开与server的连接
     * }
     */
    @Value("${ww.sentinel.cluster.client.dataId:sentinel-cluster-client-config.json}")
    private String sentinelClusterClientConfig;

    /**
     * 配置 Sentinel 集群 client 端要连接的 server 地址，即告诉 client 该去哪个 server 获取 token
     * {
     *   "serverHost": "192.168.1.100",
     *   "serverPort": 18700
     * }
     */
    @Value("${ww.sentinel.cluster.client.token-server.dataId:sentinel-cluster-token-server-config.json}")
    private String sentinelClusterTokenServerConfig;

    @Value("${ww.sentinel.cluster.role}")
    private String clusterRole;

    @PostConstruct
    public void init() {
        if ("server".equalsIgnoreCase(clusterRole)) {
            log.info("-----------初始化加载Sentinel cluster server!!!!");
            // 仅server模式下注册namespace和传输配置
            registerServerNamespaceDatasource();
            registerServerTransportDataSource();
            // server端也需要注册规则
            registerClusterRuleSupplier();
            // 设置为server角色
            ClusterStateManager.applyState(ClusterStateManager.CLUSTER_SERVER);
        } else {
            log.info("-----------初始化加载Sentinel cluster client!!!!");
            // client模式下注册规则
            registerClusterRuleSupplier();
            // 配置客户端连接服务端的超时时间
            initClientConfigProperty();
            // 配置服务端（token server）的连接，例如：ip、port等
            initClientServerAssignProperty();
            // 设置为client角色
            ClusterStateManager.applyState(ClusterStateManager.CLUSTER_CLIENT);
        }
    }

    /**
     * 客户端与服务端通讯的配置：请求超时时间
     */
    private void initClientConfigProperty() {
        ReadableDataSource<String, ClusterClientConfig> clientConfigDataSource = new NacosDataSource<>(
                this.buildProperties(),
                SENTINEL_CLUSTER_GROUP,
                sentinelClusterClientConfig,
                source -> JSON.parseObject(source, new TypeReference<ClusterClientConfig>() {
                })
        );
        log.info("【sentinel cluster】刷新客户端与服务端通讯的配置,请求超时时间：{}", clientConfigDataSource.getProperty());
        ClusterClientConfigManager.registerClientConfigProperty(clientConfigDataSource.getProperty());
    }

    /**
     * 配置token server的连接地址
     */
    private void initClientServerAssignProperty() {
        ReadableDataSource<String, ClusterClientAssignConfig> clientAssignConfigDataSource = new NacosDataSource<>(
                this.buildProperties(),
                SENTINEL_CLUSTER_GROUP,
                sentinelClusterTokenServerConfig,
                source -> JSON.parseObject(source, new TypeReference<ClusterClientAssignConfig>() {
                })
        );
        log.info("【sentinel cluster】刷新客户端链接服务端的链接地址：{}", clientAssignConfigDataSource.getProperty());
        ClusterClientConfigManager.registerServerAssignProperty(clientAssignConfigDataSource.getProperty());
    }

    /**
     * 加载动态流控规则
     */
    public void registerClusterRuleSupplier() {
        // 注册流控规则
        ClusterFlowRuleManager.setPropertySupplier(namespace -> {
            ReadableDataSource<String, List<FlowRule>> flowRuleDataSource = new NacosDataSource<>(
                    this.buildProperties(),
                    SENTINEL_GROUP,
                    namespace + FLOW_POSTFIX,
                    source -> JSON.parseObject(source, new TypeReference<List<FlowRule>>() {
                    })
            );
            log.info("【sentinel cluster】刷新cluster flowRule：{}", flowRuleDataSource.getProperty());
            return flowRuleDataSource.getProperty();
        });
        // 注册热点流控规则
        ClusterParamFlowRuleManager.setPropertySupplier(namespace -> {
            ReadableDataSource<String, List<ParamFlowRule>> paramFlowRuleDataSource = new NacosDataSource<>(
                    this.buildProperties(),
                    SENTINEL_GROUP,
                    namespace + PARAM_FLOW_POSTFIX,
                    source -> JSON.parseObject(source, new TypeReference<List<ParamFlowRule>>() {
                    })
            );
            log.info("【sentinel cluster】刷新cluster paramFlowRule：{}", paramFlowRuleDataSource.getProperty());
            return paramFlowRuleDataSource.getProperty();
        });
    }

    /**
     * 注册namespace集合
     */
    public void registerServerNamespaceDatasource() {
        ReadableDataSource<String, Set<String>> namespaceSetDataSource = new NacosDataSource<>(
                this.buildProperties(),
                SENTINEL_CLUSTER_GROUP,
                sentinelNamespaceSet,
                source -> JSON.parseObject(source, new TypeReference<Set<String>>() {
                })
        );
        log.info("【sentinel cluster】刷新namespace集合：{}", namespaceSetDataSource.getProperty());
        ClusterServerConfigManager.registerNamespaceSetProperty(namespaceSetDataSource.getProperty());
    }

    /**
     * 注册服务端传输配置
     */
    public void registerServerTransportDataSource() {
        ReadableDataSource<String, ServerTransportConfig> transportConfigDataSource = new NacosDataSource<>(
                this.buildProperties(),
                SENTINEL_CLUSTER_GROUP,
                serverTransportConfig,
                source -> JSON.parseObject(source, new TypeReference<ServerTransportConfig>() {
                })
        );
        log.info("【sentinel cluster】刷新服务端传输配置：{}", transportConfigDataSource.getProperty());
        ClusterServerConfigManager.registerServerTransportProperty(transportConfigDataSource.getProperty());
    }

    /**
     * 该方法构造nacos的地址、命名空间、账号、密码等，因为我是匿名的，所以只需要两个地址和命名空间
     */
    private Properties buildProperties() {
        Properties properties = new Properties();
        properties.setProperty(PropertyKeyConst.SERVER_ADDR, nacosHost);
        properties.setProperty(PropertyKeyConst.NAMESPACE, nacosNamespace);
        properties.setProperty(PropertyKeyConst.USERNAME, nacosUsername);
        properties.setProperty(PropertyKeyConst.PASSWORD, nacosPassword);
        return properties;
    }

}
