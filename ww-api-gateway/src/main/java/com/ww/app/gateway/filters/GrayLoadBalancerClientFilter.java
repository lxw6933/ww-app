package com.ww.app.gateway.filters;

import com.ww.app.common.exception.ApiException;
import com.ww.app.gateway.properties.ServerGrayProperties;
import com.ww.app.gateway.utils.GrayLoadBalancer;
import lombok.AllArgsConstructor;
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
 * @description:
 */
@Slf4j
@Component
@AllArgsConstructor
public class GrayLoadBalancerClientFilter implements GlobalFilter, Ordered {

    private final LoadBalancerClientFactory clientFactory;

    private final ServerGrayProperties serverGrayProperties;

    private final GatewayLoadBalancerProperties properties;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // 获取请求url
        URI url = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR);
        ServerWebExchangeUtils.addOriginalRequestUrl(exchange, url);
        if (url == null || "http".equalsIgnoreCase(url.getScheme())) {
            return chain.filter(exchange);
        }
        return doFilter(exchange, chain, url);
    }

    private Mono<Response<ServiceInstance>> choose(ServerWebExchange exchange) {
        URI uri = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR);
        if (uri == null) {
            throw new ApiException(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR + " is null");
        }
        String serviceId = uri.getHost();
        GrayLoadBalancer loadBalancer = new GrayLoadBalancer(serviceId, serverGrayProperties,
                clientFactory.getLazyProvider(serviceId, ServiceInstanceListSupplier.class));
        return loadBalancer.choose(new DefaultRequest<>(exchange.getRequest().getHeaders()));
    }

    private Mono<Void> doFilter(ServerWebExchange exchange, GatewayFilterChain chain, URI url) {
        return this.choose(exchange).doOnNext(response -> {
            if (!response.hasServer()) {
                throw NotFoundException.create(properties.isUse404(), "Unable to find instance for ".concat(url.getHost()));
            }
            // 获取服务响应实例
            ServiceInstance retrievedInstance = response.getServer();
            URI uri = exchange.getRequest().getURI();
            String overrideScheme = retrievedInstance.isSecure() ? "https" : "http";
            DelegatingServiceInstance delegatingServiceInstance = new DelegatingServiceInstance(retrievedInstance, overrideScheme);
            URI reqUrl = LoadBalancerUriTools.reconstructURI(delegatingServiceInstance, uri);
            if (log.isDebugEnabled()) {
                log.debug("GrayLoadBalancerClientFilter url chosen: {}", reqUrl.toString());
            }
            exchange.getAttributes().put(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR, reqUrl);
            exchange.getAttributes().put(ServerWebExchangeUtils.GATEWAY_LOADBALANCER_RESPONSE_ATTR, response);
        }).then(chain.filter(exchange));
    }

    @Override
    public int getOrder() {
        return ReactiveLoadBalancerClientFilter.LOAD_BALANCER_CLIENT_FILTER_ORDER;
    }
}
