package com.ww.mall.coupon.eunms;

import lombok.Getter;

import java.util.StringJoiner;

/**
 * @author ww
 * @create 2023-07-25- 09:50
 * @description:
 */
@Getter
public enum ApplyProductRangeType {

    ALL("全部商品"),
    PRODUCT_GROUP("指定商品分组"),
    SPECIFY_PRODUCT("指定商品"),
    SPECIFY_BRAND("指定商品品牌"),
    SPECIFY_CATEGORY("指定商品分类"),
    EXCLUDE_PRODUCT("排除商品");

    private final String text;

    ApplyProductRangeType(String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", name() + "[", "]")
                .add(text)
                .toString();
    }

}
