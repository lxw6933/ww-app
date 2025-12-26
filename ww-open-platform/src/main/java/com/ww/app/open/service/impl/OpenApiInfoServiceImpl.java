package com.ww.app.open.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ww.app.open.entity.OpenApiInfo;
import com.ww.app.open.enums.ApiStatus;
import com.ww.app.open.infrastructure.OpenApiInfoMapper;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.ww.app.common.utils.CaffeineUtil;
import com.ww.app.open.constant.OpenPlatformConstants;
import com.ww.app.open.service.OpenApiInfoService;
import com.ww.app.open.utils.CacheKeyBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.util.concurrent.TimeUnit;

/**
 * 开放平台API信息服务实现类
 * 
 * @author ww
 * @create 2024-05-27
 */
@Slf4j
@Service
public class OpenApiInfoServiceImpl extends ServiceImpl<OpenApiInfoMapper, OpenApiInfo> 
        implements OpenApiInfoService {

    /**
     * API信息缓存（按apiCode）
     * 容量: 500-1000
     * 过期时间: 1小时
     */
    private LoadingCache<String, OpenApiInfo> apiCodeCache;

    /**
     * API信息缓存（按路径和方法）
     * 容量: 500-1000
     * 过期时间: 1小时
     */
    private LoadingCache<String, OpenApiInfo> apiPathCache;

    @PostConstruct
    public void init() {
        // 初始化按apiCode的缓存
        this.apiCodeCache = CaffeineUtil.createAutoRefreshCache(
                OpenPlatformConstants.API_CACHE_INITIAL_CAPACITY,
                OpenPlatformConstants.API_CACHE_MAXIMUM_SIZE,
                OpenPlatformConstants.API_CACHE_EXPIRE_MINUTES,
                TimeUnit.MINUTES,
                OpenPlatformConstants.API_CACHE_REFRESH_MINUTES,
                TimeUnit.MINUTES,
                this::loadByApiCode
        );

        // 初始化按路径和方法的缓存
        this.apiPathCache = CaffeineUtil.createAutoRefreshCache(
                OpenPlatformConstants.API_CACHE_INITIAL_CAPACITY,
                OpenPlatformConstants.API_CACHE_MAXIMUM_SIZE,
                OpenPlatformConstants.API_CACHE_EXPIRE_MINUTES,
                TimeUnit.MINUTES,
                OpenPlatformConstants.API_CACHE_REFRESH_MINUTES,
                TimeUnit.MINUTES,
                this::loadByPathAndMethod
        );

        log.info("OpenApiInfoService 缓存初始化完成 - 容量: {}, 过期时间: {}分钟",
                OpenPlatformConstants.API_CACHE_MAXIMUM_SIZE,
                OpenPlatformConstants.API_CACHE_EXPIRE_MINUTES);
    }

    @Override
    public OpenApiInfo getByApiCode(String apiCode) {
        return apiCodeCache.get(apiCode);
    }

    /**
     * 从数据库加载API信息（按apiCode）
     */
    private OpenApiInfo loadByApiCode(String apiCode) {
        LambdaQueryWrapper<OpenApiInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OpenApiInfo::getApiCode, apiCode);
        return this.getOne(wrapper);
    }

    @Override
    public OpenApiInfo getByPathAndMethod(String apiPath, String httpMethod) {
        String key = CacheKeyBuilder.buildApiPathKey(apiPath, httpMethod);
        return apiPathCache.get(key);
    }

    /**
     * 从数据库加载API信息（按路径和方法）
     */
    private OpenApiInfo loadByPathAndMethod(String key) {
        String[] parts = key.split(OpenPlatformConstants.KEY_SEPARATOR, 2);
        if (parts.length != 2) {
            return null;
        }
        String apiPath = parts[0];
        String httpMethod = parts[1];

        LambdaQueryWrapper<OpenApiInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OpenApiInfo::getApiPath, apiPath)
               .eq(OpenApiInfo::getHttpMethod, httpMethod);
        return this.getOne(wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean registerApi(OpenApiInfo apiInfo) {
        // 设置初始状态为开发中
        if (apiInfo.getStatus() == null) {
            apiInfo.setStatus(ApiStatus.DEVELOPING.getCode());
        }
        
        boolean success = this.save(apiInfo);
        
        if (success) {
            log.info("API注册成功: apiCode={}, apiPath={}", apiInfo.getApiCode(), apiInfo.getApiPath());
        }
        
        return success;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean publishApi(String apiCode) {
        return updateApiStatus(apiCode, ApiStatus.PUBLISHED.getCode(), "发布");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean offlineApi(String apiCode) {
        return updateApiStatus(apiCode, ApiStatus.OFFLINE.getCode(), "下线");
    }

    /**
     * 更新API状态（抽取公共方法，避免重复代码）
     * 
     * @param apiCode API编码
     * @param status 新状态
     * @param action 操作名称（用于日志）
     * @return 是否成功
     */
    private boolean updateApiStatus(String apiCode, Integer status, String action) {
        LambdaQueryWrapper<OpenApiInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OpenApiInfo::getApiCode, apiCode);
        
        OpenApiInfo apiInfo = this.getOne(wrapper);
        if (apiInfo == null) {
            return false;
        }

        apiInfo.setStatus(status);
        boolean success = this.updateById(apiInfo);
        
        if (success) {
            clearCache(apiCode);
            log.info("API{}成功: apiCode={}", action, apiCode);
        }
        
        return success;
    }

    /**
     * 清除缓存
     */
    private void clearCache(String apiCode) {
        apiCodeCache.invalidate(apiCode);
        // 清除路径缓存：需要查询API信息获取路径和方法，然后删除对应的缓存
        OpenApiInfo apiInfo = loadByApiCode(apiCode);
        if (apiInfo != null && apiInfo.getApiPath() != null && apiInfo.getHttpMethod() != null) {
            String pathKey = CacheKeyBuilder.buildApiPathKey(apiInfo.getApiPath(), apiInfo.getHttpMethod());
            apiPathCache.invalidate(pathKey);
        }
    }
}


