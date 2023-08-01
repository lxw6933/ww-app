package com.ww.mall.gateway.filters;

import com.ww.mall.common.exception.ApiException;
import com.ww.mall.gateway.config.ServerGrayProperty;
import com.ww.mall.gateway.utils.GrayLoadBalancer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.*;
import org.springframework.cloud.gateway.config.GatewayLoadBalancerProperties;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.ReactiveLoadBalancerClientFilter;
import org.springframework.cloud.gateway.support.DelegatingServiceInstance;
import org.springframework.cloud.gateway.support.NotFoundException;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.Map;
import java.util.Set;

/**
 * @author ww
 * @create 2023-07-31- 16:52
 * @description:
 */
@Slf4j
@Component
@AllArgsConstructor
@Order(ReactiveLoadBalancerClientFilter.LOAD_BALANCER_CLIENT_FILTER_ORDER)
public class GrayLoadBalancerClientFilter implements GlobalFilter {

    private final LoadBalancerClientFactory clientFactory;

    private final ServerGrayProperty serverGrayProperty;

    private final GatewayLoadBalancerProperties properties;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String grayPrefix = "grayLb";
        // 获取请求url
        URI url = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR);
        // 获取gateway动态路由前缀
        String schemePrefix = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_SCHEME_PREFIX_ATTR);
        // 将lb替换成grayLb表示灰度负载均衡
        boolean grayRequestFlag = url == null || (!grayPrefix.equals(url.getScheme()) && !grayPrefix.equals(schemePrefix));
        if (grayRequestFlag) {
            return chain.filter(exchange);
        }
        // preserve the original url
        ServerWebExchangeUtils.addOriginalRequestUrl(exchange, url);

        if (log.isTraceEnabled()) {
            log.trace(ReactiveLoadBalancerClientFilter.class.getSimpleName() + " url before: " + url);
        }

        URI requestUri = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR);
        if (requestUri == null) {
            throw new ApiException("requestUri is null");
        }
        String serviceId = requestUri.getHost();
        // 获取serviceId有效实例
        Set<LoadBalancerLifecycle> supportedLifecycleProcessors = LoadBalancerLifecycleValidator.getSupportedLifecycleProcessors(
                        clientFactory.getInstances(serviceId, LoadBalancerLifecycle.class),
                        RequestDataContext.class,
                        ResponseData.class,
                        ServiceInstance.class);
        // 获取gateway lb请求
        DefaultRequest<RequestDataContext> lbRequest =
                new DefaultRequest<>(new RequestDataContext(new RequestData(exchange.getRequest()), getHint(serviceId)));

        return this.choose(serviceId, lbRequest, supportedLifecycleProcessors).doOnNext(response -> {
                    if (!response.hasServer()) {
                        supportedLifecycleProcessors.forEach(lifecycle ->
                                lifecycle.onComplete(new CompletionContext<>(CompletionContext.Status.DISCARD, lbRequest, response))
                        );
                        throw NotFoundException.create(properties.isUse404(), "Unable to find instance for " + url.getHost());
                    }

                    ServiceInstance retrievedInstance = response.getServer();
                    URI uri = exchange.getRequest().getURI();
                    // if the `lb:<scheme>` mechanism was used, use `<scheme>` as the default,
                    // if the loadbalancer doesn't provide one.
                    String overrideScheme = retrievedInstance.isSecure() ? "https" : "http";
                    if (schemePrefix != null) {
                        overrideScheme = url.getScheme();
                    }

                    DelegatingServiceInstance serviceInstance = new DelegatingServiceInstance(retrievedInstance,
                            overrideScheme);

                    URI requestUrl = LoadBalancerUriTools.reconstructURI(serviceInstance, uri);

                    if (log.isTraceEnabled()) {
                        log.trace("LoadBalancerClientFilter url chosen: " + requestUrl);
                    }
                    exchange.getAttributes().put(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR, requestUrl);
                    exchange.getAttributes().put(ServerWebExchangeUtils.GATEWAY_LOADBALANCER_RESPONSE_ATTR, response);
                    supportedLifecycleProcessors.forEach(lifecycle -> lifecycle.onStartRequest(lbRequest, response));
                }).then(chain.filter(exchange))
                .doOnError(throwable -> supportedLifecycleProcessors.forEach(lifecycle ->
                        lifecycle.onComplete(new CompletionContext<ResponseData, ServiceInstance, RequestDataContext>(
                                CompletionContext.Status.FAILED, throwable, lbRequest,
                                exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_LOADBALANCER_RESPONSE_ATTR))
                        )
                    )
                ).doOnSuccess(aVoid -> supportedLifecycleProcessors.forEach(lifecycle ->
                        lifecycle.onComplete(new CompletionContext<ResponseData, ServiceInstance, RequestDataContext>(
                                CompletionContext.Status.SUCCESS, lbRequest,
                                exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_LOADBALANCER_RESPONSE_ATTR),
                                new ResponseData(exchange.getResponse(), new RequestData(exchange.getRequest())))
                        )
                    )
                );
    }

    private Mono<Response<ServiceInstance>> choose(String serviceId,
                                                   Request<RequestDataContext> lbRequest,
                                                   Set<LoadBalancerLifecycle> supportedLifecycleProcessors) {
        // new 灰度负载均衡器
        GrayLoadBalancer loadBalancer = new GrayLoadBalancer(serviceId, clientFactory.getLazyProvider(serviceId, ServiceInstanceListSupplier.class));
        if (serverGrayProperty.getEnable()) {
            loadBalancer.setServerGrayProperty(serverGrayProperty);
        }
        supportedLifecycleProcessors.forEach(lifecycle -> lifecycle.onStart(lbRequest));
        return loadBalancer.choose(lbRequest);
    }

    private String getHint(String serviceId) {
        LoadBalancerProperties loadBalancerProperties = clientFactory.getProperties(serviceId);
        Map<String, String> hints = loadBalancerProperties.getHint();
        String defaultHint = hints.getOrDefault("default", "default");
        String hintPropertyValue = hints.get(serviceId);
        return hintPropertyValue != null ? hintPropertyValue : defaultHint;
    }

}
