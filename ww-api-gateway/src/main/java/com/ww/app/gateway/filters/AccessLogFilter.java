package com.ww.app.gateway.filters;

import cn.hutool.core.date.LocalDateTimeUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.json.JSONUtil;
import com.alibaba.nacos.common.utils.StringUtils;
import com.ww.app.common.constant.Constant;
import com.ww.app.common.utils.TracerUtils;
import com.ww.app.gateway.log.AccessLog;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.factory.rewrite.CachedBodyOutputMessage;
import org.springframework.cloud.gateway.filter.factory.rewrite.ModifyRequestBodyGatewayFilterFactory;
import org.springframework.cloud.gateway.support.BodyInserterContext;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ReactiveHttpOutputMessage;
import org.springframework.http.codec.CodecConfigurer;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static cn.hutool.core.date.DatePattern.NORM_DATETIME_MS_FORMATTER;

/**
 * @author ww
 * @create 2024-09-23- 09:21
 * @description:
 */
@Slf4j
@Component
public class AccessLogFilter implements GlobalFilter, Ordered {

    private final DefaultDataBufferFactory dataBufferFactory = new DefaultDataBufferFactory(true);

    @Resource
    private CodecConfigurer codecConfigurer;

    /**
     * 打印日志
     *
     * @param gatewayLog 网关日志
     */
    private void writeAccessLog(AccessLog gatewayLog) {
        // 打印到控制台，方便排查错误
        Map<String, Object> values = MapUtil.newHashMap(15, true);
        values.put("routeId", gatewayLog.getRoute() != null ? gatewayLog.getRoute().getId() : null);
        values.put("schema", gatewayLog.getSchema());
        values.put("requestUrl", gatewayLog.getRequestUrl());
        values.put("queryParams", gatewayLog.getQueryParams() != null ? 
                gatewayLog.getQueryParams().toSingleValueMap() : null);
        values.put("requestBody", gatewayLog.getRequestBody() != null && JSONUtil.isTypeJSON(gatewayLog.getRequestBody()) ?
                JSONUtil.parse(gatewayLog.getRequestBody()) : gatewayLog.getRequestBody());
        values.put("requestHeaders", gatewayLog.getRequestHeaders() != null ? 
                JSONUtil.toJsonStr(gatewayLog.getRequestHeaders().toSingleValueMap()) : null);
        values.put("userIp", gatewayLog.getUserIp());
        values.put("responseBody", gatewayLog.getResponseBody() != null && JSONUtil.isTypeJSON(gatewayLog.getResponseBody()) ?
                JSONUtil.parse(gatewayLog.getResponseBody()) : gatewayLog.getResponseBody());
        values.put("responseHeaders", gatewayLog.getResponseHeaders() != null ?
                JSONUtil.toJsonStr(gatewayLog.getResponseHeaders().toSingleValueMap()) : null);
        values.put("httpStatus", gatewayLog.getHttpStatus());
        values.put("startTime", gatewayLog.getStartTime() != null ? 
                LocalDateTimeUtil.format(gatewayLog.getStartTime(), NORM_DATETIME_MS_FORMATTER) : null);
        values.put("endTime", gatewayLog.getEndTime() != null ? 
                LocalDateTimeUtil.format(gatewayLog.getEndTime(), NORM_DATETIME_MS_FORMATTER) : null);
        values.put("duration", gatewayLog.getDuration() != null ? gatewayLog.getDuration() + " ms" : null);
        
        // 生产环境使用非格式化输出以提高性能
        boolean isPretty = !isProdEnvironment();
        String logContent = isPretty ? JSONUtil.toJsonPrettyStr(values) : JSONUtil.toJsonStr(values);
        log.info("[writeAccessLog][网关日志：{}]", logContent);
    }

    private boolean isProdEnvironment() {
        // 根据实际配置判断是否为生产环境
        return System.getProperty("spring.profiles.active", "").contains("prod");
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        // 记录请求数据
        AccessLog gatewayLog = new AccessLog();
        gatewayLog.setRoute(exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR));
        gatewayLog.setSchema(request.getURI().getScheme());
        gatewayLog.setRequestMethod(request.getMethodValue());
        gatewayLog.setRequestUrl(request.getURI().getRawPath());
        gatewayLog.setQueryParams(request.getQueryParams());
        gatewayLog.setRequestHeaders(request.getHeaders());
        gatewayLog.setStartTime(LocalDateTime.now());
        gatewayLog.setUserIp(request.getHeaders().getFirst(Constant.USER_REAL_IP));
        gatewayLog.setTraceId(TracerUtils.getTraceId());

        MediaType mediaType = request.getHeaders().getContentType();
        if (MediaType.APPLICATION_FORM_URLENCODED.isCompatibleWith(mediaType)
                || MediaType.APPLICATION_JSON.isCompatibleWith(mediaType)) {
            return filterWithRequestBody(exchange, chain, gatewayLog);
        }
        return filterWithoutRequestBody(exchange, chain, gatewayLog);
    }

    private Mono<Void> filterWithoutRequestBody(ServerWebExchange exchange, GatewayFilterChain chain, AccessLog accessLog) {
        // 包装 Response，用于记录 Response Body
        ServerHttpResponseDecorator decoratedResponse = recordResponseLog(exchange, accessLog);
        return chain.filter(exchange.mutate().response(decoratedResponse).build())
                // 工作线程打印日志
                .then(Mono.fromRunnable(() -> writeAccessLog(accessLog)));
    }

    /**
     * 参考 {@link ModifyRequestBodyGatewayFilterFactory} 实现
     * 差别主要在于使用 modifiedBody 来读取 Request Body 数据
     */
    private Mono<Void> filterWithRequestBody(ServerWebExchange exchange, GatewayFilterChain chain, AccessLog gatewayLog) {
        // 设置 Request Body 读取时，设置到网关日志
        // 此处 codecConfigurer.getReaders() 的目的，是解决 spring.codec.max-in-memory-size 不生效
        ServerRequest serverRequest = ServerRequest.create(exchange, codecConfigurer.getReaders());
        Mono<String> modifiedBody = serverRequest.bodyToMono(String.class).flatMap(body -> {
            gatewayLog.setRequestBody(body);
            return Mono.just(body);
        });

        // 创建 BodyInserter 对象
        BodyInserter<Mono<String>, ReactiveHttpOutputMessage> bodyInserter = BodyInserters.fromPublisher(modifiedBody, String.class);
        // 创建 CachedBodyOutputMessage 对象
        HttpHeaders headers = new HttpHeaders();
        headers.putAll(exchange.getRequest().getHeaders());
        // the new content type will be computed by bodyInserter
        // and then set in the request decorator
        headers.remove(HttpHeaders.CONTENT_LENGTH);
        CachedBodyOutputMessage outputMessage = new CachedBodyOutputMessage(exchange, headers);
        // 通过 BodyInserter 将 Request Body 写入到 CachedBodyOutputMessage 中
        return bodyInserter.insert(outputMessage, new BodyInserterContext()).then(Mono.defer(() -> {
            // 包装 Request，用于缓存 Request Body
            ServerHttpRequest decoratedRequest = requestDecorate(exchange, headers, outputMessage);
            // 包装 Response，用于记录 Response Body
            ServerHttpResponseDecorator decoratedResponse = recordResponseLog(exchange, gatewayLog);
            // 记录普通的
            return chain.filter(exchange.mutate().request(decoratedRequest).response(decoratedResponse).build())
                    // 工作线程打印日志
                    .then(Mono.fromRunnable(() -> writeAccessLog(gatewayLog)));

        }));
    }

    /**
     * 记录响应日志
     * 通过 DataBufferFactory 解决响应体分段传输问题。
     */
    private ServerHttpResponseDecorator recordResponseLog(ServerWebExchange exchange, AccessLog gatewayLog) {
        ServerHttpResponse response = exchange.getResponse();
        return new ServerHttpResponseDecorator(response) {

            @NonNull
            @Override
            public Mono<Void> writeWith(@NonNull Publisher<? extends DataBuffer> body) {
                if (body instanceof Flux) {
                    DataBufferFactory bufferFactory = response.bufferFactory();
                    // 计算执行时间
                    gatewayLog.setEndTime(LocalDateTime.now());
                    gatewayLog.setDuration((int) (LocalDateTimeUtil.between(gatewayLog.getStartTime(), gatewayLog.getEndTime()).toMillis()));
                    // 设置其它字段
                    gatewayLog.setResponseHeaders(response.getHeaders());
                    gatewayLog.setHttpStatus(response.getStatusCode());

                    // 获取响应类型，如果是 json 就打印
                    String originalResponseContentType = exchange.getAttribute(ServerWebExchangeUtils.ORIGINAL_RESPONSE_CONTENT_TYPE_ATTR);
                    if (StringUtils.isNotBlank(originalResponseContentType) && originalResponseContentType.contains("application/json")) {
                        Flux<? extends DataBuffer> fluxBody = Flux.from(body);
                        return super.writeWith(fluxBody.buffer().map(dataBuffers -> {
                            try {
                                // 设置 response body 到网关日志
                                byte[] content = readContent(dataBuffers);
                                // 限制响应体大小，防止内存溢出
                                String responseResult = content.length > 1024 * 1024 ? 
                                    "[Response body too large to log]" : new String(content, StandardCharsets.UTF_8);
                                gatewayLog.setResponseBody(responseResult);
                                // 响应
                                return bufferFactory.wrap(content);
                            } catch (Exception e) {
                                log.error("Failed to read response body", e);
                                gatewayLog.setResponseBody("[Error reading response body]");
                                // 确保即使处理失败，也能返回原始内容
                                return dataBufferFactory.join(dataBuffers);
                            }
                        }));
                    } else {
                        // 对于非JSON响应，记录响应类型但不记录内容
                        gatewayLog.setResponseBody("[Non-JSON response: " + originalResponseContentType + "]");
                    }
                }
                // if body is not a flux. never got there.
                return super.writeWith(body);
            }

            @NonNull
            @Override
            public Mono<Void> writeAndFlushWith(@NonNull Publisher<? extends Publisher<? extends DataBuffer>> body) {
                // 对于流式响应，简单记录类型
                gatewayLog.setResponseBody("[Streaming response]");
                return super.writeAndFlushWith(body);
            }
        };
    }

    // ========== 参考 ModifyRequestBodyGatewayFilterFactory 中的方法 ==========

    /**
     * 请求装饰器，支持重新计算 headers、body 缓存
     *
     * @param exchange 请求
     * @param headers 请求头
     * @param outputMessage body 缓存
     * @return 请求装饰器
     */
    private ServerHttpRequestDecorator requestDecorate(ServerWebExchange exchange, HttpHeaders headers, CachedBodyOutputMessage outputMessage) {
        return new ServerHttpRequestDecorator(exchange.getRequest()) {

            @NonNull
            @Override
            public HttpHeaders getHeaders() {
                long contentLength = headers.getContentLength();
                HttpHeaders httpHeaders = new HttpHeaders();
                httpHeaders.putAll(super.getHeaders());
                if (contentLength > 0) {
                    httpHeaders.setContentLength(contentLength);
                } else {
                    // TODO: this causes a 'HTTP/1.1 411 Length Required'
                    httpHeaders.set(HttpHeaders.TRANSFER_ENCODING, "chunked");
                }
                return httpHeaders;
            }

            @NonNull
            @Override
            public Flux<DataBuffer> getBody() {
                return outputMessage.getBody();
            }
        };
    }

    // ========== 参考 ModifyResponseBodyGatewayFilterFactory 中的方法 ==========

    private byte[] readContent(List<? extends DataBuffer> dataBuffers) {
        // 合并多个流集合，解决返回体分段传输
        DataBuffer join = dataBufferFactory.join(dataBuffers);
        byte[] content = new byte[join.readableByteCount()];
        join.read(content);
        // 释放掉内存
        DataBufferUtils.release(join);
        return content;
    }

}
