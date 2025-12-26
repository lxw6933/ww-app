package com.ww.app.open.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ww.app.open.entity.OpenApiPermission;
import com.ww.app.open.infrastructure.OpenApiPermissionMapper;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.ww.app.common.utils.CaffeineUtil;
import com.ww.app.open.constant.OpenPlatformConstants;
import com.ww.app.open.service.OpenApiPermissionService;
import com.ww.app.open.utils.CacheKeyBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.util.concurrent.TimeUnit;

/**
 * 开放平台API权限服务实现类
 * 
 * @author ww
 * @create 2024-05-27
 */
@Slf4j
@Service
public class OpenApiPermissionServiceImpl extends ServiceImpl<OpenApiPermissionMapper, OpenApiPermission> 
        implements OpenApiPermissionService {

    /**
     * 权限缓存
     * 容量: 2000-5000
     * 过期时间: 1小时
     */
    private LoadingCache<String, Boolean> permissionCache;

    @PostConstruct
    public void init() {
        this.permissionCache = CaffeineUtil.createAutoRefreshCache(
                OpenPlatformConstants.PERMISSION_CACHE_INITIAL_CAPACITY,
                OpenPlatformConstants.PERMISSION_CACHE_MAXIMUM_SIZE,
                OpenPlatformConstants.PERMISSION_CACHE_EXPIRE_MINUTES,
                TimeUnit.MINUTES,
                OpenPlatformConstants.PERMISSION_CACHE_REFRESH_MINUTES,
                TimeUnit.MINUTES,
                this::loadPermission
        );

        log.info("OpenApiPermissionService 缓存初始化完成 - 容量: {}, 过期时间: {}分钟",
                OpenPlatformConstants.PERMISSION_CACHE_MAXIMUM_SIZE,
                OpenPlatformConstants.PERMISSION_CACHE_EXPIRE_MINUTES);
    }

    @Override
    public boolean hasPermission(String appCode, String apiCode) {
        String key = CacheKeyBuilder.buildPermissionKey(appCode, apiCode);
        return permissionCache.get(key);
    }

    /**
     * 从数据库加载权限信息
     */
    private Boolean loadPermission(String key) {
        String[] parts = key.split(OpenPlatformConstants.KEY_SEPARATOR, 2);
        if (parts.length != 2) {
            return Boolean.FALSE;
        }
        String appCode = parts[0];
        String apiCode = parts[1];

        return this.count(OpenApiPermission.buildValidQueryWrapper(appCode, apiCode)) > 0;
    }

    @Override
    public Long getQpsLimit(String appCode, String apiCode) {
        OpenApiPermission permission = this.getOne(OpenApiPermission.buildAppQueryWrapper(appCode, apiCode));
        if (permission != null && permission.getCustomQps() != null) {
            return permission.getCustomQps();
        }
        
        return null; // 返回null表示使用API默认配置
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean grantPermission(OpenApiPermission permission) {
        // 设置默认状态为启用
        if (permission.getStatus() == null) {
            permission.setStatus(OpenPlatformConstants.STATUS_ENABLED);
        }
        
        boolean success = this.save(permission);
        
        if (success) {
            clearCache(permission.getAppCode(), permission.getApiCode());
            log.info("授权成功: appCode={}, apiCode={}", permission.getAppCode(), permission.getApiCode());
        }
        
        return success;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean revokePermission(String appCode, String apiCode) {
        OpenApiPermission permission = this.getOne(OpenApiPermission.buildAppQueryWrapper(appCode, apiCode));
        if (permission == null) {
            return false;
        }

        permission.setStatus(OpenPlatformConstants.STATUS_DISABLED);
        boolean success = this.updateById(permission);
        
        if (success) {
            clearCache(appCode, apiCode);
            log.info("撤销权限成功: appCode={}, apiCode={}", appCode, apiCode);
        }
        
        return success;
    }

    /**
     * 清除缓存
     */
    private void clearCache(String appCode, String apiCode) {
        String key = CacheKeyBuilder.buildPermissionKey(appCode, apiCode);
        permissionCache.invalidate(key);
    }
}


