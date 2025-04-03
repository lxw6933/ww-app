package com.ww.app.gateway.filters;

import com.ww.app.common.exception.ApiException;
import com.ww.app.gateway.properties.ServerGrayProperties;
import com.ww.app.gateway.utils.GrayLoadBalancer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.DefaultRequest;
import org.springframework.cloud.client.loadbalancer.LoadBalancerUriTools;
import org.springframework.cloud.client.loadbalancer.Response;
import org.springframework.cloud.gateway.config.GatewayLoadBalancerProperties;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.ReactiveLoadBalancerClientFilter;
import org.springframework.cloud.gateway.support.DelegatingServiceInstance;
import org.springframework.cloud.gateway.support.NotFoundException;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;

/**
 * @author ww
 * @create 2023-07-31- 16:52
 * @description: 灰度负载均衡过滤器 用于Gateway网关的灰度路由服务选择
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GrayLoadBalancerClientFilter implements GlobalFilter, Ordered {

    private final LoadBalancerClientFactory clientFactory;
    private final ServerGrayProperties serverGrayProperties;
    private final GatewayLoadBalancerProperties properties;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        URI url = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR);
        // 记录原始请求URL
        if (url != null) {
            ServerWebExchangeUtils.addOriginalRequestUrl(exchange, url);
        }
        // 判断是否需要进行负载均衡
        if (url == null || !"lb".equals(url.getScheme())) {
            return chain.filter(exchange);
        }
        log.debug("灰度负载均衡开始处理请求: {}", url);
        return chooseInstance(exchange)
                .doOnNext(response -> processLoadBalancerResponse(exchange, response, url))
                .then(chain.filter(exchange));
    }

    /**
     * 选择服务实例
     * 
     * @param exchange 服务交换对象
     * @return Response对象的Mono
     */
    private Mono<Response<ServiceInstance>> chooseInstance(ServerWebExchange exchange) {
        URI uri = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR);
        if (uri == null) {
            return Mono.error(new ApiException(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR + " 属性不存在"));
        }
        
        String serviceId = uri.getHost();
        if (!serverGrayProperties.getEnable()) {
            log.debug("灰度负载均衡功能已禁用，使用默认负载均衡策略");
            return Mono.empty();
        }
        log.debug("使用灰度负载均衡选择服务[{}]实例", serviceId);
        GrayLoadBalancer loadBalancer = createGrayLoadBalancer(serviceId);
        return loadBalancer.choose(new DefaultRequest<>(exchange.getRequest().getHeaders()));
    }
    
    /**
     * 创建灰度负载均衡器
     * 
     * @param serviceId 服务ID
     * @return 灰度负载均衡器
     */
    private GrayLoadBalancer createGrayLoadBalancer(String serviceId) {
        return new GrayLoadBalancer(
                serviceId,
                serverGrayProperties,
                clientFactory.getLazyProvider(serviceId, ServiceInstanceListSupplier.class)
        );
    }
    
    /**
     * 处理负载均衡响应
     * 
     * @param exchange 服务交换对象
     * @param response 负载均衡响应
     * @param originalUrl 原始URL
     */
    private void processLoadBalancerResponse(ServerWebExchange exchange, Response<ServiceInstance> response, URI originalUrl) {
        if (!response.hasServer()) {
            String message = "无法找到服务[" + originalUrl.getHost() + "]的可用实例";
            if (properties.isUse404()) {
                throw new NotFoundException(message);
            }
            throw new ApiException(message);
        }
        // 获取服务响应实例
        ServiceInstance instance = response.getServer();
        // 重建请求URI
        URI reconstructedUri = reconstructUri(instance, exchange.getRequest().getURI());
        if (log.isDebugEnabled()) {
            log.debug("灰度负载均衡选择实例: {}:{}, 重建URI: {}", instance.getHost(), instance.getPort(), reconstructedUri);
        }
        // 存储到exchange属性中
        exchange.getAttributes().put(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR, reconstructedUri);
        exchange.getAttributes().put(ServerWebExchangeUtils.GATEWAY_LOADBALANCER_RESPONSE_ATTR, response);
    }
    
    /**
     * 重建URI
     * 
     * @param instance 服务实例
     * @param originalUri 原始URI
     * @return 重建后的URI
     */
    private URI reconstructUri(ServiceInstance instance, URI originalUri) {
        String overrideScheme = instance.isSecure() ? "https" : "http";
        DelegatingServiceInstance delegatingInstance = new DelegatingServiceInstance(instance, overrideScheme);
        return LoadBalancerUriTools.reconstructURI(delegatingInstance, originalUri);
    }

    @Override
    public int getOrder() {
        return ReactiveLoadBalancerClientFilter.LOAD_BALANCER_CLIENT_FILTER_ORDER;
    }
}
