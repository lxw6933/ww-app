package com.ww.app.member.transaction;

import com.ww.app.mongodb.test.MongoTestComponent;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import javax.annotation.Resource;

/**
 * @author ww
 * @create 2025-10-20 10:42
 * @description:
 */
@SpringBootTest
@ActiveProfiles("dev")
public class MongoTransactionTest {

    @Resource
    private MongoTestComponent mongoTestComponent;

    @Test
    void testSave() {
        mongoTestComponent.testTransaction(true);
    }

    @Test
    void testTransaction() {
        mongoTestComponent.testBatchTransaction(true);
    }

    @Test
    void testBatchTransactionTime() {
        mongoTestComponent.testBatchTransactionTime();
    }

    @Test
    void testBatchTime() {
        mongoTestComponent.testBatchTime();
    }

}
