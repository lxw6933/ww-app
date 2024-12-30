package com.ww.app.web.handler;

import com.ww.app.common.constant.Constant;
import com.ww.app.common.exception.ApiException;
import com.ww.app.common.utils.SecretUtils;
import com.ww.app.web.config.SecretProperties;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter;

import javax.annotation.PostConstruct;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * @author ww
 * @create 2023-07-15- 09:58
 * @description: 请求body处理器
 */
@Slf4j
@Order(1)
@ControllerAdvice
public class RequestBodyHandler extends RequestBodyAdviceAdapter {

    @Autowired
    private SecretProperties secretProperties;

    @PostConstruct
    public void init() {
        log.info("初始化RequestBodyHandler成功...");
    }

    @Override
    public boolean supports(MethodParameter methodParameter, Type targetType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public HttpInputMessage beforeBodyRead(HttpInputMessage inputMessage, MethodParameter parameter, Type targetType, Class<? extends HttpMessageConverter<?>> converterType) throws IOException {
        return new SecretInputMessage(inputMessage);
    }

    class SecretInputMessage implements HttpInputMessage {
        private final HttpHeaders headers;
        private final HttpInputMessage httpInputMessage;

        public SecretInputMessage(HttpInputMessage httpInputMessage) {
            this.headers = httpInputMessage.getHeaders();
            this.httpInputMessage = httpInputMessage;
        }

        @SneakyThrows
        @Override
        public InputStream getBody() {
            // 判断请求是否是加密参数
            String encryptFlag = httpInputMessage.getHeaders().getFirst(Constant.ENCRYPT_HEADER);
            boolean encrypt = false;
            if (StringUtils.isNotBlank(encryptFlag)) {
                try {
                    encrypt = Boolean.parseBoolean(encryptFlag);
                } catch (Exception e) {
                    log.error("encryptFlag 类型解析异常", e);
                }
            }
            String httpBody;
            if (encrypt) {
                try {
                    // 解密请求body
                    String encryptBody = StreamUtils.copyToString(httpInputMessage.getBody(), Charset.defaultCharset());
                    log.info("接收加密参数：{}", encryptBody);
                    String decryptBody = SecretUtils.aesDecrypt(encryptBody, secretProperties.getSecretKey());
                    log.info("解密后的参数：{}", decryptBody);
                    httpBody = decryptBody;
                } catch (Exception e) {
                    log.error("请求参数解密失败", e);
                    throw new ApiException("请求报文解密失败");
                }
                if (StringUtils.isBlank(httpBody)) {
                    throw new HttpMessageConversionException("请求报文解密异常");
                }
            } else {
                httpBody = StreamUtils.copyToString(httpInputMessage.getBody(), Charset.defaultCharset());
            }
            return new ByteArrayInputStream(httpBody.getBytes(StandardCharsets.UTF_8));
        }

        @Override
        public HttpHeaders getHeaders() {
            return headers;
        }
    }

}
