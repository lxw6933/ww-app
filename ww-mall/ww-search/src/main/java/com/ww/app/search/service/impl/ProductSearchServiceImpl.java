package com.ww.app.search.service.impl;

import com.ww.app.common.common.AppPageResult;
import com.ww.app.search.entity.ProductSearch;
import com.ww.app.search.service.ProductSearchService;
import com.ww.app.search.view.bo.PortalProductPageBO;
import com.ww.app.search.view.vo.PortalProductSearchVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

/**
 * @author ww
 * @create 2024-07-25- 13:48
 * @description:
 */
@Slf4j
@Service
public class ProductSearchServiceImpl implements ProductSearchService {

    @Override
    public AppPageResult<PortalProductSearchVO> portalProductSearch(PortalProductPageBO portalProductPageBO, String curAppKey) {
        return portalProductPageBO.buildPageResult(ProductSearch.class, productSearch -> {
            try {
                // data handler
                return searchResultDataHandler(productSearch);
            } catch (Exception e) {
                log.error("搜索商品数据异常", e);
                return new PortalProductSearchVO();
            }
        });
    }

    private PortalProductSearchVO searchResultDataHandler(ProductSearch productSearch) {
        PortalProductSearchVO portalProductSearchVO = new PortalProductSearchVO();
        // TODO other info query handler
        BeanUtils.copyProperties(productSearch, portalProductSearchVO);
        return portalProductSearchVO;
    }

}
