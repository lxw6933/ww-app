package com.ww.app.open.service;

import com.ww.app.open.entity.OpenApiInfo;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 开放平台API信息服务接口
 * 
 * @author ww
 * @create 2024-05-27
 * @description: API管理服务，提供API的注册、查询、版本管理等功能
 */
public interface OpenApiInfoService extends IService<OpenApiInfo> {

    /**
     * 根据API编码查询API信息
     * 
     * @param apiCode API编码
     * @return API信息
     */
    OpenApiInfo getByApiCode(String apiCode);

    /**
     * 根据路径和方法查询API信息
     * 
     * @param apiPath API路径
     * @param httpMethod HTTP方法
     * @return API信息
     */
    OpenApiInfo getByPathAndMethod(String apiPath, String httpMethod);

    /**
     * 注册新API
     * 
     * @param apiInfo API信息
     * @return 是否成功
     */
    boolean registerApi(OpenApiInfo apiInfo);

    /**
     * 发布API
     * 
     * @param apiCode API编码
     * @return 是否成功
     */
    boolean publishApi(String apiCode);

    /**
     * 下线API
     * 
     * @param apiCode API编码
     * @return 是否成功
     */
    boolean offlineApi(String apiCode);
}


