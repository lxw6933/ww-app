package com.ww.app.search.view.bo;

import com.ww.app.search.enums.SortTypeEnum;
import lombok.Data;

/**
 * @author ww
 * @create 2024-07-26- 10:31
 * @description:
 */
@Data
public class SearchSortBO implements BaseSearch {

    /**
     * 排序类型
     */
    private SortTypeEnum sortType;

    /**
     * true：顺序  false：倒叙
     */
    private Boolean sort;

    @Override
    public boolean support() {
        return this.sortType!= null && this.sort != null;
    }

}
