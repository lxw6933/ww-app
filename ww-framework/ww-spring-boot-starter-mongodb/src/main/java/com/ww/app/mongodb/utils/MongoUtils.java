package com.ww.app.mongodb.utils;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.ww.app.common.constant.Constant;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.List;

/**
 * @author ww
 * @create 2024-12-19- 13:45
 * @description:
 */
@Slf4j
public class MongoUtils {

    private MongoUtils() {}

    private static MongoTemplate mongoTemplate;

    public static MongoTemplate getMongoTemplate() {
        if (mongoTemplate == null) {
            log.info("初始化MongodbTemplate引用");
            mongoTemplate = SpringUtil.getBean(MongoTemplate.class);
        }
        return mongoTemplate;
    }

    public static <T> String getCollectionName(Class<T> tClass) {
        if (tClass.isAnnotationPresent(Document.class)) {
            Document document = tClass.getAnnotation(Document.class);
            return StringUtils.isBlank(document.value()) ? document.collection() : document.value();
        } else {
            throw new IllegalArgumentException("Class " + tClass.getName() + " does not have @Document annotation.");
        }
    }

    /**
     * 通用游标查询方法
     *
     * @param query          查询条件
     * @param cursorField    游标字段（必须有索引，建议是 `_id` 或单调递增字段）
     * @param cursorValue    游标值（初始值可为 null 表示从头开始查询）
     * @param pageSize       每页大小
     * @param entityClass    返回数据类型
     * @param <T>            数据类型
     * @param collectionName 指定文档名称
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
        // 如果游标值不为空，则添加大于条件
        if (cursorValue != null) {
            query.addCriteria(Criteria.where(cursorField).gt(new ObjectId(cursorValue.toString())));
        }

        // 是否指定字段返回，节省网络开销
        if (CollectionUtil.isNotEmpty(fieldNames)) {
            fieldNames.forEach(fieldName -> query.fields().include(fieldName));
        }

        // 添加排序和分页
        query.with(Sort.by(sort, cursorField)).limit(pageSize);

        // 执行查询
        if (StrUtil.isEmpty(collectionName)) {
            return getMongoTemplate().find(query, entityClass, MongoUtils.getCollectionName(entityClass));
        } else {
            return getMongoTemplate().find(query, entityClass, collectionName);
        }
    }

    private static <T> List<T> queryByCursor(Query query,
                                            String cursorField,
                                            Object cursorValue,
                                            int pageSize,
                                            List<String> fieldNames,
                                            Class<T> entityClass,
                                            String collectionName) {
        return doQueryByCursor(query, cursorField, cursorValue, pageSize, Sort.Direction.ASC, fieldNames, entityClass, collectionName);
    }

    private static <T> List<T> descQueryByCursor(Query query,
                                            String cursorField,
                                            Object cursorValue,
                                            int pageSize,
                                            List<String> fieldNames,
                                            Class<T> entityClass,
                                            String collectionName) {
        return doQueryByCursor(query, cursorField, cursorValue, pageSize, Sort.Direction.DESC, fieldNames, entityClass, collectionName);
    }

    public static <T> List<T> queryByIdCursor(Query query, Object cursorValue, int pageSize, Class<T> entityClass) {
        return queryByCursor(query, Constant.MONGO_PRIMARY_KEY, cursorValue, pageSize, null, entityClass, null);
    }

    public static <T> List<T> descQueryByIdCursor(Query query, Object cursorValue, int pageSize, Class<T> entityClass) {
        return descQueryByCursor(query, Constant.MONGO_PRIMARY_KEY, cursorValue, pageSize, null, entityClass, null);
    }

    public static <T> List<T> queryByIdCursorForFields(Query query, Object cursorValue, int pageSize, List<String> fieldNames, Class<T> entityClass) {
        return queryByCursor(query, Constant.MONGO_PRIMARY_KEY, cursorValue, pageSize, fieldNames, entityClass, null);
    }

    public static <T> List<T> descQueryByIdCursorForFields(Query query, Object cursorValue, int pageSize, List<String> fieldNames, Class<T> entityClass) {
        return descQueryByCursor(query, Constant.MONGO_PRIMARY_KEY, cursorValue, pageSize, fieldNames, entityClass, null);
    }

    public static <T> List<T> queryByIdCursor(Query query, Object cursorValue, int pageSize, Class<T> entityClass, String collectionName) {
        return queryByCursor(query, Constant.MONGO_PRIMARY_KEY, cursorValue, pageSize, null, entityClass, collectionName);
    }

    public static <T> List<T> descQueryByIdCursor(Query query, Object cursorValue, int pageSize, Class<T> entityClass, String collectionName) {
        return descQueryByCursor(query, Constant.MONGO_PRIMARY_KEY, cursorValue, pageSize, null, entityClass, collectionName);
    }

    public static <T> List<T> queryByIdCursorForFields(Query query, Object cursorValue, int pageSize, List<String> fieldNames, Class<T> entityClass, String collectionName) {
        return queryByCursor(query, Constant.MONGO_PRIMARY_KEY, cursorValue, pageSize, fieldNames, entityClass, collectionName);
    }

    public static <T> List<T> descQueryByIdCursorForFields(Query query, Object cursorValue, int pageSize, List<String> fieldNames, Class<T> entityClass, String collectionName) {
        return descQueryByCursor(query, Constant.MONGO_PRIMARY_KEY, cursorValue, pageSize, fieldNames, entityClass, collectionName);
    }

}
