package com.ww.app.search.service;

import com.ww.app.search.view.bo.MongoSearchPageBO;
import com.ww.app.search.view.vo.PortalProductSearchVO;
import com.ww.app.common.common.AppPageResult;

/**
 * @author ww
 * @create 2024-07-25- 13:43
 * @description:
 */
public interface MongoProductSearchService {

    /**
     * 门户搜索
     *
     * @param mongoSearchPageBO 搜索条件
     * @param curAppKey 当前搜索渠道appKey
     * @return PageResult<PortalSearchVO>
     */
    AppPageResult<PortalProductSearchVO> portalProductSearch(MongoSearchPageBO mongoSearchPageBO, String curAppKey);

}
