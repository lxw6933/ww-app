package com.ww.mall.search.view.bo;

import lombok.Data;

import java.util.List;

/**
 * @author ww
 * @create 2024-07-26- 10:31
 * @description:
 */
@Data
public class SearchRangeBO {

    /**
     * 范围类型
     */
    private RangeType rangeType;

    /**
     * id集合
     */
    private List<Long> idList;

    enum RangeType {
        SPU,
        SMS,
        CATEGORY,
        BRAND
    }

}
