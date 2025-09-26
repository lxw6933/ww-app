package com.ww.app.ip.enums;

import com.ww.app.common.enums.BaseEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author ww
 * @create 2025-09-25 15:38
 * @description:
 */
@Getter
@AllArgsConstructor
public enum AreaTypeEnum implements BaseEnum {

    COUNTRY(1, "国家"),
    PROVINCE(2, "省份"),
    CITY(3, "城市"),
    // 县、镇、区等
    DISTRICT(4, "地区");

    /**
     * 类型
     */
    private final Integer type;

    /**
     * 名字
     */
    private final String name;

    @Override
    public String getShowValue() {
        return this.name;
    }
}
