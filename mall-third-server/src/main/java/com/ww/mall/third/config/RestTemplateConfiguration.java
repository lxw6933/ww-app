package com.ww.mall.third.config;

import com.ww.mall.third.constant.HttpProperties;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.client.loadbalancer.RetryLoadBalancerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.OkHttp3ClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author ww
 * @create 2023-07-21- 17:12
 * @description:
 */
@Configuration
public class RestTemplateConfiguration {

    /**
     * 注册RestTemplate
     *
     * @param okHttpClientHttpRequestFactory 工厂
     * @return RestTemplate
     */
    @Bean(name = "okRestTemplate")
    public RestTemplate extRestTemplate(ClientHttpRequestFactory okHttpClientHttpRequestFactory, ObjectProvider<List<ClientHttpRequestInterceptor>> requestInterceptor) {
        RestTemplate restTemplate = new RestTemplate(okHttpClientHttpRequestFactory);
        List<ClientHttpRequestInterceptor> interceptors = requestInterceptor.getIfAvailable();
        // 设置拦截器
        if (!CollectionUtils.isEmpty(interceptors)) {
            interceptors.removeIf(RetryLoadBalancerInterceptor.class::isInstance);
            restTemplate.setInterceptors(interceptors);
        }
        // 解决中文乱码问题
        for (HttpMessageConverter<?> messageConverter : restTemplate.getMessageConverters()) {
            if (messageConverter instanceof StringHttpMessageConverter) {
                ((StringHttpMessageConverter) messageConverter).setDefaultCharset(StandardCharsets.UTF_8);
                break;
            }
        }
        return restTemplate;
    }

    /**
     * 请求工厂
     *
     * @return ClientHttpRequestFactory
     */
    @Bean
    public ClientHttpRequestFactory okHttpClientHttpRequestFactory(HttpProperties httpProperties) {
        return new OkHttp3ClientHttpRequestFactory(okHttpConfigClient(httpProperties));
    }

    private OkHttpClient okHttpConfigClient(HttpProperties httpProperties) {
        ConnectionPool pool = new ConnectionPool(httpProperties.getMaxIdleConnections(), httpProperties.getKeepAliveDuration().getSeconds(), TimeUnit.SECONDS);
        return new OkHttpClient().newBuilder()
                .connectionPool(pool)
                // 设置连接超时时间
                .connectTimeout(httpProperties.getConnectTimeout().getSeconds(), TimeUnit.SECONDS)
                // 从连接成功到响应的总时间
                .readTimeout(httpProperties.getReadTimeout().getSeconds(), TimeUnit.SECONDS)
                // 写入超时时间
                .writeTimeout(httpProperties.getWriteTimeout().getSeconds(), TimeUnit.SECONDS)
                // 忽略ssl
                .sslSocketFactory(SkipSSLSocketClient.getSSLSocketFactory(), SkipSSLSocketClient.X509)
                .hostnameVerifier(SkipSSLSocketClient.getHostnameVerifier())
                .retryOnConnectionFailure(true)
                // 设置代理
                // .proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress("127.0.0.1", 8888)))
                // 拦截器
                // .addInterceptor()
                .build();
    }


}
