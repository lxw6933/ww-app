package com.ww.app.search.view.dto;

import com.ww.app.common.common.AppPage;
import com.ww.app.common.exception.ApiException;
import com.ww.app.common.utils.CollectionUtils;
import com.ww.app.search.view.bo.SearchRangeBO;
import com.ww.app.search.view.bo.SearchScopeBO;
import com.ww.app.search.view.bo.SearchSortBO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;

import java.util.List;

/**
 * @author ww
 * @create 2025-01-02- 11:01
 * @description:
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ProductSearchPageDTO extends AppPage {

    private String keyword;

    private SearchSortBO searchSortBO;

    private SearchRangeBO searchRangeBO;

    private SearchScopeBO searchScopeBO;

    public NativeSearchQueryBuilder buildQuery() {
        // 构建bool查询
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        // 全文搜索
        if (StringUtils.isNotBlank(this.keyword)) {
            boolQuery.must(QueryBuilders.multiMatchQuery(this.keyword, "name", "desc"));
        }
        // 数据域过滤
        if (this.searchScopeBO != null && this.searchScopeBO.support()) {
            List<String> idStrList = CollectionUtils.convertList(this.searchScopeBO.getIdList(), Object::toString);
            switch (this.searchScopeBO.getRangeType()) {
                case SMS:
                    boolQuery.filter(QueryBuilders.termQuery("smsId", idStrList));
                    break;
                case SPU:
                    boolQuery.filter(QueryBuilders.termQuery("productId", idStrList));
                    break;
                case BRAND:
                    boolQuery.filter(QueryBuilders.termQuery("brandId", idStrList));
                    break;
                case CATEGORY:
                    boolQuery.filter(QueryBuilders.termQuery("categoryId", idStrList));
                    break;
                default:
                    throw new ApiException("不支持" + this.searchScopeBO.getRangeType() + "类型");
            }
        }
        // 数据范围过滤
        if (this.searchRangeBO != null && this.searchRangeBO.support()) {
            RangeQueryBuilder rangeQuery;
            switch (this.searchRangeBO.getRangeType()) {
                case INTEGRAL:
                    rangeQuery = QueryBuilders.rangeQuery("integral");
                    break;
                case PRICE:
                    rangeQuery = QueryBuilders.rangeQuery("price");
                    break;
                default:
                    throw new ApiException("不支持" + this.searchRangeBO.getRangeType() + "类型");
            }
            rangeQuery.gte(this.searchRangeBO.getMin());
            rangeQuery.lte(this.searchRangeBO.getMax());
            boolQuery.filter(rangeQuery);
        }
        // 构建查询
        NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder()
                .withQuery(boolQuery)
                .withPageable(PageRequest.of(this.getPageNum(), this.getPageSize()));
        // 数据排序
        if (this.searchSortBO != null && this.searchSortBO.support()) {
            FieldSortBuilder fieldSortBuilder;
            switch (this.searchSortBO.getSortType()) {
                case INTEGRAL:
                    fieldSortBuilder = SortBuilders.fieldSort("integral");
                    break;
                case PRICE:
                    fieldSortBuilder = SortBuilders.fieldSort("price");
                    break;
                case TIME:
                    fieldSortBuilder = SortBuilders.fieldSort("updatedAt");
                    break;
                case SALE_NUMBER:
                    fieldSortBuilder = SortBuilders.fieldSort("salesCount");
                    break;
                default:
                    throw new ApiException("不支持" + this.searchSortBO.getSortType() + "类型");
            }
            SortOrder order = this.searchSortBO.getSort() ? SortOrder.ASC : SortOrder.DESC;
            queryBuilder.withSorts(fieldSortBuilder.order(order));
        } else {
            queryBuilder.withSorts(SortBuilders.fieldSort("price").order(SortOrder.ASC));
        }
        return queryBuilder;
    }

}
