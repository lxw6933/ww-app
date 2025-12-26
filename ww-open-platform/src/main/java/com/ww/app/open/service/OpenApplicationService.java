package com.ww.app.open.service;

import com.ww.app.open.entity.OpenApplication;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 开放平台应用服务接口
 * 
 * @author ww
 * @create 2024-05-27
 * @description: 应用管理服务，提供应用的注册、审核、查询等功能
 */
public interface OpenApplicationService extends IService<OpenApplication> {

    /**
     * 根据应用编码查询应用信息
     * 
     * @param appCode 应用编码
     * @return 应用信息
     */
    OpenApplication getByAppCode(String appCode);

    /**
     * 注册新应用
     * 
     * @param application 应用信息
     * @return 是否成功
     */
    boolean registerApplication(OpenApplication application);

    /**
     * 审核应用
     * 
     * @param appCode 应用编码
     * @param status 审核状态
     * @param auditRemark 审核意见
     * @param auditor 审核人
     * @return 是否成功
     */
    boolean auditApplication(String appCode, Integer status, String auditRemark, String auditor);

    /**
     * 启用/禁用应用
     * 
     * @param appCode 应用编码
     * @param enabled 是否启用
     * @return 是否成功
     */
    boolean enableApplication(String appCode, boolean enabled);

    /**
     * 验证应用密钥
     * 
     * @param appCode 应用编码
     * @param appSecret 应用密钥
     * @return 是否有效
     */
    boolean validateAppSecret(String appCode, String appSecret);
}


