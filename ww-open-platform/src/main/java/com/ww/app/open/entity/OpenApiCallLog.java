package com.ww.app.open.entity;

import com.ww.app.mongodb.common.BaseDoc;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * 开放平台API调用日志实体
 */
@Data
@Document("open_api_call_log")
@EqualsAndHashCode(callSuper = true)
public class OpenApiCallLog extends BaseDoc {

    /** 流水号 */
    private String transId;

    /** 应用编码 */
    private String appCode;

    /** 商户编码 */
    private String sysCode;

    /** API编码 */
    private String apiCode;

    /** API路径 */
    private String apiPath;

    /** HTTP方法 */
    private String httpMethod;

    /** 请求IP */
    private String requestIp;

    /** 请求时间（毫秒时间戳） */
    private Long requestTime;

    /** 响应时间（毫秒时间戳） */
    private Long responseTime;

    /** 耗时（毫秒） */
    private Long duration;

    /** 请求参数（脱敏后） */
    private String requestParams;

    /** 响应结果（脱敏后） */
    private String responseResult;

    /** 响应状态码 */
    private Integer responseStatus;

    /** 是否成功：0-失败，1-成功 */
    private Integer success;

    /** 错误信息 */
    private String errorMessage;

    /** 用户ID（如果API需要登录） */
    private Long userId;
}

