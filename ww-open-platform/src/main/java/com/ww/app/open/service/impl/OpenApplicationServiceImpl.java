package com.ww.app.open.service.impl;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.ww.app.common.utils.CaffeineUtil;
import com.ww.app.open.constant.OpenPlatformConstants;
import com.ww.app.open.entity.OpenApplication;
import com.ww.app.open.enums.ApplicationStatus;
import com.ww.app.open.infrastructure.OpenApplicationMapper;
import com.ww.app.open.service.OpenApplicationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.util.concurrent.TimeUnit;

/**
 * 开放平台应用服务实现类
 * 
 * @author ww
 * @create 2024-05-27
 */
@Slf4j
@Service
public class OpenApplicationServiceImpl extends ServiceImpl<OpenApplicationMapper, OpenApplication> 
        implements OpenApplicationService {

    /**
     * 应用信息缓存
     * 容量: 500-1000
     * 过期时间: 1小时
     */
    private LoadingCache<String, OpenApplication> applicationCache;

    @PostConstruct
    public void init() {
        this.applicationCache = CaffeineUtil.createAutoRefreshCache(
                OpenPlatformConstants.APPLICATION_CACHE_INITIAL_CAPACITY,
                OpenPlatformConstants.APPLICATION_CACHE_MAXIMUM_SIZE,
                OpenPlatformConstants.APPLICATION_CACHE_EXPIRE_MINUTES,
                TimeUnit.MINUTES,
                OpenPlatformConstants.APPLICATION_CACHE_REFRESH_MINUTES,
                TimeUnit.MINUTES,
                this::loadApplication
        );

        log.info("OpenApplicationService 缓存初始化完成 - 容量: {}, 过期时间: {}分钟",
                OpenPlatformConstants.APPLICATION_CACHE_MAXIMUM_SIZE,
                OpenPlatformConstants.APPLICATION_CACHE_EXPIRE_MINUTES);
    }

    @Override
    public OpenApplication getByAppCode(String appCode) {
        return applicationCache.get(appCode);
    }

    /**
     * 从数据库加载应用信息
     */
    private OpenApplication loadApplication(String appCode) {
        return this.getOne(OpenApplication.buildAppQueryWrapper(appCode));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean registerApplication(OpenApplication application) {
        // 生成应用密钥
        if (application.getAppSecret() == null) {
            application.setAppSecret(generateAppSecret());
        }
        
        // 设置初始状态为待审核
        application.setStatus(ApplicationStatus.PENDING.getCode());
        
        // 保存到数据库
        boolean success = this.save(application);
        
        if (success) {
            log.info("应用注册成功: appCode={}, sysCode={}", application.getAppCode(), application.getSysCode());
        }
        
        return success;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean auditApplication(String appCode, Integer status, String auditRemark, String auditor) {
        OpenApplication application = this.getOne(OpenApplication.buildAppQueryWrapper(appCode));
        if (application == null) {
            return false;
        }

        application.setStatus(status);
        application.setAuditRemark(auditRemark);
        application.setAuditor(auditor);
        application.setAuditTime(System.currentTimeMillis());

        boolean success = this.updateById(application);
        
        if (success) {
            clearCache(appCode);
            log.info("应用审核完成: appCode={}, status={}, auditor={}", appCode, status, auditor);
        }
        
        return success;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean enableApplication(String appCode, boolean enabled) {
        Integer status = enabled ? ApplicationStatus.ENABLED.getCode() : ApplicationStatus.DISABLED.getCode();
        return updateApplicationStatus(appCode, status, "状态变更");
    }

    /**
     * 更新应用状态（抽取公共方法，避免重复代码）
     * 
     * @param appCode 应用编码
     * @param status 新状态
     * @param action 操作名称（用于日志）
     * @return 是否成功
     */
    private boolean updateApplicationStatus(String appCode, Integer status, String action) {
        OpenApplication application = this.getOne(OpenApplication.buildAppQueryWrapper(appCode));
        if (application == null) {
            return false;
        }

        application.setStatus(status);
        boolean success = this.updateById(application);
        
        if (success) {
            clearCache(appCode);
            log.info("应用{}: appCode={}, status={}", action, appCode, status);
        }
        
        return success;
    }

    @Override
    public boolean validateAppSecret(String appCode, String appSecret) {
        OpenApplication application = getByAppCode(appCode);
        if (application == null || application.getAppSecret() == null) {
            return false;
        }
        return application.getAppSecret().equals(appSecret);
    }

    /**
     * 生成应用密钥
     */
    private String generateAppSecret() {
        return IdUtil.simpleUUID() + System.currentTimeMillis();
    }

    /**
     * 清除缓存
     */
    private void clearCache(String appCode) {
        applicationCache.invalidate(appCode);
    }
}


