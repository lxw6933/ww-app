package com.ww.app.search.view.bo;

import cn.hutool.core.collection.CollectionUtil;
import com.ww.app.search.enums.ScopeTypeEnum;
import lombok.Data;

import java.util.List;

/**
 * @author ww
 * @create 2024-07-26- 10:31
 * @description:
 */
@Data
public class SearchScopeBO implements BaseSearch {

    /**
     * 范围类型
     */
    private ScopeTypeEnum rangeType;

    /**
     * 是否包含id集合
     */
    private boolean contain = true;

    /**
     * id集合
     */
    private List<Long> idList;

    @Override
    public boolean support() {
        return CollectionUtil.isNotEmpty(this.idList) && this.rangeType != null;
    }

}
