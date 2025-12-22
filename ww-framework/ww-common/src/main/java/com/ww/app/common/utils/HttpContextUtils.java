package com.ww.app.common.utils;

import cn.hutool.core.io.IoUtil;
import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ww.app.common.common.Result;
import com.ww.app.common.constant.Constant;
import com.ww.app.common.utils.json.JacksonUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.MediaType;
import org.springframework.util.CollectionUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * @author ww
 * @create 2024-09-20- 17:22
 * @description:
 */
public class HttpContextUtils {

    private static final ObjectMapper MAPPER = JacksonUtils.getObjectMapper();

    private HttpContextUtils() {}

    /**
     * 判断是否为JSON请求
     *
     * @param request HTTP请求
     * @return 是否为JSON请求
     */
    public static boolean isJsonRequest(HttpServletRequest request) {
        String contentType = request.getContentType();
        return contentType != null && contentType.contains(MediaType.APPLICATION_JSON_VALUE);
    }

    /**
     * 从请求头获取指定参数
     *
     * @param request    HTTP请求
     * @param headerName 请求头名称
     * @return 参数值
     */
    public static String getHeaderValue(HttpServletRequest request, String headerName) {
        return request.getHeader(headerName);
    }

    /**
     * 从请求中获取所有参数(URL参数)
     *
     * @param request HTTP请求
     * @return 参数Map
     */
    public static Map<String, Object> getRequestParams(HttpServletRequest request) {
        Map<String, Object> params = new TreeMap<>();
        // 获取请求参数
        Map<String, String[]> requestParams = request.getParameterMap();
        if (!CollectionUtils.isEmpty(requestParams)) {
            for (Map.Entry<String, String[]> entry : requestParams.entrySet()) {
                String key = entry.getKey();
                String[] values = entry.getValue();
                if (values != null && values.length > 0) {
                    params.put(key, values[0]);
                }
            }
        }
        return params;
    }

    /**
     * 获取请求体中的JSON参数
     *
     * @param request HTTP请求
     * @return 参数Map
     */
    public static Map<String, Object> getRequestBodyParams(HttpServletRequest request) {
        Map<String, Object> params = new TreeMap<>();
        // 只处理JSON请求体
        if (HttpContextUtils.isJsonRequest(request)) {
            try {
                // 读取请求体内容
                String body = IoUtil.read(request.getInputStream(), StandardCharsets.UTF_8);
                if (StringUtils.isNotBlank(body)) {
                    JsonNode node = MAPPER.readTree(body);
                    params = JacksonUtils.convertJsonNode(node);
                    // 解析JSON数据
//                    Map<String, Object> jsonMap = JacksonUtils.getObjectMapper().readValue(body, new TypeReference<Map<String, Object>>() {});
//                    if (jsonMap != null) {
//                        for (Map.Entry<String, Object> entry : jsonMap.entrySet()) {
//                            String key = entry.getKey();
//                            Object value = entry.getValue();
//                            if (value != null) {
//                                params.put(key, String.valueOf(value));
//                            }
//                        }
//                    }
                }
            } catch (IOException e) {
                // 记录日志或处理异常
                throw new RuntimeException("解析请求体参数失败", e);
            }
        }
        return params;
    }

    public static HttpServletRequest getHttpServletRequest() {
        return ((ServletRequestAttributes) Objects.requireNonNull(RequestContextHolder.getRequestAttributes())).getRequest();
    }

    public static String getAppReqIp() {
        return HttpContextUtils.getHttpServletRequest().getHeader(Constant.USER_REAL_IP);
    }

    public static String getProjectPath(){
        HttpServletRequest request = getHttpServletRequest();
        return request.getScheme() + "://" + request.getServerName() + ":"
                + request.getServerPort() + request.getContextPath() + "/";
    }

    public static void write(HttpServletResponse response, Result<Object> result) {
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_OK);
        response.setCharacterEncoding("UTF-8");
        try(PrintWriter writer = response.getWriter()) {
            writer.write(JSON.toJSONString(result));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
