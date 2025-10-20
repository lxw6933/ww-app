package com.ww.app.mongodb.handler;

import com.mongodb.bulk.BulkWriteResult;
import com.ww.app.common.interfaces.BulkDataHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;

import static com.ww.app.mongodb.config.MongodbAutoConfiguration.DEFAULT_TRANSACTION_MONGODB_MANAGER;

/**
 * @author ww
 * @create 2024-10-14- 16:53
 * @description:
 */
@Slf4j
@Component
public class MongoBulkDataHandler<T> implements BulkDataHandler<T> {

    @Resource
    private MongoTemplate mongoTemplate;

    @Override
    @Transactional(transactionManager = DEFAULT_TRANSACTION_MONGODB_MANAGER)
    public int bulkSave(List<T> dataList) {
        Class<?> targetClass = dataList.get(0).getClass();
        // 初始化 BulkOperations
        // UNORDERED: 条记录插入失败，其他数据仍然会继续插入
        // ORDERED: 遇到错误时停止后续操作
        BulkOperations bulkOps = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, targetClass);
        // 将所有数据添加到 bulk 操作中
        bulkOps.insert(dataList);
        // 提交批量操作
        BulkWriteResult bulkWriteResult = bulkOps.execute();
        return bulkWriteResult.getInsertedCount();
    }

    @Override
    @Transactional(transactionManager = DEFAULT_TRANSACTION_MONGODB_MANAGER)
    public int bulkSave(List<T> dataList, String collectionName) {
        BulkOperations bulkOps = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, collectionName);
        // 将所有数据添加到 bulk 操作中
        bulkOps.insert(dataList);
        // 提交批量操作
        BulkWriteResult bulkWriteResult = bulkOps.execute();
        return bulkWriteResult.getInsertedCount();
    }
}
