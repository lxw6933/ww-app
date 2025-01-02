package com.ww.app.search.view.bo;

import com.ww.app.search.enums.RangeTypeEnum;
import lombok.Data;

/**
 * @author ww
 * @create 2024-07-26- 10:31
 * @description:
 */
@Data
public class SearchRangeBO implements BaseSearch {

    private RangeTypeEnum rangeType;

    private Integer min;

    private Integer max;

    @Override
    public boolean support() {
        return this.rangeType != null && this.min != null && this.max != null && this.max > this.min;
    }

}
