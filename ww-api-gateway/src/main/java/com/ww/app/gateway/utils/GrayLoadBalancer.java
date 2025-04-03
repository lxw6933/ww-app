package com.ww.app.gateway.utils;

import cn.hutool.core.collection.CollUtil;
import com.alibaba.cloud.nacos.balancer.NacosBalancer;
import com.ww.app.common.constant.Constant;
import com.ww.app.common.utils.IpUtil;
import com.ww.app.gateway.properties.ServerGrayProperties;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

import java.util.List;
import java.util.function.Predicate;

import static com.ww.app.common.utils.CollectionUtils.filterList;

/**
 * @author ww
 * @create 2023-07-31- 16:57
 * @description: 灰度发布负载均衡策略
 * 基于请求头中的灰度标记和版本信息进行服务实例的选择
 */
@Slf4j
@RequiredArgsConstructor
public class GrayLoadBalancer implements ReactorServiceInstanceLoadBalancer {

    private static final String VERSION_METADATA_KEY = "version";
    
    /**
     * 服务id
     */
    private final String serviceId;

    /**
     * 灰度自定义属性
     */
    private final ServerGrayProperties serverGrayProperties;

    /**
     * 服务id下所有的实例
     */
    private final ObjectProvider<ServiceInstanceListSupplier> serviceInstanceListSupplierProvider;

    @Override
    public Mono<Response<ServiceInstance>> choose(Request request) {
        if (!(request.getContext() instanceof HttpHeaders)) {
            log.warn("Request context is not HttpHeaders for service: {}", this.serviceId);
            return Mono.just(new EmptyResponse());
        }
        // 获得 HttpHeaders 属性，实现从 header 中获取 version
        HttpHeaders headers = (HttpHeaders) request.getContext();
        // 获取服务实例列表
        return getServiceInstanceList().map(instances -> getInstanceResponse(instances, headers));
    }
    
    /**
     * 获取服务实例列表
     *
     * @return 服务实例列表的Mono
     */
    private Mono<List<ServiceInstance>> getServiceInstanceList() {
        ServiceInstanceListSupplier supplier = this.serviceInstanceListSupplierProvider
                .getIfAvailable(NoopServiceInstanceListSupplier::new);
        return supplier.get().next();
    }

    /**
     * 根据灰度信息选择合适的服务实例
     *
     * @param instances 服务实例列表
     * @param headers 请求头
     * @return 服务实例响应
     */
    private Response<ServiceInstance> getInstanceResponse(List<ServiceInstance> instances, HttpHeaders headers) {
        // 如果服务实例为空，则直接返回
        if (CollUtil.isEmpty(instances)) {
            log.warn("No servers available for service: {}", this.serviceId);
            return new EmptyResponse();
        }
        // 提取灰度信息
        GrayInfo grayInfo = extractGrayInfo(headers);
        // 根据灰度信息选择服务实例
        List<ServiceInstance> selectedInstances = selectInstances(instances, grayInfo);
        // 如果没有找到匹配的实例，记录警告并使用原始实例列表
        if (CollUtil.isEmpty(selectedInstances)) {
            log.warn("[服务{}没有满足版本{}的{}服务实例]", serviceId, grayInfo.getVersion(), grayInfo.isGrayRequest() ? "灰度" : "生产");
            selectedInstances = instances;
        }
        // 使用nacos balancer按权重随机选择一个实例
        ServiceInstance selectedInstance = NacosBalancer.getHostByRandomWeight3(selectedInstances);
        if (selectedInstance != null) {
            log.debug("选择服务实例: {} (version={})", selectedInstance.getInstanceId(), selectedInstance.getMetadata().getOrDefault(VERSION_METADATA_KEY, "unknown"));
            return new DefaultResponse(selectedInstance);
        }
        return new EmptyResponse();
    }
    
    /**
     * 从请求头中提取灰度信息
     *
     * @param headers 请求头
     * @return 灰度信息对象
     */
    private GrayInfo extractGrayInfo(HttpHeaders headers) {
        String requestIp = StringUtils.defaultIfBlank(headers.getFirst(Constant.USER_REAL_IP), IpUtil.UNKNOWN);
        String requestGrayTag = StringUtils.defaultIfBlank(headers.getFirst(Constant.GRAY_TAG), Constant.ERROR);
        String grayVersion = StringUtils.defaultIfBlank(headers.getFirst(Constant.GRAY_VERSION), Constant.ERROR);
        // 前端灰度请求标识或后台配置灰度ip
        boolean isGrayRequest = Constant.GRAY_TAG_VALUE.equals(requestGrayTag);
        if (!isGrayRequest) {
            // 判断是否为后端配置的灰度ip
            if (CollUtil.isNotEmpty(serverGrayProperties.getGrayIps())) {
                try{
                    IpUtil.validIpStr(serverGrayProperties.getGrayIps(), requestIp);
                    isGrayRequest = true;
                } catch (Exception e) {
                    if (log.isDebugEnabled()) {
                        log.debug("非灰度ip请求");
                    }
                }
            }
        }
        return new GrayInfo(isGrayRequest, grayVersion);
    }
    
    /**
     * 根据灰度信息选择服务实例
     *
     * @param instances 服务实例列表
     * @param grayInfo 灰度信息
     * @return 过滤后的服务实例列表
     */
    private List<ServiceInstance> selectInstances(List<ServiceInstance> instances, GrayInfo grayInfo) {
        Predicate<ServiceInstance> filter;
        if (grayInfo.isGrayRequest()) {
            // 灰度请求 - 选择匹配灰度版本的实例
            filter = instance -> grayInfo.getVersion().equals(instance.getMetadata().get(VERSION_METADATA_KEY));
        } else {
            // 正常请求 - 选择非灰度版本的实例
            filter = instance -> !grayInfo.getVersion().equals(instance.getMetadata().get(VERSION_METADATA_KEY));
        }
        return filterList(instances, filter);
    }
    
    /**
     * 灰度信息封装类
     */
    @Data
    private static class GrayInfo {
        private final boolean isGrayRequest;
        @Getter
        private final String version;
        
        public GrayInfo(boolean isGrayRequest, String version) {
            this.isGrayRequest = isGrayRequest;
            this.version = version;
        }

        public boolean isGrayRequest() {
            return isGrayRequest;
        }

    }
}
