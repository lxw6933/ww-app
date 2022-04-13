package com.ww.mall.gateway.filters;

import io.netty.buffer.ByteBufAllocator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.NettyDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.server.HandlerStrategies;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * @description:
 * 1：在缓存Body时：
 *    不能够在Filter内部直接进行缓存，需要按照响应式的处理方式，在异步操作路途上进行缓存Body，
 *    由于Body只能读取一次，所以要读取完成后要重新封装新的request和exchange才能保证请求正常传递到下游
 * 2：在缓存FormData时：
 *    FormData也只能读取一次，所以在读取完毕后，需要重新封装request和exchange,这里要注意，
 *    如果对FormData内容进行了修改，则必须重新定义Header中的content-length已保证传输数据的大小一致
 * @author: ww
 * @create: 2021/7/4 下午2:00
 **/
@Slf4j
@Component
public class MyGlobalGatewayFilter implements GlobalFilter, Ordered {

    /**
     * default HttpMessageReader
     */
    private static final List<HttpMessageReader<?>> messageReaders = HandlerStrategies.withDefaults().messageReaders();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        log.info("网关全局过滤器执行:" + request);
        // 将request信息保存到GatewayContext中
        String path = request.getPath().pathWithinApplication().value();
        GatewayContext gatewayContext = new GatewayContext();
        gatewayContext.setPath(path);
        // 将GatewayContext保存到exchange中
        exchange.getAttributes().put(GatewayContext.CACHE_GATEWAY_CONTEXT, gatewayContext);
        HttpHeaders headers = request.getHeaders();
        MediaType contentType = headers.getContentType();
        log.info("start-------------------------------------------------");
        log.info("HttpMethod:{},Url:{}", request.getMethod(), request.getURI().getRawPath());
        if (request.getMethod() == HttpMethod.GET) {
            log.info("---------------------get----------------------------");
        } else if (request.getMethod() == HttpMethod.POST) {
            if (MediaType.APPLICATION_JSON.equals(contentType)) {
                // 缓存body信息
                return readBody(exchange, chain, gatewayContext);
            } else if (MediaType.APPLICATION_FORM_URLENCODED.equals(contentType)) {
                // 缓存表单信息
                return readFormData(exchange, chain, gatewayContext);
            }
        }
        return chain.filter(exchange);
    }

    private Mono<Void> readFormData(ServerWebExchange exchange, GatewayFilterChain chain, GatewayContext gatewayContext) {
        final ServerHttpRequest request = exchange.getRequest();
        HttpHeaders headers = request.getHeaders();
        return exchange.getFormData().doOnNext(multiValueMap -> {
                gatewayContext.setFormData(multiValueMap);
                log.info("Post x-www-form-urlencoded:{}", multiValueMap);
                log.info("end-------------------------------------------------");
                }).then(Mono.defer(() -> {
                    Charset charset = headers.getContentType().getCharset() == null ? StandardCharsets.UTF_8 : headers.getContentType().getCharset();
                    String charsetName = charset.name();
                    MultiValueMap<String, String> formData = gatewayContext.getFormData();
                    // formData is empty just return
                    if (null == formData || formData.isEmpty()) {
                        return chain.filter(exchange);
                    }
                    StringBuilder formDataBodyBuilder = new StringBuilder();
                    try {
                        // repackage form data
                        for (Map.Entry<String, List<String>> entry : formData.entrySet()) {
                            List<String> entryValue = entry.getValue();
                            if (entryValue.size() > 1) {
                                for (String value : entryValue) {
                                    formDataBodyBuilder.append(entry.getKey()).append("=").append(URLEncoder.encode(value, charsetName)).append("&");
                                }
                            } else {
                                formDataBodyBuilder.append(entry.getKey()).append("=").append(URLEncoder.encode(entryValue.get(0), charsetName)).append("&");
                            }
                        }
                    } catch (UnsupportedEncodingException e) {
                        // ignore URLEncode Exception
                    }
                    // substring with the last char '&'
                    String formDataBodyString = "";
                    if (formDataBodyBuilder.length() > 0) {
                        formDataBodyString = formDataBodyBuilder.substring(0, formDataBodyBuilder.length() - 1);
                    }
                    // get data bytes
                    byte[] bodyBytes = formDataBodyString.getBytes(charset);
                    int contentLength = bodyBytes.length;
                    ServerHttpRequestDecorator decorator = new ServerHttpRequestDecorator(request) {
                                // change content-length
                                @Override
                                public HttpHeaders getHeaders() {
                                    HttpHeaders httpHeaders = new HttpHeaders();
                                    httpHeaders.putAll(super.getHeaders());
                                    if (contentLength > 0) {
                                        httpHeaders.setContentLength(contentLength);
                                    } else {
                                        httpHeaders.set(HttpHeaders.TRANSFER_ENCODING, "chunked");
                                    }
                                    return httpHeaders;
                                }
                                // read bytes to Flux<Databuffer>
                                @Override
                                public Flux<DataBuffer> getBody() {
                                    return DataBufferUtils.read(new ByteArrayResource(bodyBytes), new NettyDataBufferFactory(ByteBufAllocator.DEFAULT), contentLength);
                                }
                            };
                    ServerWebExchange mutateExchange = exchange.mutate().request(decorator).build();
                    return chain.filter(mutateExchange);
                })
        );
    }

    private Mono<Void> readBody(ServerWebExchange exchange, GatewayFilterChain chain, GatewayContext gatewayContext) {
        return DataBufferUtils.join(exchange.getRequest().getBody()).flatMap(dataBuffer -> {
            /*
             * read the body Flux<DataBuffer>, and release the buffer
             * //TODO when SpringCloudGateway Version Release To G.SR2,this can be update with the new version's feature
             * see PR https://github.com/spring-cloud/spring-cloud-gateway/pull/1095
             */
            byte[] bytes = new byte[dataBuffer.readableByteCount()];
            dataBuffer.read(bytes);
            DataBufferUtils.release(dataBuffer);
            Flux<DataBuffer> cachedFlux = Flux.defer(() -> {
                DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
                DataBufferUtils.retain(buffer);
                return Mono.just(buffer);
            });
            // repackage ServerHttpRequest
            ServerHttpRequest mutatedRequest = new ServerHttpRequestDecorator(exchange.getRequest()) {
                @Override
                public Flux<DataBuffer> getBody() {
                    return cachedFlux;
                }
            };
            // mutate exchage with new ServerHttpRequest
            ServerWebExchange mutatedExchange = exchange.mutate().request(mutatedRequest).build();
            // read body string with default messageReaders
            return ServerRequest.create(mutatedExchange, messageReaders).bodyToMono(String.class).doOnNext(objectValue -> {
                        log.info("PostBody:{}", objectValue);
                        log.info("end-------------------------------------------------");
                        gatewayContext.setCacheBody(objectValue);
            }).then(chain.filter(mutatedExchange));
        });
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
