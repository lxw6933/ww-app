package com.ww.mall.search.view.bo;

import cn.hutool.core.collection.CollectionUtil;
import lombok.Data;

import java.util.List;

/**
 * @author ww
 * @create 2024-07-26- 10:31
 * @description:
 */
@Data
public class SearchRangeBO implements BaseSearch {

    /**
     * 范围类型
     */
    private RangeType rangeType;

    /**
     * id集合
     */
    private List<Long> idList;

    @Override
    public boolean support() {
        return CollectionUtil.isNotEmpty(this.idList) && this.rangeType != null;
    }

    enum RangeType {
        SPU,
        SMS,
        CATEGORY,
        BRAND
    }

}
