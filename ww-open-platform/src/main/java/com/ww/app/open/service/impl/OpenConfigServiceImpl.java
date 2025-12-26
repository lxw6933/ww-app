package com.ww.app.open.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ww.app.open.entity.OpenConfig;
import com.ww.app.open.infrastructure.OpenConfigMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.ww.app.common.utils.CaffeineUtil;
import com.ww.app.open.constant.OpenPlatformConstants;
import com.ww.app.open.service.OpenConfigService;
import com.ww.app.open.utils.StatusValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.util.concurrent.TimeUnit;

/**
 * 开放平台配置服务实现类
 * 
 * @author ww
 * @create 2024-05-27
 */
@Slf4j
@Service
public class OpenConfigServiceImpl extends ServiceImpl<OpenConfigMapper, OpenConfig> 
        implements OpenConfigService {

    /**
     * 配置缓存
     * 容量: 500-1000
     * 过期时间: 1小时
     */
    private LoadingCache<String, String> configCache;

    /**
     * 空值缓存（防止缓存穿透）
     * 容量: 500
     * 过期时间: 5分钟
     */
    private Cache<String, Boolean> nullValueCache;

    @PostConstruct
    public void init() {
        this.configCache = CaffeineUtil.createAutoRefreshCache(
                OpenPlatformConstants.CONFIG_CACHE_INITIAL_CAPACITY,
                OpenPlatformConstants.CONFIG_CACHE_MAXIMUM_SIZE,
                OpenPlatformConstants.CONFIG_CACHE_EXPIRE_MINUTES,
                TimeUnit.MINUTES,
                OpenPlatformConstants.CONFIG_CACHE_REFRESH_MINUTES,
                TimeUnit.MINUTES,
                this::loadConfig
        );

        // 初始化空值缓存
        this.nullValueCache = CaffeineUtil.createCache(
                OpenPlatformConstants.NULL_VALUE_CACHE_INITIAL_CAPACITY,
                OpenPlatformConstants.NULL_VALUE_CACHE_MAXIMUM_SIZE,
                OpenPlatformConstants.NULL_VALUE_CACHE_EXPIRE_MINUTES,
                TimeUnit.MINUTES
        );

        log.info("OpenConfigService 缓存初始化完成 - 配置缓存容量: {}, 空值缓存容量: {}",
                OpenPlatformConstants.CONFIG_CACHE_MAXIMUM_SIZE,
                OpenPlatformConstants.NULL_VALUE_CACHE_MAXIMUM_SIZE);
    }

    @Override
    public String getConfigValue(String configKey) {
        return getConfigValue(configKey, null);
    }

    @Override
    public String getConfigValue(String configKey, String defaultValue) {
        try {
            // 检查空值缓存
            Boolean isNull = nullValueCache.getIfPresent(configKey);
            if (Boolean.TRUE.equals(isNull)) {
                return defaultValue;
            }

            String value = configCache.get(configKey);
            if (value != null) {
                return value;
            }

            // 如果缓存返回null，说明配置不存在，加入空值缓存
            nullValueCache.put(configKey, Boolean.TRUE);
            return defaultValue;
        } catch (Exception e) {
            log.error("获取配置失败: configKey={}", configKey, e);
            return defaultValue;
        }
    }

    /**
     * 从数据库加载配置
     */
    private String loadConfig(String configKey) {
        LambdaQueryWrapper<OpenConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OpenConfig::getConfigKey, configKey);
        OpenConfig config = this.getOne(wrapper);
        return config != null ? config.getConfigValue() : null;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean setConfigValue(String configKey, String configValue) {
        LambdaQueryWrapper<OpenConfig> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OpenConfig::getConfigKey, configKey);
        
        OpenConfig config = this.getOne(wrapper);
        if (config == null) {
            config = new OpenConfig();
            config.setConfigKey(configKey);
            config.setConfigValue(configValue);
            return this.save(config);
        } else {
            if (!StatusValidator.isConfigEditable(config.getEditable())) {
                log.warn("配置不可修改: configKey={}", configKey);
                return false;
            }
            config.setConfigValue(configValue);
            boolean success = this.updateById(config);
            if (success) {
                clearCache(configKey);
            }
            return success;
        }
    }

    @Override
    public void refreshCache() {
        // 清除所有配置缓存，下次访问时重新加载
        configCache.invalidateAll();
        log.info("配置缓存已刷新");
    }

    /**
     * 清除缓存
     */
    private void clearCache(String configKey) {
        configCache.invalidate(configKey);
        nullValueCache.invalidate(configKey);
    }
}


