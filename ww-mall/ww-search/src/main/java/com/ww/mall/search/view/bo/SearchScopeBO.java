package com.ww.mall.search.view.bo;

import lombok.Data;

/**
 * @author ww
 * @create 2024-07-26- 10:31
 * @description:
 */
@Data
public class SearchScopeBO implements BaseSearch {

    private ScopeType scopeType;

    private Integer min;

    private Integer max;

    @Override
    public boolean support() {
        return this.scopeType != null && this.min != null && this.max != null && this.max > this.min;
    }

    enum ScopeType {
        INTEGRAL,
        PRICE
    }

}
