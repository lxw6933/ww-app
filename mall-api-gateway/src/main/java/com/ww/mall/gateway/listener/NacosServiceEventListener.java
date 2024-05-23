package com.ww.mall.gateway.listener;

import com.alibaba.fastjson.JSON;
import com.alibaba.nacos.client.naming.event.InstancesChangeEvent;
import com.alibaba.nacos.common.notify.NotifyCenter;
import com.alibaba.nacos.common.notify.listener.Subscriber;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import static org.springframework.cloud.loadbalancer.core.CachingServiceInstanceListSupplier.SERVICE_INSTANCE_CACHE_NAME;

/**
 * @author ww
 * @create 2024-05-23- 15:59
 * @description:
 */
@Slf4j
//@EnableCaching
//@Component
public class NacosServiceEventListener extends Subscriber<InstancesChangeEvent> {

    @Resource
    private CacheManager defaultLoadBalancerCacheManager;

    @PostConstruct
    public void registerToNotifyCenter(){
        NotifyCenter.registerSubscriber(this);
    }

    @Override
    public void onEvent(InstancesChangeEvent event) {
        log.info("接收微服务实例变更事件：{}", JSON.toJSONString(event));
        Cache cache = defaultLoadBalancerCacheManager.getCache(SERVICE_INSTANCE_CACHE_NAME);
        if (cache != null) {
            cache.evict(event.getServiceName());
        }
        log.info("实例刷新完成");
    }

    @Override
    public Class<? extends com.alibaba.nacos.common.notify.Event> subscribeType() {
        return InstancesChangeEvent.class;
    }
}
