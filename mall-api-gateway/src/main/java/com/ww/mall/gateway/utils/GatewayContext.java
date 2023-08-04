package com.ww.mall.gateway.utils;

import lombok.Data;
import org.springframework.util.MultiValueMap;

/**
 * @author ww
 * @create 2022-04-13- 16:14
 * @description:
 */
@Data
public class GatewayContext {

    public static final String CACHE_GATEWAY_CONTEXT = "cacheGatewayContext";

    /**
     * 缓存post请求body参数信息
     */
    private String cacheBody;

    /**
     * 缓存表单参数信息
     */
    private MultiValueMap<String, String> formData;

    /**
     * 缓存请求路劲信息
     */
    private String path;

}
