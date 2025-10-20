package com.ww.app.member.mongo;

import com.mongodb.ReadPreference;
import com.mongodb.client.MongoCollection;
import com.ww.app.mongodb.common.BaseDoc;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;
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

import static com.ww.app.mongodb.config.MongoMasterReadConfiguration.MASTER_MONGO_TEMPLATE;

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

    @Resource(name = MASTER_MONGO_TEMPLATE)
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
        log.info("===> 对比两个 MongoTemplate 的核心差异（读偏好、工厂、客户端、Bean 实例）");

        // 1) 有效读偏好（以集合级为准，实际执行时生效）
        ReadPreference defaultReadPref = mongoTemplate.execute(TestEntity.class, MongoCollection::getReadPreference);
        ReadPreference masterReadPref = masterMongoTemplate.execute(TestEntity.class, MongoCollection::getReadPreference);
        log.info("[mongoTemplate] Effective ReadPreference = {}", defaultReadPref);
        log.info("[masterMongoTemplate] Effective ReadPreference = {}", masterReadPref);

        // 明确断言：两个模板的读偏好应当不同（默认 secondaryPreferred vs primary）
        Assertions.assertNotEquals(defaultReadPref.getName(), masterReadPref.getName(),
                "两个模板的 ReadPreference 应该不同：默认应为 secondaryPreferred，master 应为 primary");

        // 2) 工厂与客户端信息
        printFactoryAndClientInfo(mongoTemplate, "mongoTemplate");
        printFactoryAndClientInfo(masterMongoTemplate, "masterMongoTemplate");

        // 3) Bean 实例与引用差异
        log.info("[mongoTemplate]       identityHashCode=0x{}", Integer.toHexString(System.identityHashCode(mongoTemplate)));
        log.info("[masterMongoTemplate] identityHashCode=0x{}", Integer.toHexString(System.identityHashCode(masterMongoTemplate)));
        Assertions.assertNotSame(mongoTemplate, masterMongoTemplate, "两个模板实例应为不同 Bean");

        // 1. 插入一条数据到主库
        TestEntity entity = new TestEntity();
        entity.setName("master-test");
        entity.setDescription("测试主库写入");
        entity.setTestType("write");

        masterMongoTemplate.insert(entity);
        log.info("[masterMongoTemplate] 插入数据: {}", entity);

        // 2. 分别使用两个模板读取数据
        Query query = new Query(Criteria.where("name").is("master-test"));
        TestEntity readFromMaster = masterMongoTemplate.findOne(query, TestEntity.class);
        TestEntity readFromDefault = mongoTemplate.findOne(query, TestEntity.class);

        log.info("[masterMongoTemplate] 查询结果: {}", readFromMaster);
        log.info("[mongoTemplate] 查询结果: {}", readFromDefault);

        // 3. 验证连接配置是否不同
        org.bson.Document masterDbStats = masterMongoTemplate.executeCommand("{ dbStats: 1 }");
        org.bson.Document defaultDbStats = mongoTemplate.executeCommand("{ dbStats: 1 }");

        log.info("[masterMongoTemplate] dbStats: {}", masterDbStats.get("primary"));
        log.info("[mongoTemplate] dbStats: {}", defaultDbStats.get("primary"));

        // 4. 验证模板对象引用不同
        if (masterMongoTemplate == mongoTemplate) {
            log.info("masterMongoTemplate 与 mongoTemplate 是同一个对象");
        }

        log.info("✅ 测试完成，两者配置已区分。");
    }

    private void printFactoryAndClientInfo(MongoTemplate template, String templateName) {
        try {
            Object mongoDbFactory = template.getMongoDbFactory();
            log.info("[{}] MongoDbFactory = {}", templateName, mongoDbFactory.getClass().getName());

            if (mongoDbFactory instanceof org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory) {
                org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory factory =
                        (org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory) mongoDbFactory;
                // 反射拿到内部 mongoClient
                Field clientField = factory.getClass().getDeclaredField("mongoClient");
                clientField.setAccessible(true);
                Object client = clientField.get(factory);
                if (client != null) {
                    log.info("[{}] MongoClient   = {}", templateName, client.getClass().getName());
                    try {
                        Method getSettingsMethod = client.getClass().getMethod("getSettings");
                        Object settings = getSettingsMethod.invoke(client);
                        if (settings != null) {
                            Method getClusterSettingsMethod = settings.getClass().getMethod("getClusterSettings");
                            Object clusterSettings = getClusterSettingsMethod.invoke(settings);
                            if (clusterSettings != null) {
                                Method getHostsMethod = clusterSettings.getClass().getMethod("getHosts");
                                Object hosts = getHostsMethod.invoke(clusterSettings);
                                log.info("[{}] Hosts       = {}", templateName, hosts);
                            }
                        }
                    } catch (Exception ignored) {
                        // 忽略获取 settings 的异常，仅用于增强日志
                    }
                }
            }

            // 打印集合层 effective ReadPreference 名称
            ReadPreference effective = template.execute(TestEntity.class, c -> c.getReadPreference());
            log.info("[{}] Effective ReadPreference = {}", templateName,
                    effective == null ? null : effective.getName());
        } catch (Exception e) {
            log.warn("[{}] 打印工厂/客户端信息失败: {}", templateName, e.getMessage());
        }
    }

    /**
     * 测试2: 立即插入后，从库读取不到，而主库可读（主从延迟的直观对比）
     */
    @Test
    public void testImmediateInsertMasterVisibleSecondaryInvisible() {
        log.info("========== 测试2: 立即插入后主从可见性对比 ==========");

        // 使用 master 模板插入，确保写入落在主库
        String uniqueName = "lag-test-" + System.currentTimeMillis();
        TestEntity entity = new TestEntity();
        entity.setName(uniqueName);
        entity.setDescription("主从滞后可见性测试");
        entity.setTestType("LAG_TEST");

        masterMongoTemplate.insert(entity);
        log.info("[masterMongoTemplate] 已插入: {}", uniqueName);

        // 立刻查询：主库应可读；默认模板（secondaryPreferred）可能由于复制滞后读不到
        Query query = new Query(Criteria.where("name").is(uniqueName));

        TestEntity readFromDefault = mongoTemplate.findOne(query, TestEntity.class);
        TestEntity readFromMaster = masterMongoTemplate.findOne(query, TestEntity.class);


        log.info("[masterMongoTemplate] 立即查询结果: {}", readFromMaster == null ? null : readFromMaster.getName());
        log.info("[mongoTemplate]       立即查询结果: {}", readFromDefault == null ? null : readFromDefault.getName());

        // 断言主库可读
        Assertions.assertNotNull(readFromMaster, "主库读取应立即可见");
        // 从库可能读不到（如果你的集群确有复制延迟），此处不强制断言为 null，但打印提示
        if (readFromDefault == null) {
            log.info("[mongoTemplate] 由于主从复制延迟，立即读取未命中，符合预期");
        } else {
            log.info("[mongoTemplate] 立即读取命中，当前复制延迟较低或路由至主库");
        }

        // 清理
        masterMongoTemplate.remove(query, TestEntity.class);
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
