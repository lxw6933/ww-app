package com.ww.app.common.enums;

import lombok.AllArgsConstructor;

/**
 * @author ww
 * @create 2024-05-24 21:05
 * @description:
 */
@AllArgsConstructor
public enum SensitiveWordHandlerType {
    /**
     * 抛出异常
     */
    EXCEPTION,

    /**
     * 替换/脱敏
     */
    REPLACE;
}

