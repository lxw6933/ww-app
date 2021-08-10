package com.ww.mall.enums;

/**
 * @description: 字典枚举
 * @author: ww
 * @create: 2021-05-18 17:56
 */
public enum DictTypeEnum {

    /**
     * 国籍
     */
    COUNTRY("country"),

    /**
     * 民族
     */
    NATION("nation"),

    /**
     * 证件类型
     */
    ID_TYPE("idType");

    private String code;

    DictTypeEnum(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

}
