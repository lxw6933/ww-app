package com.ww.mall.search.service;

import com.ww.mall.search.view.bo.PortalProductSearchBO;
import com.ww.mall.search.view.vo.PortalProductSearchVO;
import com.ww.mall.web.cmmon.MallPageResult;

/**
 * @author ww
 * @create 2024-07-25- 13:43
 * @description:
 */
public interface ProductSearchService {

    /**
     * 门户搜索
     *
     * @param portalProductSearchBO 搜索条件
     * @param curAppKey 当前搜索渠道appKey
     * @return PageResult<PortalSearchVO>
     */
    MallPageResult<PortalProductSearchVO> portalProductSearch(PortalProductSearchBO portalProductSearchBO, String curAppKey);

}
