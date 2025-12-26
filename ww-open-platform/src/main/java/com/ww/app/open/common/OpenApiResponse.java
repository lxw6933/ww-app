package com.ww.app.open.common;

import cn.hutool.core.date.DatePattern;
import com.ww.app.common.common.Result;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 开放平台统一响应格式
 * 
 * @author ww
 * @create 2024-05-27
 * @description: 开放平台API的统一响应格式，包含流水号等信息
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class OpenApiResponse<T> extends Result<T> {

    /**
     * 流水号
     */
    private String transId;

    /**
     * 响应时间（yyyy-MM-dd HH:mm:ss）
     */
    private String respTime;

    /**
     * 应用编码
     */
    private String appCode;

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern(DatePattern.NORM_DATETIME_PATTERN);

    public static <T> OpenApiResponse<T> success(T data, String transId, String appCode) {
        OpenApiResponse<T> response = new OpenApiResponse<>();
        response.setData(data);
        response.setTransId(transId);
        response.setAppCode(appCode);
        response.setCode(200);
        response.setMsg("success");
        response.setRespTime(java.time.LocalDateTime.now().format(DATE_TIME_FORMATTER));
        return response;
    }

    public static <T> OpenApiResponse<T> error(Integer code, String msg, String transId, String appCode) {
        OpenApiResponse<T> response = new OpenApiResponse<>();
        response.setCode(code);
        response.setMsg(msg);
        response.setTransId(transId);
        response.setAppCode(appCode);
        response.setRespTime(LocalDateTime.now().format(DATE_TIME_FORMATTER));
        return response;
    }
}

