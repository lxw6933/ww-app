package com.ww.app.open.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * API状态枚举
 * 
 * @author ww
 * @create 2024-05-27
 */
@Getter
@AllArgsConstructor
public enum ApiStatus {
    
    /**
     * 开发中
     */
    DEVELOPING(0, "开发中"),
    
    /**
     * 已发布
     */
    PUBLISHED(1, "已发布"),
    
    /**
     * 已下线
     */
    OFFLINE(2, "已下线"),
    
    /**
     * 已废弃
     */
    DEPRECATED(3, "已废弃");

    private final Integer code;
    private final String desc;
}




