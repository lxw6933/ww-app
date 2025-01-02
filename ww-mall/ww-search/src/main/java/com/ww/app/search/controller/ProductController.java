package com.ww.app.search.controller;

import com.ww.app.common.common.AppPageResult;
import com.ww.app.search.entity.Product;
import com.ww.app.search.entity.es.ProductDoc;
import com.ww.app.search.service.MongoProductSearchService;
import com.ww.app.search.service.ProductSearchService;
import com.ww.app.search.view.bo.MongoSearchPageBO;
import com.ww.app.search.view.dto.ProductSearchPageDTO;
import com.ww.app.search.view.vo.PortalProductSearchVO;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author ww
 * @create 2023-08-14- 10:54
 * @description:
 */
@RestController
@RequestMapping("/product")
public class ProductController {

    @Resource
    private ElasticsearchRestTemplate elasticsearchRestTemplate;

    @Resource
    private MongoProductSearchService mongoProductSearchService;

    @Resource
    private ProductSearchService productSearchService;

    @PostMapping("/portal/mongo/product/search")
    public AppPageResult<PortalProductSearchVO> portalProductSearch(@RequestBody @Validated MongoSearchPageBO mongoSearchPageBO, @RequestHeader("appkey") String appKey) {
        return mongoProductSearchService.portalProductSearch(mongoSearchPageBO, appKey);
    }

    @GetMapping("/portal/es/product/search")
    public AppPageResult<ProductDoc> search(ProductSearchPageDTO query) {
        return productSearchService.search(query);
    }

    @GetMapping("/query")
    public List<Product> queryProduct(String name) {
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery()
                .should(QueryBuilders.matchQuery("title", name))
                .should(QueryBuilders.matchQuery("productSkuAttrs", name))
                .should(QueryBuilders.matchQuery("secondTitle", name));
        // 参数定义了至少满足几个子句
        boolQueryBuilder.minimumShouldMatch(2);
        // 构建高亮查询
        NativeSearchQuery searchQuery = new NativeSearchQueryBuilder()
                .withQuery(boolQueryBuilder)
                .withHighlightFields(
                        new HighlightBuilder.Field("title")
                        , new HighlightBuilder.Field("secondTitle")
                        , new HighlightBuilder.Field("productSkuAttrs"))
                .withHighlightBuilder(new HighlightBuilder().preTags("<span style='color:red'>").postTags("</span>"))
                .build();
        // 查询
        SearchHits<Product> search = elasticsearchRestTemplate.search(searchQuery, Product.class);
        // 得到查询返回的内容
        List<SearchHit<Product>> searchHits = search.getSearchHits();
        // 设置一个最后需要返回的实体类集合
        List<Product> productList = new ArrayList<>();
        // 遍历返回的内容进行处理
        for (SearchHit<Product> searchHit : searchHits) {
            // 高亮的内容
            Map<String, List<String>> highlightFields = searchHit.getHighlightFields();
            // 将高亮的内容填充到content中
            searchHit.getContent().setTitle(highlightFields.get("title") == null ? searchHit.getContent().getTitle() : highlightFields.get("title").get(0));
            searchHit.getContent().setSecondTitle(highlightFields.get("secondTitle") == null ? searchHit.getContent().getSecondTitle() : highlightFields.get("secondTitle").get(0));
            searchHit.getContent().setProductSkuAttrs(highlightFields.get("productSkuAttrs") == null ? searchHit.getContent().getProductSkuAttrs() : highlightFields.get("productSkuAttrs").get(0));
            // 放到实体类中
            productList.add(searchHit.getContent());
        }
        return productList;
    }

}
