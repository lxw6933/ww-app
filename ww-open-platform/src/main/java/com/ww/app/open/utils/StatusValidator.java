package com.ww.app.open.utils;

import com.ww.app.common.enums.GlobalResCodeConstants;
import com.ww.app.common.exception.ApiException;
import com.ww.app.open.constant.OpenPlatformConstants;
import com.ww.app.open.entity.OpenApplication;
import com.ww.app.open.entity.OpenApiInfo;
import com.ww.app.open.enums.ApiStatus;
import com.ww.app.open.enums.ApplicationStatus;

/**
 * 状态验证工具类
 * 
 * @author ww
 * @create 2024-05-27
 * @description: 统一管理状态验证逻辑，避免重复代码
 */
public class StatusValidator {

    private StatusValidator() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * 验证应用状态是否启用
     * 
     * @param application 应用信息
     * @throws ApiException 如果应用未启用
     */
    public static void validateApplicationEnabled(OpenApplication application) {
        if (application == null) {
            throw new ApiException(GlobalResCodeConstants.BAD_REQUEST.getCode(), "应用不存在");
        }
        
        Integer status = application.getStatus();
        if (!ApplicationStatus.ENABLED.getCode().equals(status)) {
            throw new ApiException(GlobalResCodeConstants.FORBIDDEN.getCode(), "应用未启用");
        }
    }

    /**
     * 验证API状态是否已发布
     * 
     * @param apiInfo API信息
     * @throws ApiException 如果API未发布
     */
    public static void validateApiPublished(OpenApiInfo apiInfo) {
        if (apiInfo == null) {
            throw new ApiException(GlobalResCodeConstants.NOT_FOUND.getCode(), "API不存在");
        }
        
        Integer status = apiInfo.getStatus();
        if (!ApiStatus.PUBLISHED.getCode().equals(status)) {
            throw new ApiException(GlobalResCodeConstants.FORBIDDEN.getCode(), "API未发布");
        }
    }

    /**
     * 验证权限状态是否启用
     * 
     * @param status 权限状态
     * @return true-启用，false-禁用
     */
    public static boolean isPermissionEnabled(Integer status) {
        return OpenPlatformConstants.STATUS_ENABLED.equals(status);
    }

    /**
     * 验证配置是否可编辑
     * 
     * @param editable 可编辑标识
     * @return true-可编辑，false-不可编辑
     */
    public static boolean isConfigEditable(Integer editable) {
        return OpenPlatformConstants.STATUS_ENABLED.equals(editable);
    }
}

