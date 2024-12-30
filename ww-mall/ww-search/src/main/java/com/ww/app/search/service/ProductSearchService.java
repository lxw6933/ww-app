package com.ww.app.search.service;

import com.ww.app.search.view.bo.PortalProductPageBO;
import com.ww.app.search.view.vo.PortalProductSearchVO;
import com.ww.app.common.common.AppPageResult;

/**
 * @author ww
 * @create 2024-07-25- 13:43
 * @description:
 */
public interface ProductSearchService {

    /**
     * 门户搜索
     *
     * @param portalProductPageBO 搜索条件
     * @param curAppKey 当前搜索渠道appKey
     * @return PageResult<PortalSearchVO>
     */
    AppPageResult<PortalProductSearchVO> portalProductSearch(PortalProductPageBO portalProductPageBO, String curAppKey);

}
