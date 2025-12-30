package com.ww.app.open.service;

import com.ww.app.open.entity.OpenApiPermission;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 开放平台API权限服务接口
 * 
 * @author ww
 * @create 2024-05-27
 * @description: API权限管理服务，控制应用对API的访问权限
 */
public interface OpenApiPermissionService extends IService<OpenApiPermission> {

    /**
     * 检查应用是否有权限调用API
     * 
     * @param appCode 应用编码
     * @param apiCode API编码
     * @return 是否有权限
     */
    boolean hasPermission(String appCode, String apiCode);

    /**
     * 获取应用的API限流配置
     * 
     * @param appCode 应用编码
     * @param apiCode API编码
     * @return 限流QPS，如果返回null则使用API默认配置
     */
    Long getQpsLimit(String appCode, String apiCode);

    /**
     * 授权应用访问API
     * 
     * @param permission 权限信息
     * @return 是否成功
     */
    boolean grantPermission(OpenApiPermission permission);

    /**
     * 撤销应用对API的访问权限
     * 
     * @param appCode 应用编码
     * @param apiCode API编码
     * @return 是否成功
     */
    boolean revokePermission(String appCode, String apiCode);
}



