package com.ww.app.mongodb.common;

import cn.hutool.extra.spring.SpringUtil;
import com.ww.app.common.common.AppPage;
import com.ww.app.common.common.AppPageResult;
import com.ww.app.common.constant.Constant;
import com.ww.app.mongodb.utils.MongoUtils;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.springframework.core.GenericTypeResolver;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Collation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

/**
 * @author ww
 * @create 2024-10-25- 16:28
 * @description:
 */
public abstract class AbstractMongoPage<T> extends AppPage {

    @SuppressWarnings("all")
    protected Class<T> tClass = (Class<T>) GenericTypeResolver.resolveTypeArgument(this.getClass(), AbstractMongoPage.class);

    protected MongoTemplate mongoTemplate = SpringUtil.getBean(MongoTemplate.class);

    /**
     * 构建分页查询条件
     */
    public abstract Criteria buildQuery();

    /**
     * 构建排序条件【默认id倒叙，需自定义重写即可】
     */
    protected Sort buildSort() {
        return Sort.by(Sort.Direction.DESC, Constant.MONGO_PRIMARY_KEY);
    }

    /**
     * 构建分组条件【默认无分组条件，需自定义重写即可】
     */
    protected AggregationOperation buildGroup() {
        return null;
    }

    private FacetResult<T> buildPageQueryResult() {
        return buildPageQueryResult(null);
    }

    /**
     * 构建查询结果
     *
     * @return List<T>
     */
    private FacetResult<T> buildPageQueryResult(String collectionName) {
        // number str sort
        Collation collation = Collation.of(Locale.CHINESE).numericOrdering(true);
        // query condition
        AggregationOperation matchAggregation = Aggregation.match(this.buildQuery());
        // sort condition
        AggregationOperation sortAggregation = Aggregation.sort(this.buildSort());
        // page condition
        AggregationOperation skip = Aggregation.skip((long) (getPageNum() - 1) * getPageSize());
        AggregationOperation limit = Aggregation.limit(getPageSize());
        // count condition
        AggregationOperation countAggregation = Aggregation.count().as("totalCount");

        List<AggregationOperation> mainPipeline = new ArrayList<>();
        mainPipeline.add(matchAggregation);
        if (this.buildGroup() != null) {
            mainPipeline.add(this.buildGroup());
        }
        mainPipeline.add(sortAggregation);
        mainPipeline.add(skip);
        mainPipeline.add(limit);

        List<AggregationOperation> countPipeline = new ArrayList<>();
        countPipeline.add(matchAggregation);
        if (this.buildGroup() != null) {
            countPipeline.add(this.buildGroup());
        }
        countPipeline.add(countAggregation);

        // build FacetOperation merge mainPipeline and countPipeline
        FacetOperation facetOperation = Aggregation.facet(
                mainPipeline.toArray(new AggregationOperation[0])
        ).as("data").and(
                countPipeline.toArray(new AggregationOperation[0])
        ).as("countInfo");
        // add extra options
        AggregationOptions options = Aggregation.newAggregationOptions()
                .collation(collation)
                .allowDiskUse(true)
                .build();
        // build the aggregation pipeline
        Aggregation aggregation = Aggregation.newAggregation(facetOperation).withOptions(options);
        // query aggregation data result
        AggregationResults<Document> results = mongoTemplate.aggregate(aggregation, StringUtils.isNotBlank(collectionName) ? collectionName : MongoUtils.getCollectionName(tClass), Document.class);
        Document resultDoc = results.getUniqueMappedResult();
        if (resultDoc == null) {
            return new FacetResult<>(Collections.emptyList(), 0);
        }
        List<T> dataList = resultDoc.getList("data", tClass);
        long totalCount = resultDoc.getList("countInfo", Document.class).get(0).getLong("totalCount");
        return new FacetResult<>(dataList, (int) totalCount);
    }

    /**
     * 构建分页查询结果
     *
     * @param convert 目标类型转换器
     * @param <R>     目标类型
     * @return MallPageResult<R>
     */
    public <R> AppPageResult<R> buildPageConvertResult(String collectionName, Function<T, R> convert) {
        // query aggregation data result
        FacetResult<T> facetResult = this.buildPageQueryResult(collectionName);
        // return
        return new AppPageResult<>(this.getPageNum(), this.getPageSize(), facetResult.getTotalCount(), facetResult.getDataList(), convert);
    }

    public <R> AppPageResult<R> buildPageConvertResult(Function<T, R> convert) {
        return buildPageConvertResult(null, convert);
    }

    public AppPageResult<T> buildPageResult(String collectionName) {
        // query aggregation data result
        FacetResult<T> facetResult = this.buildPageQueryResult(collectionName);
        // return
        return new AppPageResult<>(this.getPageNum(), this.getPageSize(), facetResult.getTotalCount(), facetResult.getDataList());
    }

    public AppPageResult<T> buildPageResult() {
        return buildPageResult(null);
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    private static class FacetResult<T> {
        private List<T> dataList;
        private int totalCount;
    }

    /**
     * 普通分页查询
     *
     * @return 分页数据
     */
    public <R> AppPageResult<R> simplePageConvertResult(Function<T, R> convert) {
        return simplePageConvertResult(null, convert);
    }

    public <R> AppPageResult<R> simplePageConvertResult(String collectionName, Function<T, R> convert) {
        List<T> dataList = getSimpleDataResult(collectionName);
        long total = StringUtils.isNotBlank(collectionName) ? mongoTemplate.count(Query.query(buildQuery()), tClass, collectionName) : mongoTemplate.count(Query.query(buildQuery()), tClass);
        return new AppPageResult<>(getPageNum(), getPageSize(), (int) total, dataList, convert);
    }

    public AppPageResult<T> simplePageResult() {
        return simplePageResult(null);
    }

    public AppPageResult<T> simplePageResult(String collectionName) {
        List<T> dataList = getSimpleDataResult(collectionName);
        long total = StringUtils.isNotBlank(collectionName) ? mongoTemplate.count(Query.query(buildQuery()), tClass, collectionName) : mongoTemplate.count(Query.query(buildQuery()), tClass);
        return new AppPageResult<>(getPageNum(), getPageSize(), (int) total, dataList);
    }

    private List<T> getSimpleDataResult(String collectionName) {
        Query query = new Query()
                .addCriteria(buildQuery())
                .with(buildSort())
                .skip((long) (getPageNum() - 1) * getPageSize())
                .limit(getPageSize());
        return StringUtils.isNotBlank(collectionName) ? mongoTemplate.find(query, tClass, collectionName) : mongoTemplate.find(query, tClass);
    }

    /**
     * 普通条件查询结果
     *
     * @return 普通查询结果
     */
    public List<T> simpleQueryResult() {
        return simpleQueryResult(null);
    }

    public List<T> simpleQueryResult(String collectionName) {
        Query query = new Query()
                .addCriteria(buildQuery())
                .with(buildSort());
        return StringUtils.isNotBlank(collectionName) ? mongoTemplate.find(query, tClass, collectionName) : mongoTemplate.find(query, tClass);
    }

    /**
     * 普通条件查询结果限制结果数量
     *
     * @return 普通查询结果
     */
    public List<T> simpleQuerySizeResult() {
        return simpleQuerySizeResult(null);
    }

    public List<T> simpleQuerySizeResult(String collectionName) {
        Query query = new Query()
                .addCriteria(buildQuery())
                .with(buildSort())
                .limit(getPageSize());
        return StringUtils.isNotBlank(collectionName) ? mongoTemplate.find(query, tClass, collectionName) : mongoTemplate.find(query, tClass);
    }

}
