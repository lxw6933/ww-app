package com.ww.mall.search.service.impl;

import com.ww.mall.common.common.MallPageResult;
import com.ww.mall.search.entity.ProductSearch;
import com.ww.mall.search.service.ProductSearchService;
import com.ww.mall.search.view.bo.PortalProductPageBO;
import com.ww.mall.search.view.vo.PortalProductSearchVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * @author ww
 * @create 2024-07-25- 13:48
 * @description:
 */
@Slf4j
@Service
public class ProductSearchServiceImpl implements ProductSearchService {

    @Resource
    private MongoTemplate mongoTemplate;

    @Override
    public MallPageResult<PortalProductSearchVO> portalProductSearch(PortalProductPageBO portalProductPageBO, String curAppKey) {
        // query aggregation data result
        List<ProductSearch> productSearchResult = portalProductPageBO.buildPageQueryResult(mongoTemplate, ProductSearch.class);
        // query aggregation data result totalCount
        int total = (int) portalProductPageBO.buildPageQueryResultTotalCount(mongoTemplate, ProductSearch.class);

        List<PortalProductSearchVO> resultVOList = new ArrayList<>();
        productSearchResult.forEach(res -> {
            try {
                // data handler
                PortalProductSearchVO resultVO = searchResultDataHandler(res);
                resultVOList.add(resultVO);
            } catch (Exception e) {
                log.error("搜索商品数据异常", e);
            }
        });
        return new MallPageResult<>(portalProductPageBO.getPageNum(), portalProductPageBO.getPageSize(), total, resultVOList);
    }

    private PortalProductSearchVO searchResultDataHandler(ProductSearch productSearch) {
        PortalProductSearchVO portalProductSearchVO = new PortalProductSearchVO();
        // TODO other info query handler
        BeanUtils.copyProperties(productSearch, portalProductSearchVO);
        return portalProductSearchVO;
    }

}
