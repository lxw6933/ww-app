package com.ww.app.search.service.impl;

import com.ww.app.common.common.AppPageResult;
import com.ww.app.search.entity.es.ProductDoc;
import com.ww.app.search.service.ProductSearchService;
import com.ww.app.search.view.dto.ProductSearchPageDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author ww
 * @create 2025-01-02- 11:04
 * @description:
 */
@Slf4j
@Service
public class ProductSearchServiceImpl implements ProductSearchService {

    @Resource
    private ElasticsearchRestTemplate elasticsearchRestTemplate;

    @Override
    public AppPageResult<ProductDoc> search(ProductSearchPageDTO dto) {
        // 构建查询条件
        NativeSearchQueryBuilder queryBuilder = dto.buildQuery();
        // 执行查询
        SearchHits<ProductDoc> searchHits = elasticsearchRestTemplate.search(queryBuilder.build(), ProductDoc.class);
        // 构建返回结果
        List<ProductDoc> results = searchHits.stream()
                .map(SearchHit::getContent)
                .collect(Collectors.toList());
        return new AppPageResult<>(dto.getPageNum(), dto.getPageSize(), Math.toIntExact(searchHits.getTotalHits()), results);
    }

}
