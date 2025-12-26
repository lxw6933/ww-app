package com.ww.app.open.service;

import com.ww.app.open.entity.OpenConfig;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 开放平台配置服务接口
 * 
 * @author ww
 * @create 2024-05-27
 * @description: 配置管理服务，提供动态配置功能
 */
public interface OpenConfigService extends IService<OpenConfig> {

    /**
     * 获取配置值
     * 
     * @param configKey 配置键
     * @return 配置值
     */
    String getConfigValue(String configKey);

    /**
     * 获取配置值（带默认值）
     * 
     * @param configKey 配置键
     * @param defaultValue 默认值
     * @return 配置值
     */
    String getConfigValue(String configKey, String defaultValue);

    /**
     * 设置配置值
     * 
     * @param configKey 配置键
     * @param configValue 配置值
     * @return 是否成功
     */
    boolean setConfigValue(String configKey, String configValue);

    /**
     * 刷新配置缓存
     */
    void refreshCache();
}


