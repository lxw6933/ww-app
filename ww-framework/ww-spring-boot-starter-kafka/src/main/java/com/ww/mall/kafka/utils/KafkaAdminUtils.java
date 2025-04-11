package com.ww.mall.kafka.utils;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Kafka管理工具类
 */
public class KafkaAdminUtils implements FactoryBean<AdminClient>, DisposableBean {
    
    private final String bootstrapServers;
    private AdminClient adminClient;
    
    /**
     * 构造方法
     * 
     * @param bootstrapServers Kafka服务器地址
     */
    public KafkaAdminUtils(String bootstrapServers) {
        this.bootstrapServers = bootstrapServers;
    }
    
    /**
     * 创建AdminClient
     * 
     * @param bootstrapServers Kafka服务器地址
     * @return AdminClient实例
     */
    public static AdminClient createAdminClient(String bootstrapServers) {
        Properties props = new Properties();
        props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, "30000");
        return AdminClient.create(props);
    }
    
    /**
     * 创建AdminClient
     * 
     * @param configs 配置
     * @return AdminClient实例
     */
    public static AdminClient createAdminClient(Map<String, Object> configs) {
        return AdminClient.create(configs);
    }
    
    /**
     * 创建管理员客户端配置
     * 
     * @param bootstrapServers Kafka服务器地址
     * @return 配置Map
     */
    public static Map<String, Object> createAdminClientConfig(String bootstrapServers) {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configs.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, "30000");
        return configs;
    }
    
    @Override
    public AdminClient getObject() {
        if (adminClient == null) {
            adminClient = createAdminClient(bootstrapServers);
        }
        return adminClient;
    }
    
    @Override
    public Class<?> getObjectType() {
        return AdminClient.class;
    }
    
    @Override
    public boolean isSingleton() {
        return true;
    }
    
    @Override
    public void destroy() {
        if (adminClient != null) {
            adminClient.close();
        }
    }
} 