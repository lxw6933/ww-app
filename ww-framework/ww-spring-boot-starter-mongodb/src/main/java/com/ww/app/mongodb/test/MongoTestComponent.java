package com.ww.app.mongodb.test;

import com.mongodb.bulk.BulkWriteResult;
import com.ww.app.common.annotation.TimeCost;
import com.ww.app.common.interfaces.BulkDataHandler;
import com.ww.app.mongodb.common.BaseDoc;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

import static com.ww.app.mongodb.config.MongodbAutoConfiguration.DEFAULT_TRANSACTION_MONGODB_MANAGER;

/**
 * @author ww
 * @create 2025-10-20 11:40
 * @description:
 */
@Slf4j
@Component
public class MongoTestComponent {

    @Resource
    private MongoTemplate mongoTemplate;

    @Resource
    private BulkDataHandler<A> mongoBulkDataHandler;

    @Transactional(transactionManager = DEFAULT_TRANSACTION_MONGODB_MANAGER)
    public void testTransaction(boolean e) {
        A a1 = new A("aa11", 111);
        mongoTemplate.insert(a1);
        if (e) {
            int a = 1 / 0;
        }
        A a2 = new A("aa4", 14);
        mongoTemplate.insert(a2);
    }

    public void testBatchTransaction(boolean e) {
        String namePrefix = "b";
        List<A> aList = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            aList.add(new A(namePrefix + i, i));
        }
        if (e) {
            // name是唯一键
            aList.add(new A(namePrefix + "9", 10));
        }
        mongoBulkDataHandler.bulkSave(aList);
    }

    @TimeCost
    public void testBatchTransactionTime() {
        String namePrefix = "transTime";
        List<A> aList = new ArrayList<>();
        for (int i = 1; i <= 5000; i++) {
            aList.add(new A(namePrefix + i, i));
        }
        int insertCount = mongoBulkDataHandler.bulkSave(aList);
        log.info("【事务】批量插入数据量：{}", insertCount);
    }

    @TimeCost
    public void testBatchTime() {
        String namePrefix = "nontransTime";
        List<A> aList = new ArrayList<>();
        for (int i = 1; i <= 5000; i++) {
            aList.add(new A(namePrefix + i, i));
        }
        BulkOperations bulkOps = mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, A.class);
        // 将所有数据添加到 bulk 操作中
        bulkOps.insert(aList);
        // 提交批量操作
        BulkWriteResult bulkWriteResult = bulkOps.execute();
        log.info("【非事务】批量插入数据量：{}", bulkWriteResult.getInsertedCount());
    }

    @EqualsAndHashCode(callSuper = true)
    @Data
    @Document("test")
    @AllArgsConstructor
    public static class A extends BaseDoc {
        private String name;
        private int age;
    }

}
