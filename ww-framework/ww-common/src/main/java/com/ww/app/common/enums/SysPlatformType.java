package com.ww.app.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * @author ww
 * @create 2024-05-20- 09:38
 * @description:
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public enum SysPlatformType {

    BOSS("BOSS平台"),
    OPERATION("运营平台"),
    MERCHANT("商家平台");

    private String typeName;

}
