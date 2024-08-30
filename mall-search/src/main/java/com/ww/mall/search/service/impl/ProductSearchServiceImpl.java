package com.ww.mall.search.service.impl;

import com.ww.mall.search.entity.ProductSearch;
import com.ww.mall.search.service.ProductSearchService;
import com.ww.mall.search.view.bo.PortalProductSearchBO;
import com.ww.mall.search.view.vo.PortalProductSearchVO;
import com.ww.mall.web.cmmon.MallPageResult;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.beans.BeanUtils;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Collation;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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
    public MallPageResult<PortalProductSearchVO> portalProductSearch(PortalProductSearchBO portalProductSearchBO, String curAppKey) {
        String collectionName = "v2_product_search";
        boolean integralChannel = true;
        // number str sort
        Collation collation = Collation.of(Locale.CHINESE).numericOrdering(true);
        // query condition
        AggregationOperation matchAggregation = Aggregation.match(portalProductSearchBO.buildQueryCriteria());
        // sort condition
        AggregationOperation sortAggregation = Aggregation.sort(portalProductSearchBO.buildSort(integralChannel));
        // group condition
        AggregationOperation groupAggregation = portalProductSearchBO.buildGroup();
        // page condition
        AggregationOperation skip = Aggregation.skip((long) (portalProductSearchBO.getPageNum() - 1) * portalProductSearchBO.getPageSize());
        AggregationOperation limit = Aggregation.limit(portalProductSearchBO.getPageSize());
        // build the aggregation pipeline
        Aggregation aggregation = Aggregation.newAggregation(matchAggregation, sortAggregation, groupAggregation, skip, limit)
                .withOptions(Aggregation.newAggregationOptions().collation(collation).build());
        // query aggregation data result
        AggregationResults<ProductSearch> productSearchAggregationResult = mongoTemplate.aggregate(aggregation, collectionName, ProductSearch.class);
        List<ProductSearch> productSearchResult = productSearchAggregationResult.getMappedResults();
        // totalCount
        Aggregation countAggregation = Aggregation.newAggregation(matchAggregation, groupAggregation, Aggregation.count().as("totalCount"));
        AggregationResults<Document> countResults = mongoTemplate.aggregate(countAggregation, collectionName, Document.class);
        int total = !countResults.getMappedResults().isEmpty() ? countResults.getMappedResults().get(0).getInteger("totalCount") : 0;

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
        return null;
        // 未引入mybatis-plus
//        return new MallPageResult<>(new MallPage(portalProductSearchBO.getPageNum(), portalProductSearchBO.getPageSize()), resultVOList, total);
    }

    private PortalProductSearchVO searchResultDataHandler(ProductSearch productSearch) {
        PortalProductSearchVO portalProductSearchVO = new PortalProductSearchVO();
        // TODO other info query handler
        BeanUtils.copyProperties(productSearch, portalProductSearchVO);
        return portalProductSearchVO;
    }

}
