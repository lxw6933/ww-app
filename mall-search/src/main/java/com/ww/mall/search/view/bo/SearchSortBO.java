package com.ww.mall.search.view.bo;

import lombok.Data;

/**
 * @author ww
 * @create 2024-07-26- 10:31
 * @description:
 */
@Data
public class SearchSortBO {

    /**
     * 排序类型
     */
    private SortType sortType;

    /**
     * true：顺序  false：倒叙
     */
    private Boolean sort;

    enum SortType {
        INTEGRAL,
        SALE_NUMBER,
        TIME,
        PRICE
    }

}
