package com.ww.mall.web.config.sentinel;

import com.alibaba.csp.sentinel.cluster.ClusterStateManager;
import com.alibaba.csp.sentinel.cluster.client.config.ClusterClientAssignConfig;
import com.alibaba.csp.sentinel.cluster.client.config.ClusterClientConfig;
import com.alibaba.csp.sentinel.cluster.client.config.ClusterClientConfigManager;
import com.alibaba.csp.sentinel.datasource.ReadableDataSource;
import com.alibaba.csp.sentinel.datasource.nacos.NacosDataSource;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.alibaba.nacos.api.PropertyKeyConst;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.util.Properties;

/**
 * @author ww
 * @create 2024-08-13- 15:02
 * @description:
 */
@Configuration
public class SentinelClusterClientConfiguration {

    @Value("${sentinel.nacos.address:127.0.0.1:8848}")
    private  String nacosAddress;

    @Value("${sentinel.nacos.namespace:zshop-api-sentinel}")
    private  String nacosNamespace;

    @Value("${sentinel.nacos.client.group:CLUSTER_CLIENT_GROUP}")
    private String clusterClientGroup;

    @Value("${sentinel.nacos.client.dataId:cluster-client-dataId}")
    private String clusterClientDataId;

    @Value("${sentinel.nacos.client.token-server.dataId:token-server-config-dataId}")
    private String tokenServerConfigDataId;

    @PostConstruct
    public void init() {
        System.out.println("-----------初始化加载Sentinel cluster服务端!!!!");
        // 初始化角色为客户端
        initClusterRole();
        // 配置客户端连接服务端的超时时间
        initClientConfigProperty();
        // 配置服务端（token server）的连接，例如：ip、port等
        initClientServerAssignProperty();
    }

    /**
     * 初始化角色为客户端
     */
    public void initClusterRole() {
        ClusterStateManager.applyState(ClusterStateManager.CLUSTER_CLIENT);
    }

    /**
     * 客户端与服务端通讯的配置：请求超时时间
     */
    private void initClientConfigProperty() {
        ReadableDataSource<String, ClusterClientConfig> clientConfigDs = new NacosDataSource<>(
                this.buildProperties(),
                clusterClientGroup,
                clusterClientDataId,
                source -> JSON.parseObject(source, new TypeReference<ClusterClientConfig>() {})
        );
        ClusterClientConfigManager.registerClientConfigProperty(clientConfigDs.getProperty());
    }

    /**
     * 配置token server的连接地址
     */
    private void initClientServerAssignProperty() {
        ReadableDataSource<String, ClusterClientAssignConfig> clientAssignDs = new NacosDataSource<>(
            this.buildProperties(),
            clusterClientGroup,
            tokenServerConfigDataId,
            source -> JSON.parseObject(source, new TypeReference<ClusterClientAssignConfig>() {})
        );
        ClusterClientConfigManager.registerServerAssignProperty(clientAssignDs.getProperty());
    }

    /**
     * 该方法构造nacos的地址、命名空间、账号、密码等，因为我是匿名的，所以只需要两个地址和命名空间
     */
    private Properties buildProperties() {
        Properties properties = new Properties();
        properties.setProperty(PropertyKeyConst.SERVER_ADDR, nacosAddress);
        properties.setProperty(PropertyKeyConst.NAMESPACE, nacosNamespace);
        return properties;
    }

}
