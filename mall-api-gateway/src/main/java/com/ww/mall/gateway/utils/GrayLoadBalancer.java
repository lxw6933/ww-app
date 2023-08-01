package com.ww.mall.gateway.utils;

import cn.hutool.core.collection.CollUtil;
import com.alibaba.cloud.nacos.balancer.NacosBalancer;
import com.ww.mall.gateway.config.ServerGrayProperty;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.DefaultResponse;
import org.springframework.cloud.client.loadbalancer.EmptyResponse;
import org.springframework.cloud.client.loadbalancer.Request;
import org.springframework.cloud.client.loadbalancer.Response;
import org.springframework.cloud.loadbalancer.core.NoopServiceInstanceListSupplier;
import org.springframework.cloud.loadbalancer.core.ReactorServiceInstanceLoadBalancer;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.http.HttpHeaders;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * @author ww
 * @create 2023-07-31- 16:57
 * @description: 灰度发布负载均衡策略
 */
@Slf4j
@RequiredArgsConstructor
public class GrayLoadBalancer implements ReactorServiceInstanceLoadBalancer {

    private final ObjectProvider<ServiceInstanceListSupplier> serviceInstanceListSupplierProvider;

    private final String serviceId;

    private ServerGrayProperty serverGrayProperty;

    @Override
    public Mono<Response<ServiceInstance>> choose(Request request) {
        // 获得 HttpHeaders 属性，实现从 header 中获取 version
        HttpHeaders headers = (HttpHeaders) request.getContext();
        // 选择实例
        ServiceInstanceListSupplier supplier =
                this.serviceInstanceListSupplierProvider.getIfAvailable(NoopServiceInstanceListSupplier::new);
        return supplier.get().next().map(item -> getInstanceResponse(item, headers));
    }

    private Response<ServiceInstance> getInstanceResponse(List<ServiceInstance> instances, HttpHeaders headers) {
        // 如果服务实例为空，则直接返回
        if (instances.isEmpty()) {
            log.warn("No servers available for service: {}", this.serviceId);
            return new EmptyResponse();
        }
        // 获取配置的灰度版本
        String version = serverGrayProperty.getGrayVersion();
        // 获取配置的灰度用户列表
        List<String> grayUsers = serverGrayProperty.getGrayUsers();
        // todo 这里获取可以根据等候的信息获取
        String userId = Objects.requireNonNull(headers.get("userId")).stream().findFirst().orElse("");

        List<ServiceInstance> chooseInstances;
        if (StringUtils.isEmpty(version) && !grayUsers.contains(userId)) {
            // 如果没配置灰度版本，且没配置灰度用户列表【正常返回所有实例】
            chooseInstances = instances;
        } else {
            // 获取灰度版本实例【实例meta里获取版本号、灰度用户列表对比】
            chooseInstances = filterList(instances, instance ->
                    version.equals(instance.getMetadata().get("version")) && grayUsers.contains(userId)
            );
            // 没有灰度实例，正常访问
            if (CollUtil.isEmpty(chooseInstances)) {
                log.warn("[serviceId({}) 没有满足版本({})的灰度服务实例列表]", serviceId, version);
                chooseInstances = instances;
            }
        }
        // 基于 tag 过滤实例列表
//        chooseInstances = filterTagServiceInstances(chooseInstances, headers);
        // 随机 + 权重获取实例列表
        return new DefaultResponse(NacosBalancer.getHostByRandomWeight3(chooseInstances));
    }

    public static <T> List<T> filterList(Collection<T> from, Predicate<T> predicate) {
        if (CollUtil.isEmpty(from)) {
            return new ArrayList<>();
        }
        return from.stream().filter(predicate).collect(Collectors.toList());
    }

    /**
     * 基于 tag 请求头，过滤匹配 tag 的服务实例列表
     *
//     * @param instances 服务实例列表
//     * @param headers   请求头
     * @return 服务实例列表
     */
//    private List<ServiceInstance> filterTagServiceInstances(List<ServiceInstance> instances, HttpHeaders headers) {
//        // 情况一，没有 tag 时，直接返回
//        String tag = EnvUtils.getTag(headers);
//        if (StringUtils.isEmpty(tag)) {
//            return instances;
//        }
//        // 有 tag 时，使用 tag 匹配服务实例
//        List<ServiceInstance> chooseInstances = filterList(instances, instance -> tag.equals(EnvUtils.getTag(instance)));
//        if (CollectionUtils.isEmpty(chooseInstances)) {
//            log.warn("[serviceId({}) 没有满足 tag({}) 的服务实例列表]", serviceId, tag);
//            chooseInstances = instances;
//        }
//        return chooseInstances;
//    }

    public void setServerGrayProperty(ServerGrayProperty serverGrayProperty) {
        this.serverGrayProperty = serverGrayProperty;
    }

}
