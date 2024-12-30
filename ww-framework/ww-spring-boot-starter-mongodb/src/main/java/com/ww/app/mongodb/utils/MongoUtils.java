package com.ww.app.mongodb.utils;

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
     * @param mongoTemplate  MongoTemplate 实例
     * @param query          查询条件
     * @param cursorField    游标字段（必须有索引，建议是 `_id` 或单调递增字段）
     * @param cursorValue    游标值（初始值可为 null 表示从头开始查询）
     * @param pageSize       每页大小
     * @param entityClass    返回数据类型
     * @param <T>            数据类型
     * @return 查询结果列表
     */
    public static <T> List<T> queryByCursor(MongoTemplate mongoTemplate,
                                            Query query,
                                            String cursorField,
                                            Object cursorValue,
                                            int pageSize,
                                            Class<T> entityClass) {
        // 如果游标值不为空，则添加大于条件
        if (cursorValue != null) {
            query.addCriteria(Criteria.where(cursorField).gt(new ObjectId(cursorValue.toString())));
        }

        // 添加排序和分页
        query.with(Sort.by(Sort.Direction.ASC, cursorField)).limit(pageSize);

        // 执行查询
        return mongoTemplate.find(query, entityClass, MongoUtils.getCollectionName(entityClass));
    }

    public static <T> List<T> pageByIdCursor(MongoTemplate mongoTemplate, Query query, Object cursorValue, int pageSize, Class<T> entityClass) {
        return queryByCursor(mongoTemplate, query, Constant.MONGO_PRIMARY_KEY, cursorValue, pageSize, entityClass);
    }

}
