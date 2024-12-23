package com.ww.mall.web.config;

import cn.hutool.core.collection.CollUtil;
import com.alibaba.cloud.nacos.balancer.NacosBalancer;
import com.ww.mall.common.constant.Constant;
import com.ww.mall.common.exception.ApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.*;
import org.springframework.cloud.loadbalancer.core.NoopServiceInstanceListSupplier;
import org.springframework.cloud.loadbalancer.core.ReactorServiceInstanceLoadBalancer;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.http.HttpHeaders;
import reactor.core.publisher.Mono;

import java.net.UnknownHostException;
import java.util.List;

import static com.ww.mall.common.utils.CollectionUtils.filterList;

/**
 * @author ww
 * @create 2024-11-15- 15:16
 * @description:
 */
@Slf4j
@RequiredArgsConstructor
public class GrayFeignLoadBalancer implements ReactorServiceInstanceLoadBalancer {
    /**
     * 服务id
     */
    private final String serviceId;

    /**
     * 服务id下所有的实例
     */
    private final ObjectProvider<ServiceInstanceListSupplier> serviceInstanceListSupplierProvider;

    @Override
    public Mono<Response<ServiceInstance>> choose(Request request) {
        // 获得 HttpHeaders 属性，实现从 header 中获取 version
        RequestDataContext context = (RequestDataContext) request.getContext();
        HttpHeaders headers = context.getClientRequest().getHeaders();
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
        // 请求是否包含灰度标记
        String requestGrayTag = StringUtils.defaultIfBlank(headers.getFirst(Constant.GRAY_TAG), "error");
        // 获取配置的灰度版本
        String grayVersion = StringUtils.defaultIfBlank(headers.getFirst(Constant.GRAY_VERSION), "error");
        // 是否灰度请求
        boolean grayFlag = Constant.GRAY_TAG_VALUE.equals(requestGrayTag);

        List<ServiceInstance> chooseInstances;
        if (!grayFlag) {
            // 正常返回生产版本实例
            chooseInstances = filterList(instances, instance -> !grayVersion.equals(instance.getMetadata().get("version")));
        } else {
            // 获取灰度版本实例【实例meta里获取版本号、灰度用户列表对比】
            chooseInstances = filterList(instances, instance -> grayVersion.equals(instance.getMetadata().get("version")));
            // 没有灰度实例，正常访问
            if (CollUtil.isEmpty(chooseInstances)) {
                log.warn("[serviceId({}) 没有满足版本({})的灰度服务实例列表]", serviceId, grayVersion);
                chooseInstances = instances;
            }
        }
        String serverIp = StringUtils.defaultIfBlank(headers.getFirst(Constant.SERVER_IP), null);
        if (StringUtils.isNotBlank(serverIp)) {
            chooseInstances = filterList(chooseInstances, res -> res.getHost().equals(serverIp));
        }
        if (CollectionUtils.isEmpty(chooseInstances)) {
            throw new ApiException("服务不可用");
        }
        // 随机 + 权重获取实例列表
        return new DefaultResponse(NacosBalancer.getHostByRandomWeight3(chooseInstances));
    }
}
