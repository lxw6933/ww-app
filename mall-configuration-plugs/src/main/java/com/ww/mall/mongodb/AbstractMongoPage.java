package com.ww.mall.mongodb;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.ww.mall.common.common.MallPage;
import com.ww.mall.common.common.MallPageResult;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.query.Collation;
import org.springframework.data.mongodb.core.query.Criteria;

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
     * @param tClass T class
     * @return List<T>
     */
    public List<T> buildPageQueryResult(MongoTemplate mongoTemplate, Class<T> tClass) {
        // number str sort
        Collation collation = Collation.of(Locale.CHINESE).numericOrdering(true);
        // query condition
        AggregationOperation matchAggregation = Aggregation.match(this.buildQuery());
        // sort condition
        AggregationOperation sortAggregation = Aggregation.sort(this.buildSort());
        // page condition
        AggregationOperation skip = Aggregation.skip((long) (getPageNum() - 1) * getPageSize());
        AggregationOperation limit = Aggregation.limit(getPageSize());

        List<AggregationOperation> operations = ListUtil.toList(matchAggregation, sortAggregation, skip, limit);
        if (this.buildGroup() != null) {
            operations.add(buildGroup());
        }
        // build the aggregation pipeline
        Aggregation aggregation = Aggregation.newAggregation(operations)
                .withOptions(Aggregation.newAggregationOptions().collation(collation).build());
        // query aggregation data result
        AggregationResults<T> operateLogAggregationResult = mongoTemplate.aggregate(aggregation, tClass, tClass);
        return operateLogAggregationResult.getMappedResults();
    }

    /**
     * 构建查询结果总数量
     *
     * @param mongoTemplate mongoTemplate
     * @param tClass T class
     * @return long
     */
    public long buildPageQueryResultTotalCount(MongoTemplate mongoTemplate, Class<T> tClass) {
        // query condition
        AggregationOperation matchAggregation = Aggregation.match(this.buildQuery());

        List<AggregationOperation> operations = ListUtil.toList(matchAggregation);
        if (this.buildGroup() != null) {
            operations.add(buildGroup());
        }
        operations.add(Aggregation.count().as("total"));
        // totalCount
        Aggregation countAggregation = Aggregation.newAggregation(operations);

        AggregationResults<TotalCount> countResults = mongoTemplate.aggregate(countAggregation, tClass, TotalCount.class);
        return countResults.getMappedResults().isEmpty() ? 0 : countResults.getMappedResults().get(0).getTotal();
    }

    /**
     * 构建分页查询结果
     *
     * @param tClass T class
     * @param convert 目标类型转换器
     * @return MallPageResult<R>
     * @param <R> 目标类型
     */
    public <R> MallPageResult<R> buildPageResult(Class<T> tClass, Function<T, R> convert) {
        MongoTemplate mongoTemplate = SpringUtil.getBean(MongoTemplate.class);
        // query aggregation data result
        List<T> resultList = this.buildPageQueryResult(mongoTemplate, tClass);
        // query aggregation data result totalCount
        int total = (int) this.buildPageQueryResultTotalCount(mongoTemplate, tClass);
        // return
        return new MallPageResult<>(this.getPageNum(), this.getPageSize(), total, resultList, convert);
    }

    public MallPageResult<T> buildPageResult(Class<T> tClass) {
        MongoTemplate mongoTemplate = SpringUtil.getBean(MongoTemplate.class);
        // query aggregation data result
        List<T> resultList = this.buildPageQueryResult(mongoTemplate, tClass);
        // query aggregation data result totalCount
        int total = (int) this.buildPageQueryResultTotalCount(mongoTemplate, tClass);
        // return
        return new MallPageResult<>(this.getPageNum(), this.getPageSize(), total, resultList);
    }

    public static <T> String getCollectionName(Class<T> tClass) {
        if (tClass.isAnnotationPresent(Document.class)) {
            Document document = tClass.getAnnotation(Document.class);
            return document.collection();
        } else {
            throw new IllegalArgumentException("Class " + tClass.getName() + " does not have @Document annotation.");
        }
    }

    @Getter
    @Setter
    private static class TotalCount {
        private long total;
    }

}
