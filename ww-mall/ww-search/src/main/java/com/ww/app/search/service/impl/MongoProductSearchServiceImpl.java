package com.ww.app.search.service.impl;

import com.ww.app.common.common.AppPageResult;
import com.ww.app.search.entity.mongo.ProductDoc;
import com.ww.app.search.service.MongoProductSearchService;
import com.ww.app.search.view.bo.MongoSearchPageBO;
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
public class MongoProductSearchServiceImpl implements MongoProductSearchService {

    @Override
    public AppPageResult<PortalProductSearchVO> portalProductSearch(MongoSearchPageBO mongoSearchPageBO, String curAppKey) {
        return mongoSearchPageBO.buildPageResult(productDoc -> {
            try {
                // data handler
                return searchResultDataHandler(productDoc);
            } catch (Exception e) {
                log.error("搜索商品数据异常", e);
                return new PortalProductSearchVO();
            }
        });
    }

    private PortalProductSearchVO searchResultDataHandler(ProductDoc productDoc) {
        PortalProductSearchVO portalProductSearchVO = new PortalProductSearchVO();
        // TODO other info query handler
        BeanUtils.copyProperties(productDoc, portalProductSearchVO);
        return portalProductSearchVO;
    }

}
