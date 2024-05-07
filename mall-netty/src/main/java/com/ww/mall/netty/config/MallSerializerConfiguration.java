package com.ww.mall.netty.config;

import com.ww.mall.netty.properties.MallNettyProperties;
import com.ww.mall.netty.serializer.MallSerializer;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

/**
 * @author ww
 * @create 2024-05-06 22:29
 * @description:
 */
@Configuration
public class MallSerializerConfiguration implements InitializingBean, ApplicationContextAware {

    private ApplicationContext applicationContext;

    private final Map<Integer, MallSerializer> serializerMap = new HashMap<>();

    @Resource
    private MallNettyProperties mallNettyProperties;

    @Override
    public void afterPropertiesSet() throws Exception {
        Map<String, MallSerializer> targetMap = this.applicationContext.getBeansOfType(MallSerializer.class);
        for (Map.Entry<String, MallSerializer> entry : targetMap.entrySet()) {
            serializerMap.put(entry.getValue().type().type, entry.getValue());
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    public MallSerializer getSerializer() {
        return serializerMap.get(getDefaultSerializeType());
    }

    public int getDefaultSerializeType() {
        return mallNettyProperties.getSerializerType();
    }

}
