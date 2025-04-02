package com.ww.app.mongodb.common;

import cn.hutool.extra.spring.SpringUtil;
import com.ww.app.common.common.AppPage;
import com.ww.app.common.common.AppPageResult;
import com.ww.app.common.constant.Constant;
import com.ww.app.common.exception.ApiException;
import com.ww.app.mongodb.utils.MongoUtils;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
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
 * @description: MongoDB分页查询基类
 * 提供通用的MongoDB分页查询功能，支持聚合查询和简单查询
 */
@Slf4j
public abstract class AbstractMongoPage<T> extends AppPage {

    /** 目标实体类型 */
    protected final Class<T> tClass;

    /** MongoDB操作模板 */
    protected final MongoTemplate mongoTemplate;
    
    /** 中文支持的Collation配置，用于排序和比较 */
    private static final Collation CHINESE_COLLATION = Collation.of(Locale.CHINESE).numericOrdering(true);

    /**
     * 构造函数，初始化实体类型和MongoDB操作模板
     * 增加类型安全检查
     */
    @SuppressWarnings("unchecked")
    public AbstractMongoPage() {
        // 解析泛型参数
        Class<T> resolvedClass = (Class<T>) GenericTypeResolver.resolveTypeArgument(this.getClass(), AbstractMongoPage.class);
        // 类型安全检查
        if (resolvedClass == null) {
            log.error("无法解析泛型参数类型，请确保正确指定泛型类型");
            throw new ApiException("初始化MongoDB分页查询失败：无法确定泛型类型");
        }
        this.tClass = resolvedClass;
        this.mongoTemplate = SpringUtil.getBean(MongoTemplate.class);
    }

    /**
     * 构建分页查询条件（子类必须实现此方法）
     * 
     * @return MongoDB查询条件
     */
    public abstract Criteria buildQuery();

    /**
     * 构建排序条件（默认按_id降序排列）
     * 子类可重写此方法自定义排序规则
     * 
     * @return 排序条件
     */
    protected Sort buildSort() {
        return Sort.by(Sort.Direction.DESC, Constant.MONGO_PRIMARY_KEY);
    }

    /**
     * 构建分组条件（默认无分组）
     * 子类可重写此方法添加分组逻辑
     *
     * @return 分组操作，默认返回null表示无分组
     */
    protected AggregationOperation buildGroup() {
        return null;
    }

    /**
     * 构建分页查询结果（默认集合名）
     */
    private FacetResult<T> buildPageQueryResult() {
        return buildPageQueryResult(null);
    }

    /**
     * 构建分页查询结果
     * 使用MongoDB聚合管道实现分页和统计总数
     *
     * @param collectionName 集合名称，为null时使用实体类推断的集合名
     * @return 分页结果
     */
    private FacetResult<T> buildPageQueryResult(String collectionName) {
        try {
            // 构建匹配条件
            AggregationOperation matchOperation = Aggregation.match(this.buildQuery());
            
            // 构建排序条件
            AggregationOperation sortOperation = Aggregation.sort(this.buildSort());
            
            // 构建分页条件
            long skip = (long) (getPageNum() - 1) * getPageSize();
            AggregationOperation skipOperation = Aggregation.skip(skip);
            AggregationOperation limitOperation = Aggregation.limit(getPageSize());
            
            // 构建计数条件
            AggregationOperation countOperation = Aggregation.count().as("totalCount");

            // 构建数据查询管道
            List<AggregationOperation> dataPipeline = new ArrayList<>();
            dataPipeline.add(matchOperation);
            
            // 添加可选的分组操作
            AggregationOperation groupOperation = this.buildGroup();
            if (groupOperation != null) {
                dataPipeline.add(groupOperation);
            }
            
            dataPipeline.add(sortOperation);
            dataPipeline.add(skipOperation);
            dataPipeline.add(limitOperation);

            // 构建计数查询管道
            List<AggregationOperation> countPipeline = new ArrayList<>();
            countPipeline.add(matchOperation);
            
            if (groupOperation != null) {
                countPipeline.add(groupOperation);
            }
            
            countPipeline.add(countOperation);

            // 使用Facet将数据查询和计数查询合并为一次操作
            FacetOperation facetOperation = Aggregation.facet(
                    dataPipeline.toArray(new AggregationOperation[0])
            ).as("data").and(
                    countPipeline.toArray(new AggregationOperation[0])
            ).as("countInfo");
            
            // 添加聚合选项：中文排序支持和允许使用磁盘（处理大数据集）
            AggregationOptions options = Aggregation.newAggregationOptions()
                    .collation(CHINESE_COLLATION)
                    .allowDiskUse(true)
                    .build();
            
            // 构建完整的聚合管道
            Aggregation aggregation = Aggregation.newAggregation(facetOperation).withOptions(options);
            
            // 确定要查询的集合名
            String targetCollection = StringUtils.isNotBlank(collectionName) ? 
                    collectionName : MongoUtils.getCollectionName(tClass);
            
            // 执行聚合查询
            AggregationResults<Document> results = mongoTemplate.aggregate(
                    aggregation, targetCollection, Document.class);
            
            // 解析结果
            Document resultDoc = results.getUniqueMappedResult();
            if (resultDoc == null) {
                return new FacetResult<>(Collections.emptyList(), 0);
            }
            
            // 提取数据列表和总数
            List<T> dataList = resultDoc.getList("data", tClass);
            List<Document> countDocs = resultDoc.getList("countInfo", Document.class);
            
            // 安全获取总数
            int totalCount = 0;
            if (countDocs != null && !countDocs.isEmpty() && countDocs.get(0) != null) {
                totalCount = countDocs.get(0).getInteger("totalCount", 0);
            }
            
            return new FacetResult<>(dataList, totalCount);
        } catch (Exception e) {
            // 出现异常时记录日志并返回空结果
            log.error("执行MongoDB分页查询出错: {}", e.getMessage(), e);
            return new FacetResult<>(Collections.emptyList(), 0);
        }
    }

    /**
     * 构建分页查询结果并转换为目标类型
     *
     * @param collectionName 集合名称
     * @param convert 目标类型转换函数
     * @param <R> 目标类型
     * @return 分页结果
     */
    public <R> AppPageResult<R> buildPageConvertResult(String collectionName, Function<T, R> convert) {
        // 查询聚合数据结果
        FacetResult<T> facetResult = this.buildPageQueryResult(collectionName);
        // 转换并返回结果
        return new AppPageResult<>(
                this.getPageNum(), 
                this.getPageSize(), 
                facetResult.getTotalCount(), 
                facetResult.getDataList(), 
                convert
        );
    }

    /**
     * 构建分页查询结果并转换为目标类型（使用默认集合名）
     */
    public <R> AppPageResult<R> buildPageConvertResult(Function<T, R> convert) {
        return buildPageConvertResult(null, convert);
    }

    /**
     * 构建分页查询结果（不转换类型）
     */
    public AppPageResult<T> buildPageResult(String collectionName) {
        // 查询聚合数据结果
        FacetResult<T> facetResult = this.buildPageQueryResult(collectionName);
        // 返回结果
        return new AppPageResult<>(
                this.getPageNum(), 
                this.getPageSize(), 
                facetResult.getTotalCount(), 
                facetResult.getDataList()
        );
    }

    /**
     * 构建分页查询结果（使用默认集合名）
     */
    public AppPageResult<T> buildPageResult() {
        return buildPageResult(null);
    }

    /**
     * Facet查询结果包装类
     * 包含数据列表和总数
     */
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    private static class FacetResult<T> {
        /** 数据列表 */
        private List<T> dataList;
        /** 总数 */
        private int totalCount;
    }

    /**
     * 使用简单查询实现分页（性能更好但功能更简单）
     * 转换为目标类型
     *
     * @param convert 类型转换函数
     * @param <R> 目标类型
     * @return 分页结果
     */
    public <R> AppPageResult<R> simplePageConvertResult(Function<T, R> convert) {
        return simplePageConvertResult(null, convert);
    }

    /**
     * 使用简单查询实现分页并转换为目标类型
     */
    public <R> AppPageResult<R> simplePageConvertResult(String collectionName, Function<T, R> convert) {
        try {
            // 获取分页数据
            List<T> dataList = getSimpleDataResult(collectionName);
            
            // 构建计数查询
            Criteria queryCriteria = buildQuery();
            long total;
            
            // 根据集合名是否提供执行不同的计数查询
            if (StringUtils.isNotBlank(collectionName)) {
                total = mongoTemplate.count(Query.query(queryCriteria), tClass, collectionName);
            } else {
                total = mongoTemplate.count(Query.query(queryCriteria), tClass);
            }
            
            // 构建并返回分页结果
            return new AppPageResult<>(getPageNum(), getPageSize(), (int) total, dataList, convert);
        } catch (Exception e) {
            log.error("执行简单分页查询出错: {}", e.getMessage(), e);
            return new AppPageResult<>(getPageNum(), getPageSize(), 0, Collections.emptyList(), convert);
        }
    }

    /**
     * 使用简单查询实现分页（默认集合名）
     */
    public AppPageResult<T> simplePageResult() {
        return simplePageResult(null);
    }

    /**
     * 使用简单查询实现分页
     */
    public AppPageResult<T> simplePageResult(String collectionName) {
        try {
            List<T> dataList = getSimpleDataResult(collectionName);
            
            // 执行计数查询
            long total;
            if (StringUtils.isNotBlank(collectionName)) {
                total = mongoTemplate.count(Query.query(buildQuery()), tClass, collectionName);
            } else {
                total = mongoTemplate.count(Query.query(buildQuery()), tClass);
            }
            
            return new AppPageResult<>(getPageNum(), getPageSize(), (int) total, dataList);
        } catch (Exception e) {
            log.error("执行简单分页查询出错: {}", e.getMessage(), e);
            return new AppPageResult<>(getPageNum(), getPageSize(), 0, Collections.emptyList());
        }
    }

    /**
     * 获取简单查询的数据结果
     */
    public List<T> getSimpleDataResult(String collectionName) {
        return getSimpleDataResult(collectionName, getPageNum(), getPageSize());
    }

    /**
     * 获取简单查询的数据结果（指定页码和页大小）
     * 
     * @param collectionName 集合名称
     * @param pageNumber 页码
     * @param pageSize 每页大小
     * @return 查询结果
     */
    public List<T> getSimpleDataResult(String collectionName, int pageNumber, int pageSize) {
        try {
            // 构建查询对象
            Query query = new Query()
                    .addCriteria(buildQuery())
                    .with(buildSort())
                    .skip((long) (pageNumber - 1) * pageSize)
                    .limit(pageSize);
            
            // 设置中文排序支持
            query.collation(CHINESE_COLLATION);
            
            // 执行查询
            if (StringUtils.isNotBlank(collectionName)) {
                return mongoTemplate.find(query, tClass, collectionName);
            } else {
                return mongoTemplate.find(query, tClass);
            }
        } catch (Exception e) {
            log.error("获取简单查询数据出错: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * 普通条件查询（不分页）
     */
    public List<T> simpleQueryResult() {
        return simpleQueryResult(null);
    }

    /**
     * 普通条件查询（指定集合）
     */
    public List<T> simpleQueryResult(String collectionName) {
        try {
            Query query = new Query()
                    .addCriteria(buildQuery())
                    .with(buildSort());
            
            // 设置中文排序支持
            query.collation(CHINESE_COLLATION);
            
            if (StringUtils.isNotBlank(collectionName)) {
                return mongoTemplate.find(query, tClass, collectionName);
            } else {
                return mongoTemplate.find(query, tClass);
            }
        } catch (Exception e) {
            log.error("执行简单查询出错: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * 普通条件查询并限制结果数量
     */
    public List<T> simpleQuerySizeResult() {
        return simpleQuerySizeResult(null);
    }

    /**
     * 普通条件查询并限制结果数量（指定集合）
     */
    public List<T> simpleQuerySizeResult(String collectionName) {
        try {
            Query query = new Query()
                    .addCriteria(buildQuery())
                    .with(buildSort())
                    .limit(getPageSize());
            
            // 设置中文排序支持
            query.collation(CHINESE_COLLATION);
            
            if (StringUtils.isNotBlank(collectionName)) {
                return mongoTemplate.find(query, tClass, collectionName);
            } else {
                return mongoTemplate.find(query, tClass);
            }
        } catch (Exception e) {
            log.error("执行限制大小查询出错: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }
}
