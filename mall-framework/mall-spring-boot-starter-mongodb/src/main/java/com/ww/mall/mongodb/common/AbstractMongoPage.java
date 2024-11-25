package com.ww.mall.mongodb.common;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.ww.mall.common.common.MallPage;
import com.ww.mall.common.common.MallPageResult;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.bson.Document;
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
public abstract class AbstractMongoPage<T> extends MallPage {

    /**
     * 构建分页查询条件
     */
    public abstract Criteria buildQuery();

    /**
     * 构建分页排序条件
     */
    public abstract Sort buildSort();

    /**
     * 构建分组条件
     */
    protected AggregationOperation buildGroup() {
        return null;
    }

    /**
     * 构建查询结果
     *
     * @param mongoTemplate mongoTemplate
     * @param tClass        T class
     * @return List<T>
     */
    private FacetResult<T> buildPageQueryResult(MongoTemplate mongoTemplate, Class<T> tClass) {
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
        AggregationResults<Document> results = mongoTemplate.aggregate(aggregation, getCollectionName(tClass), Document.class);
        Document resultDoc = results.getUniqueMappedResult();
        if (resultDoc == null) {
            return new FacetResult<>(Collections.emptyList(), 0);
        }
        List<T> dataList = resultDoc.getList("data", tClass);
        long totalCount = resultDoc.getList("countInfo", Document.class).get(0).getLong("totalCount");
        return new FacetResult<>(dataList, (int) totalCount);
    }

    /**
     * 构建查询结果总数量
     *
     * @param mongoTemplate mongoTemplate
     * @param tClass        T class
     * @return long
     */
    @Deprecated
    private long buildPageQueryResultTotalCount(MongoTemplate mongoTemplate, Class<T> tClass) {
        // query condition
        AggregationOperation matchAggregation = Aggregation.match(this.buildQuery());

        List<AggregationOperation> operations = ListUtil.toList(matchAggregation);
        if (this.buildGroup() != null) {
            operations.add(buildGroup());
        }
        operations.add(Aggregation.count().as("totalCount"));
        // totalCount
        Aggregation countAggregation = Aggregation.newAggregation(operations);

        AggregationResults<TotalCount> countResults = mongoTemplate.aggregate(countAggregation, tClass, TotalCount.class);
        return countResults.getMappedResults().isEmpty() ? 0 : countResults.getMappedResults().get(0).getTotal();
    }

    /**
     * 构建分页查询结果
     *
     * @param tClass  T class
     * @param convert 目标类型转换器
     * @param <R>     目标类型
     * @return MallPageResult<R>
     */
    public <R> MallPageResult<R> buildPageResult(Class<T> tClass, Function<T, R> convert) {
        MongoTemplate mongoTemplate = SpringUtil.getBean(MongoTemplate.class);
        // query aggregation data result
        FacetResult<T> facetResult = this.buildPageQueryResult(mongoTemplate, tClass);
        // return
        return new MallPageResult<>(this.getPageNum(), this.getPageSize(), facetResult.getTotalCount(), facetResult.getDataList(), convert);
    }

    public MallPageResult<T> buildPageResult(Class<T> tClass) {
        MongoTemplate mongoTemplate = SpringUtil.getBean(MongoTemplate.class);
        // query aggregation data result
        FacetResult<T> facetResult = this.buildPageQueryResult(mongoTemplate, tClass);
        // return
        return new MallPageResult<>(this.getPageNum(), this.getPageSize(), facetResult.getTotalCount(), facetResult.getDataList());
    }

    public static <T> String getCollectionName(Class<T> tClass) {
        if (tClass.isAnnotationPresent(org.springframework.data.mongodb.core.mapping.Document.class)) {
            org.springframework.data.mongodb.core.mapping.Document document = tClass.getAnnotation(org.springframework.data.mongodb.core.mapping.Document.class);
            return document.collection();
        } else {
            throw new IllegalArgumentException("Class " + tClass.getName() + " does not have @Document annotation.");
        }
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    private static class TotalCount {
        private long total;
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
     * @param tClass T class
     * @return 分页数据
     */
    public MallPageResult<T> simplePageResult(Class<T> tClass) {
        MongoTemplate mongoTemplate = SpringUtil.getBean(MongoTemplate.class);
        Query query = new Query()
                .addCriteria(buildQuery())
                .with(buildSort())
                .skip((long) (getPageNum() - 1) * getPageSize())
                .limit(getPageSize());
        // 获取分页数据
        List<T> dataList = mongoTemplate.find(query, tClass);
        // 获取总记录数
        long total = mongoTemplate.count(Query.query(buildQuery()), tClass);
        return new MallPageResult<>(getPageNum(), getPageSize(), (int) total, dataList);
    }

}
