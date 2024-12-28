package com.ww.mall.gateway.sentinel;

import com.alibaba.cloud.sentinel.datasource.config.NacosDataSourceProperties;
import com.alibaba.csp.sentinel.datasource.Converter;
import com.alibaba.csp.sentinel.datasource.WritableDataSource;
import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.PropertyKeyConst;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.exception.NacosException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.Properties;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author ww
 * @create 2023-07-20- 11:26
 * @description: sentinel dashboard 规则配置写入 nacos
 */
@Slf4j
public class NacosWritableDataSource<T> implements WritableDataSource<T> {

    /**
     * 每种规则配置的convert
     */
    private final Converter<T, String> configEncoder;
    /**
     * nacos配置
     */
    private final NacosDataSourceProperties nacosDataSourceProperties;
    /**
     * nacos配置操作对象
     */
    private ConfigService configService;

    private final Lock lock = new ReentrantLock(true);

    public NacosWritableDataSource(NacosDataSourceProperties nacosDataSourceProperties, Converter<T, String> configEncoder) {
        if (configEncoder == null) {
            throw new IllegalArgumentException("Config encoder cannot be null");
        }
        if (nacosDataSourceProperties == null) {
            throw new IllegalArgumentException("Config nacosDataSourceProperties cannot be null");
        }
        this.configEncoder = configEncoder;
        this.nacosDataSourceProperties = nacosDataSourceProperties;
        final Properties properties = buildProperties(nacosDataSourceProperties);
        try {
            // 也可以直接注入NacosDataSource，然后反射获取其configService属性
            this.configService = NacosFactory.createConfigService(properties);
        } catch (NacosException e) {
            log.error("create configService failed.", e);
        }
    }

    private Properties buildProperties(NacosDataSourceProperties nacosDataSourceProperties) {
        Properties properties = new Properties();
        if (!StringUtils.isEmpty(nacosDataSourceProperties.getServerAddr())) {
            properties.setProperty(PropertyKeyConst.SERVER_ADDR, nacosDataSourceProperties.getServerAddr());
        } else {
            properties.setProperty(PropertyKeyConst.ACCESS_KEY, nacosDataSourceProperties.getAccessKey());
            properties.setProperty(PropertyKeyConst.SECRET_KEY, nacosDataSourceProperties.getSecretKey());
            properties.setProperty(PropertyKeyConst.ENDPOINT, nacosDataSourceProperties.getEndpoint());
        }
        if (!StringUtils.isEmpty(nacosDataSourceProperties.getNamespace())) {
            properties.setProperty(PropertyKeyConst.NAMESPACE, nacosDataSourceProperties.getNamespace());
        }
        if (!StringUtils.isEmpty(nacosDataSourceProperties.getUsername())) {
            properties.setProperty(PropertyKeyConst.USERNAME, nacosDataSourceProperties.getUsername());
        }
        if (!StringUtils.isEmpty(nacosDataSourceProperties.getPassword())) {
            properties.setProperty(PropertyKeyConst.PASSWORD, nacosDataSourceProperties.getPassword());
        }
        return properties;
    }

    @Override
    public void write(T value) throws Exception {
        lock.lock();
        // todo handle cluster concurrent problem
        try {
            String convertResult = configEncoder.convert(value);
            if (configService == null) {
                log.error("configServer is null, can not continue.");
                return;
            }
            // 将sentinel dashborad配置的规则写入nacos对应的配置文件
            boolean published = configService.publishConfig(nacosDataSourceProperties.getDataId(), nacosDataSourceProperties.getGroupId(), convertResult);
            if (!published) {
                log.error("sentinel {} publish to nacos failed.", nacosDataSourceProperties.getRuleType());
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void close() {
        // do nothing
    }

}
