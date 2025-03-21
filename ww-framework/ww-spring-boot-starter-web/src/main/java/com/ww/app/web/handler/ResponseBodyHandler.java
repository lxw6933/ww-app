package com.ww.app.web.handler;

import com.ww.app.common.common.Result;
import com.ww.app.common.constant.Constant;
import com.ww.app.common.exception.ApiException;
import com.ww.app.common.utils.SecretUtils;
import com.ww.app.common.utils.json.JacksonUtils;
import com.ww.app.web.config.SecretProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.lang.NonNull;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;

/**
 * @author ww
 * @create 2023-07-15- 09:58
 * @description: 响应结果处理器
 */
@Slf4j
@ControllerAdvice
public class ResponseBodyHandler implements ResponseBodyAdvice<Object> {

    private static final List<String> FILTER_PACKAGE = Collections.singletonList("com.ww.app");

    @Resource
    private SecretProperties secretProperties;

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @PostConstruct
    public void init() {
        log.info("初始化ResponseBodyHandler成功...");
    }

    @Override
    public boolean supports(@NonNull MethodParameter returnType, @NonNull Class converterType) {
        // 通过supports方法，我们可以选择哪些类，或者哪些方法要对response进行处理，其余的则不处理。
        // true：进行拦截响应   fasle：不拦截   默认拦截所有mvc响应
        // 只对@ResponseBody有效果
//        boolean responseBody = returnType.hasParameterAnnotation(RequestBody.class);
        // 判断请求的方法是否贴有@Secret注解
        return filter(returnType);
    }

    @Override
    public Object beforeBodyWrite(Object body, @NonNull MethodParameter returnType, @NonNull MediaType selectedContentType, @NonNull Class selectedConverterType, @NonNull ServerHttpRequest request, @NonNull ServerHttpResponse response) {
        Result<Object> result;
        if (body instanceof Result) {
            result = (Result<Object>) body;
        } else {
            result = Result.success(body);
        }
        response.getHeaders().set(Constant.ENCRYPT_HEADER, "false");
        // 是否开启加密
        if (secretProperties.isEnabled()) {
            boolean encrypt = isEncrypt(request, secretProperties.getEncryptUriList(), secretProperties.getExcludeUriList());
            if (encrypt) {
                // 告知前端响应结果已加密
                response.getHeaders().set(Constant.ENCRYPT_HEADER, "true");
                try {
                    return SecretUtils.aesEncrypt(JacksonUtils.toJsonString(result), secretProperties.getSecretKey());
                } catch (Exception e) {
                    throw new ApiException("内容不合法");
                }
            }
        }
        return result;
    }

    private boolean isEncrypt(ServerHttpRequest request, List<String> encryptUriList, List<String> excludeUriList) {
        boolean encrypt = false;
        // 远程调用接口不加密
        String feignFlag = request.getHeaders().getFirst(Constant.FEIGN_FLAG);
        if (StringUtils.isNotEmpty(feignFlag) && Boolean.TRUE.equals(Boolean.parseBoolean(feignFlag))) {
            return false;
        }
        // 需要加密的接口
        if (CollectionUtils.isNotEmpty(encryptUriList)) {
            for (String uri : encryptUriList) {
                if (pathMatcher.match(uri, request.getURI().getPath())) {
                    encrypt = true;
                    break;
                }
            }
        }
        // 不需要加密的接口
        if (CollectionUtils.isNotEmpty(excludeUriList) && encrypt) {
            for (String excludeUri : excludeUriList) {
                if (pathMatcher.match(excludeUri, request.getURI().getPath())) {
                    encrypt = false;
                    break;
                }
            }
        }
        return encrypt;
    }

    private Boolean filter(MethodParameter methodParameter) {
        Class<?> declaringClass = methodParameter.getDeclaringClass();
        // 检查过滤包路径
        return FILTER_PACKAGE.stream().anyMatch(l -> declaringClass.getName().contains(l));
    }
}
