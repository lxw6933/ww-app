package com.ww.app.open.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 应用状态枚举
 * 
 * @author ww
 * @create 2024-05-27
 */
@Getter
@AllArgsConstructor
public enum ApplicationStatus {
    
    /**
     * 待审核
     */
    PENDING(0, "待审核"),
    
    /**
     * 已启用
     */
    ENABLED(1, "已启用"),
    
    /**
     * 已禁用
     */
    DISABLED(2, "已禁用"),
    
    /**
     * 已拒绝
     */
    REJECTED(3, "已拒绝");

    private final Integer code;
    private final String desc;
}


