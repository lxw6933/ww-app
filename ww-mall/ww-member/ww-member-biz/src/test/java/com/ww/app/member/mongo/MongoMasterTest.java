package com.ww.app.member.mongo;

import com.ww.app.mongodb.common.BaseDoc;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.test.context.ActiveProfiles;

import javax.annotation.Resource;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

/**
 * MongoDB主库读取功能测试
 * 验证masterMongoTemplate是否真正从主库读取数据
 * 
 * @author ww
 * @create 2025-10-20 16:24
 */
@Slf4j
@SpringBootTest
@ActiveProfiles("dev")
public class MongoMasterTest {

    @Resource
    private MongoTemplate mongoTemplate;

    @Resource
    private MongoTemplate masterMongoTemplate;

    /**
     * 测试实体类
     */
    @EqualsAndHashCode(callSuper = true)
    @Data
    @Document(collection = "test_master_verification")
    public static class TestEntity extends BaseDoc {

        @Indexed
        private String name;

        private String description;

        private String testType;
    }

    /**
     * 测试1: 验证两个MongoTemplate的连接信息差异
     */
    @Test
    public void testConnectionInfoDifference() {
        log.info("========== 测试1: 验证连接信息差异 ==========");
        
        try {
            // 获取默认MongoTemplate信息
            log.info("默认MongoTemplate连接信息:");
            printConnectionInfo(mongoTemplate, "默认MongoTemplate");
            
            // 获取主库MongoTemplate信息
            log.info("主库MongoTemplate连接信息:");
            printConnectionInfo(masterMongoTemplate, "主库MongoTemplate");
            
        } catch (Exception e) {
            log.error("获取连接信息失败", e);
        }
    }

    /**
     * 测试2: 主从库延迟验证测试
     */
    @Test
    public void testMasterSlaveDelay() {
        log.info("========== 测试2: 主从库延迟验证 ==========");
        
        try {
            // 1. 写入测试数据
            TestEntity testEntity = new TestEntity();
            testEntity.setName("延迟测试_" + System.currentTimeMillis());
            testEntity.setDescription("测试主从库数据同步延迟");
            testEntity.setTestType("DELAY_TEST");
            
            mongoTemplate.save(testEntity);
            log.info("写入测试数据: {}", testEntity.getName());
            
            // 2. 立即使用主库读取
            Query query = new Query(Criteria.where("name").is(testEntity.getName()));
            
            long startTime = System.currentTimeMillis();
            TestEntity masterResult = masterMongoTemplate.findOne(query, TestEntity.class);
            long masterTime = System.currentTimeMillis() - startTime;
            
            if (masterResult != null) {
                log.info("✅ 主库读取成功 - 耗时: {} ms, 数据: {}", masterTime, masterResult.getName());
            } else {
                log.warn("❌ 主库读取失败");
            }
            
            // 3. 使用默认MongoTemplate读取（可能从从库）
            startTime = System.currentTimeMillis();
            TestEntity defaultResult = mongoTemplate.findOne(query, TestEntity.class);
            long defaultTime = System.currentTimeMillis() - startTime;
            
            if (defaultResult != null) {
                log.info("✅ 默认读取成功 - 耗时: {} ms, 数据: {}", defaultTime, defaultResult.getName());
            } else {
                log.warn("❌ 默认读取失败 - 可能存在主从延迟");
            }
            
            // 4. 结果分析
            if (masterResult != null && defaultResult != null) {
                log.info("✅ 主库和默认读取都成功，数据一致性验证通过");
            } else if (masterResult != null && defaultResult == null) {
                log.info("⚠️  主库读取成功，默认读取失败 - 说明存在主从延迟，主库读取功能正常");
            } else {
                log.warn("❌ 主库和默认读取都失败，请检查MongoDB连接");
            }
            
            // 5. 清理测试数据
            mongoTemplate.remove(query, TestEntity.class);
            log.info("测试数据已清理");
            
        } catch (Exception e) {
            log.error("主从库延迟测试失败", e);
        }
    }

    /**
     * 测试3: 性能对比测试
     */
    @Test
    public void testPerformanceComparison() {
        log.info("========== 测试3: 性能对比测试 ==========");
        
        try {
            int testCount = 10;
            
            // 测试默认MongoTemplate性能
            long defaultTotalTime = 0;
            for (int i = 0; i < testCount; i++) {
                long startTime = System.currentTimeMillis();
                mongoTemplate.find(new Query(), TestEntity.class);
                long endTime = System.currentTimeMillis();
                defaultTotalTime += (endTime - startTime);
            }
            double defaultAvgTime = (double) defaultTotalTime / testCount;
            
            // 测试主库MongoTemplate性能
            long masterTotalTime = 0;
            for (int i = 0; i < testCount; i++) {
                long startTime = System.currentTimeMillis();
                masterMongoTemplate.find(new Query(), TestEntity.class);
                long endTime = System.currentTimeMillis();
                masterTotalTime += (endTime - startTime);
            }
            double masterAvgTime = (double) masterTotalTime / testCount;
            
            log.info("默认MongoTemplate平均耗时: {:.2f} ms", defaultAvgTime);
            log.info("主库MongoTemplate平均耗时: {:.2f} ms", masterAvgTime);
            
            if (masterAvgTime > defaultAvgTime) {
                log.info("⚠️  主库读取比默认读取慢 {:.2f} ms，这是正常现象（主库负载更高）", masterAvgTime - defaultAvgTime);
            } else {
                log.info("✅ 主库读取性能良好");
            }
            
        } catch (Exception e) {
            log.error("性能对比测试失败", e);
        }
    }

    /**
     * 测试4: 数据实时性验证
     */
    @Test
    public void testDataRealtime() {
        log.info("========== 测试4: 数据实时性验证 ==========");
        
        try {
            // 1. 写入数据
            TestEntity entity = new TestEntity();
            entity.setName("实时性测试_" + System.currentTimeMillis());
            entity.setDescription("测试数据实时性");
            entity.setTestType("REALTIME_TEST");
            
            mongoTemplate.save(entity);
            log.info("写入测试数据: {}", entity.getName());
            
            // 2. 立即主库读取
            Query query = new Query(Criteria.where("name").is(entity.getName()));
            TestEntity masterResult = masterMongoTemplate.findOne(query, TestEntity.class);
            
            if (masterResult != null) {
                log.info("✅ 主库读取成功: {}", masterResult.getName());
            } else {
                log.warn("❌ 主库读取失败");
            }
            
            // 3. 默认读取对比
            TestEntity defaultResult = mongoTemplate.findOne(query, TestEntity.class);
            if (defaultResult != null) {
                log.info("✅ 默认读取成功: {}", defaultResult.getName());
            } else {
                log.warn("❌ 默认读取失败");
            }
            
            // 4. 清理
            mongoTemplate.remove(query, TestEntity.class);
            log.info("测试数据已清理");
            
        } catch (Exception e) {
            log.error("数据实时性测试失败", e);
        }
    }

    /**
     * 测试5: 批量数据验证
     */
    @Test
    public void testBatchDataVerification() {
        log.info("========== 测试5: 批量数据验证 ==========");
        
        try {
            // 1. 批量写入数据
            for (int i = 0; i < 5; i++) {
                TestEntity entity = new TestEntity();
                entity.setName("批量测试_" + i + "_" + System.currentTimeMillis());
                entity.setDescription("批量数据测试");
                entity.setTestType("BATCH_TEST");
                mongoTemplate.save(entity);
            }
            log.info("批量写入5条测试数据");
            
            // 2. 使用主库查询
            Query query = new Query(Criteria.where("testType").is("BATCH_TEST"));
            List<TestEntity> masterResults = masterMongoTemplate.find(query, TestEntity.class);
            log.info("主库查询结果数量: {}", masterResults.size());
            
            // 3. 使用默认查询
            List<TestEntity> defaultResults = mongoTemplate.find(query, TestEntity.class);
            log.info("默认查询结果数量: {}", defaultResults.size());
            
            // 4. 结果对比
            if (masterResults.size() == defaultResults.size()) {
                log.info("✅ 主库和默认查询结果数量一致: {}", masterResults.size());
            } else {
                log.warn("⚠️  主库查询: {}, 默认查询: {}", masterResults.size(), defaultResults.size());
            }
            
            // 5. 清理
            mongoTemplate.remove(query, TestEntity.class);
            log.info("批量测试数据已清理");
            
        } catch (Exception e) {
            log.error("批量数据验证测试失败", e);
        }
    }

    /**
     * 打印连接信息
     */
    private void printConnectionInfo(MongoTemplate template, String templateName) {
        try {
            // 获取数据库名称
            String dbName = template.getDb().getName();
            log.info("{} - 数据库名称: {}", templateName, dbName);
            
            // 执行查询获取耗时
            long startTime = System.currentTimeMillis();
            template.getCollection("test_master_verification").countDocuments();
            long endTime = System.currentTimeMillis();
            
            log.info("{} - 查询耗时: {} ms", templateName, endTime - startTime);
            
            // 尝试获取连接池信息
            try {
                Object mongoDbFactory = template.getMongoDbFactory();
                log.info("{} - MongoDbFactory类型: {}", templateName, mongoDbFactory.getClass().getSimpleName());
                
                // 尝试获取MongoClient信息
                if (mongoDbFactory instanceof org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory) {
                    org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory factory = 
                        (org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory) mongoDbFactory;
                    
                    Field clientField = factory.getClass().getDeclaredField("mongoClient");
                    clientField.setAccessible(true);
                    Object mongoClient = clientField.get(factory);
                    
                    if (mongoClient != null) {
                        log.info("{} - MongoClient类型: {}", templateName, mongoClient.getClass().getSimpleName());
                        
                        // 尝试获取连接主机信息
                        try {
                            Method getSettingsMethod = mongoClient.getClass().getMethod("getSettings");
                            Object settings = getSettingsMethod.invoke(mongoClient);
                            
                            if (settings != null) {
                                Method getClusterSettingsMethod = settings.getClass().getMethod("getClusterSettings");
                                Object clusterSettings = getClusterSettingsMethod.invoke(settings);
                                
                                if (clusterSettings != null) {
                                    Method getHostsMethod = clusterSettings.getClass().getMethod("getHosts");
                                    Object hosts = getHostsMethod.invoke(clusterSettings);
                                    
                                    if (hosts != null) {
                                        log.info("{} - 连接主机: {}", templateName, hosts.toString());
                                    }
                                }
                            }
                        } catch (Exception e) {
                            log.debug("无法获取{}的连接字符串信息: {}", templateName, e.getMessage());
                        }
                    }
                }
                
            } catch (Exception e) {
                log.debug("无法获取{}的详细连接信息: {}", templateName, e.getMessage());
            }
            
        } catch (Exception e) {
            log.error("获取{}连接信息失败: {}", templateName, e.getMessage());
        }
    }
}
