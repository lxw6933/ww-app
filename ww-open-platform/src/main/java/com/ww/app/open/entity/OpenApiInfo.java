package com.ww.app.open.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.ww.app.mybatis.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 开放平台API信息实体
 * 
 * @author ww
 * @create 2024-05-27
 * @description: API接口管理实体，用于管理开放平台提供的API接口信息
 */
@Data
@TableName("open_api_info")
@EqualsAndHashCode(callSuper = true)
public class OpenApiInfo extends BaseEntity {

    /**
     * API编码（唯一标识，格式：模块.功能.方法）
     */
    private String apiCode;

    /**
     * API名称
     */
    private String apiName;

    /**
     * API版本号
     */
    private String apiVersion;

    /**
     * API路径
     */
    private String apiPath;

    /**
     * HTTP方法（GET, POST, PUT, DELETE等）
     */
    private String httpMethod;

    /**
     * API描述
     */
    private String description;

    /**
     * API分组
     */
    private String apiGroup;

    /**
     * 是否需要签名验证：0-否，1-是
     */
    private Integer needSign;

    /**
     * 是否需要权限验证：0-否，1-是
     */
    private Integer needAuth;

    /**
     * 默认限流配置（QPS）
     */
    private Long defaultQps;

    /**
     * API状态：0-开发中，1-已发布，2-已下线，3-已废弃
     */
    private Integer status;

    /**
     * 请求参数示例（JSON格式）
     */
    private String requestExample;

    /**
     * 响应参数示例（JSON格式）
     */
    private String responseExample;

    /**
     * 是否需要登录：0-否，1-是
     */
    private Integer needLogin;

    /**
     * 排序号
     */
    private Integer sortOrder;
}

