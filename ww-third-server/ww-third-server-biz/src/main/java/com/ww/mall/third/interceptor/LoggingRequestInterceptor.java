package com.ww.mall.third.interceptor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;
import org.springframework.util.StreamUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * @author ww
 * @create 2023-07-21- 17:10
 * @description: 第三方请求拦截器
 */
@Slf4j
@Component
public class LoggingRequestInterceptor implements ClientHttpRequestInterceptor {
    @NonNull
    @Override
    public ClientHttpResponse intercept(@NonNull HttpRequest request, @NonNull byte[] body, ClientHttpRequestExecution execution) throws IOException {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        traceRequest(request, body);
        ClientHttpResponse response = new BufferingClientHttpResponseWrapper(execution.execute(request, body));
        stopWatch.stop();
        traceResponse(response, stopWatch);
        return response;
    }

    private void traceRequest(HttpRequest request, byte[] body) {
        boolean fileUpdateFlag = request.getHeaders().getContentType().toString().contains(MediaType.MULTIPART_FORM_DATA_VALUE);
        log.info("\n" +
                        "===========================request begin================================================\n" +
                        "URI         : {}\n" +
                        "Method      : {}\n" +
                        "Headers     : {}\n" +
                        "Request body: {}\n" +
                        "===========================request end==================================================",
                request.getURI(), request.getMethod(), request.getHeaders(), fileUpdateFlag ?  "file": new String(body, StandardCharsets.UTF_8));
    }

    private void traceResponse(ClientHttpResponse response, StopWatch stopWatch) throws IOException {
        String body = StreamUtils.copyToString(response.getBody(), StandardCharsets.UTF_8);
        log.info("\n" +
                        "===========================response begin================================================\n" +
                        "Status code  : {}\n" +
                        "Status text  : {}\n" +
                        "Headers      : {}\n" +
                        "Response body: {}\n" +
                        "cost time    : {}ms\n" +
                        "===========================response end==================================================",
                response.getStatusCode(), response.getStatusText(), response.getHeaders(), body, stopWatch.getTotalTimeMillis());
    }

    /**
     * 响应内容备份
     */
    static final class BufferingClientHttpResponseWrapper implements ClientHttpResponse {

        private final ClientHttpResponse response;

        private byte[] body;

        BufferingClientHttpResponseWrapper(ClientHttpResponse response) {
            this.response = response;
        }

        @NonNull
        @Override
        public HttpStatus getStatusCode() throws IOException {
            return this.response.getStatusCode();
        }

        @Override
        public int getRawStatusCode() throws IOException {
            return this.response.getRawStatusCode();
        }

        @NonNull
        @Override
        public String getStatusText() throws IOException {
            return this.response.getStatusText();
        }

        @NonNull
        @Override
        public HttpHeaders getHeaders() {
            return this.response.getHeaders();
        }

        @NonNull
        @Override
        public InputStream getBody() throws IOException {
            if (this.body == null) {
                this.body = StreamUtils.copyToByteArray(this.response.getBody());
            }
            return new ByteArrayInputStream(this.body);
        }

        @Override
        public void close() {
            this.response.close();
        }
    }

}
