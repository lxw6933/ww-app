package com.ww.app.open.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ww.app.mybatis.common.BaseEntity;
import com.ww.app.open.constant.OpenPlatformConstants;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 开放平台API权限实体
 * 
 * @author ww
 * @create 2024-05-27
 * @description: 应用与API的权限关联表，控制哪些应用可以调用哪些API
 */
@Data
@TableName("open_api_permission")
@EqualsAndHashCode(callSuper = true)
public class OpenApiPermission extends BaseEntity {

    /**
     * 应用编码
     */
    private String appCode;

    /**
     * API编码
     */
    private String apiCode;

    /**
     * 权限状态：0-禁用，1-启用
     */
    private Integer status;

    /**
     * 自定义限流配置（QPS），为空则使用API默认配置
     */
    private Long customQps;

    /**
     * 权限类型：0-只读，1-读写
     */
    private int permissionType;

    /**
     * 生效开始时间
     */
    private Long startTime;

    /**
     * 生效结束时间
     */
    private Long endTime;

    public static LambdaQueryWrapper<OpenApiPermission> buildAppQueryWrapper(String appCode, String apiCode) {
        LambdaQueryWrapper<OpenApiPermission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OpenApiPermission::getAppCode, appCode)
                .eq(OpenApiPermission::getApiCode, apiCode);
        return wrapper;
    }

    public static LambdaQueryWrapper<OpenApiPermission> buildValidQueryWrapper(String appCode, String apiCode) {
        LambdaQueryWrapper<OpenApiPermission> wrapper = buildAppQueryWrapper(appCode, apiCode);
        wrapper.eq(OpenApiPermission::getStatus, OpenPlatformConstants.STATUS_ENABLED);

        long currentTime = System.currentTimeMillis();
        wrapper.and(w -> w.isNull(OpenApiPermission::getStartTime)
                        .or().le(OpenApiPermission::getStartTime, currentTime))
                .and(w -> w.isNull(OpenApiPermission::getEndTime)
                        .or().ge(OpenApiPermission::getEndTime, currentTime));
        return wrapper;
    }

}


