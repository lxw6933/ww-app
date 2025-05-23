package com.ww.app.mongodb.utils;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.ww.app.common.constant.Constant;
import com.ww.app.common.exception.ApiException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author ww
 * @create 2024-12-19- 13:45
 * @description: MongoDB工具类 提供MongoDB常用操作的工具方法
 */
@Slf4j
public class MongoUtils {

    /**
     * 默认分页大小
     */
    private static final int DEFAULT_PAGE_SIZE = 20;

    /**
     * 集合名称缓存
     */
    private static final ConcurrentHashMap<Class<?>, String> COLLECTION_NAME_CACHE = new ConcurrentHashMap<>();

    /**
     * MongoTemplate实例
     */
    private static volatile MongoTemplate mongoTemplate;

    private MongoUtils() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * 获取MongoTemplate实例
     *
     * @return MongoTemplate实例
     */
    public static MongoTemplate getMongoTemplate() {
        if (mongoTemplate == null) {
            synchronized (MongoUtils.class) {
                if (mongoTemplate == null) {
                    log.info("初始化MongoTemplate实例");
                    mongoTemplate = SpringUtil.getBean(MongoTemplate.class);
                }
            }
        }
        return mongoTemplate;
    }

    /**
     * 获取集合名称
     *
     * @param entityClass 实体类
     * @return 集合名称
     */
    public static <T> String getCollectionName(Class<T> entityClass) {
        if (entityClass == null) {
            throw new IllegalArgumentException("实体类不能为空");
        }
        return COLLECTION_NAME_CACHE.computeIfAbsent(entityClass, clazz -> {
            if (!clazz.isAnnotationPresent(Document.class)) {
                throw new IllegalArgumentException("Class " + clazz.getName() + " does not have @Document annotation.");
            }
            Document document = clazz.getAnnotation(Document.class);
            return StringUtils.isBlank(document.value()) ? document.collection() : document.value();
        });
    }

    /**
     * 通用游标查询方法
     *
     * @param query          查询条件
     * @param cursorField    游标字段
     * @param cursorValue    游标值
     * @param pageSize       每页大小
     * @param sort          排序方向
     * @param fieldNames    返回字段
     * @param entityClass   返回数据类型
     * @param collectionName 集合名称
     * @return 查询结果列表
     */
    private static <T> List<T> doQueryByCursor(Query query,
                                            String cursorField,
                                            Object cursorValue,
                                            int pageSize,
                                            Sort.Direction sort,
                                            List<String> fieldNames,
                                            Class<T> entityClass,
                                            String collectionName) {
        // 参数校验
        validateQueryParams(query, cursorField, pageSize, entityClass);

        // 添加游标条件
        if (cursorValue != null) {
            ObjectId target = new ObjectId(cursorValue.toString());
            switch (sort) {
                case ASC:
                    query.addCriteria(Criteria.where(cursorField).gt(target));
                    break;
                case DESC:
                    query.addCriteria(Criteria.where(cursorField).lt(target));
                    break;
            }
        }

        // 添加字段过滤
        if (CollectionUtil.isNotEmpty(fieldNames)) {
            fieldNames.forEach(fieldName -> query.fields().include(fieldName));
        }

        // 添加排序和分页
        query.with(Sort.by(sort, cursorField)).limit(pageSize);

        // 执行查询
        try {
            String collection = StrUtil.isEmpty(collectionName) ? 
                    getCollectionName(entityClass) : collectionName;
            return getMongoTemplate().find(query, entityClass, collection);
        } catch (Exception e) {
            log.error("MongoDB查询异常, collection: {}, query: {}", collectionName, query, e);
            throw new ApiException("MongoDB查询异常: " + e.getMessage());
        }
    }

    /**
     * 验证查询参数
     */
    private static <T> void validateQueryParams(Query query, String cursorField, int pageSize, Class<T> entityClass) {
        if (query == null) {
            throw new IllegalArgumentException("查询条件不能为空");
        }
        if (StringUtils.isBlank(cursorField)) {
            throw new IllegalArgumentException("游标字段不能为空");
        }
        if (pageSize <= 0) {
            throw new IllegalArgumentException("分页大小必须大于0");
        }
        if (entityClass == null) {
            throw new IllegalArgumentException("返回类型不能为空");
        }
    }

    /**
     * 升序游标查询
     */
    private static <T> List<T> queryByCursor(Query query,
                                            String cursorField,
                                            Object cursorValue,
                                            int pageSize,
                                            List<String> fieldNames,
                                            Class<T> entityClass,
                                            String collectionName) {
        return doQueryByCursor(query, cursorField, cursorValue, pageSize, 
                Sort.Direction.ASC, fieldNames, entityClass, collectionName);
    }

    /**
     * 降序游标查询
     */
    private static <T> List<T> descQueryByCursor(Query query,
                                            String cursorField,
                                            Object cursorValue,
                                            int pageSize,
                                            List<String> fieldNames,
                                            Class<T> entityClass,
                                            String collectionName) {
        return doQueryByCursor(query, cursorField, cursorValue, pageSize, 
                Sort.Direction.DESC, fieldNames, entityClass, collectionName);
    }

    /**
     * 基于ID的升序游标查询
     */
    public static <T> List<T> queryByIdCursor(Query query, Object cursorValue, int pageSize, Class<T> entityClass) {
        return queryByCursor(query, Constant.MONGO_PRIMARY_KEY, cursorValue, pageSize, null, entityClass, null);
    }

    /**
     * 基于ID的降序游标查询
     */
    public static <T> List<T> descQueryByIdCursor(Query query, Object cursorValue, int pageSize, Class<T> entityClass) {
        return descQueryByCursor(query, Constant.MONGO_PRIMARY_KEY, cursorValue, pageSize, null, entityClass, null);
    }

    /**
     * 基于ID的升序游标查询(指定字段)
     */
    public static <T> List<T> queryByIdCursorForFields(Query query, Object cursorValue, int pageSize, 
            List<String> fieldNames, Class<T> entityClass) {
        return queryByCursor(query, Constant.MONGO_PRIMARY_KEY, cursorValue, pageSize, fieldNames, entityClass, null);
    }

    /**
     * 基于ID的降序游标查询(指定字段)
     */
    public static <T> List<T> descQueryByIdCursorForFields(Query query, Object cursorValue, int pageSize, 
            List<String> fieldNames, Class<T> entityClass) {
        return descQueryByCursor(query, Constant.MONGO_PRIMARY_KEY, cursorValue, pageSize, fieldNames, entityClass, null);
    }

    /**
     * 基于ID的升序游标查询(指定集合)
     */
    public static <T> List<T> queryByIdCursor(Query query, Object cursorValue, int pageSize, 
            Class<T> entityClass, String collectionName) {
        return queryByCursor(query, Constant.MONGO_PRIMARY_KEY, cursorValue, pageSize, null, entityClass, collectionName);
    }

    /**
     * 基于ID的降序游标查询(指定集合)
     */
    public static <T> List<T> descQueryByIdCursor(Query query, Object cursorValue, int pageSize, 
            Class<T> entityClass, String collectionName) {
        return descQueryByCursor(query, Constant.MONGO_PRIMARY_KEY, cursorValue, pageSize, null, entityClass, collectionName);
    }

    /**
     * 基于ID的升序游标查询(指定字段和集合)
     */
    public static <T> List<T> queryByIdCursorForFields(Query query, Object cursorValue, int pageSize, 
            List<String> fieldNames, Class<T> entityClass, String collectionName) {
        return queryByCursor(query, Constant.MONGO_PRIMARY_KEY, cursorValue, pageSize, fieldNames, entityClass, collectionName);
    }

    /**
     * 基于ID的降序游标查询(指定字段和集合)
     */
    public static <T> List<T> descQueryByIdCursorForFields(Query query, Object cursorValue, int pageSize, 
            List<String> fieldNames, Class<T> entityClass, String collectionName) {
        return descQueryByCursor(query, Constant.MONGO_PRIMARY_KEY, cursorValue, pageSize, fieldNames, entityClass, collectionName);
    }
}
