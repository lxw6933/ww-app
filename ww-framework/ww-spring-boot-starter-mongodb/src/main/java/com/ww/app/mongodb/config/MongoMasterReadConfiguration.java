package com.ww.app.mongodb.config;

import com.mongodb.ReadPreference;
import com.mongodb.client.MongoClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.mongo.MongoProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;

/**
 * @author ww
 * @create 2023-10-20- 09:18
 * @description: 提供强制从主库读取数据的MongoTemplate配置
 */
@Slf4j
@Configuration
public class MongoMasterReadConfiguration {

    /**
     * 主库MongoTemplate Bean名称
     */
    public static final String MASTER_MONGO_TEMPLATE = "masterMongoTemplate";

    /**
     * 主库MongoDatabaseFactory Bean名称
     */
    public static final String MASTER_MONGO_DATABASE_FACTORY = "masterMongoDatabaseFactory";

    /**
     * 创建主库MongoDatabaseFactory
     * 配置读偏好为主库优先
     */
    @Bean(MASTER_MONGO_DATABASE_FACTORY)
    public MongoDatabaseFactory masterMongoDatabaseFactory(MongoClient mongoClient, MongoProperties mongoProperties) {
        log.info("初始化MongoDB主库DatabaseFactory，配置读偏好为主库优先");
        // 创建主库优先的MongoDatabaseFactory
        return new MasterMongoDatabaseFactory(
            mongoClient, 
            mongoProperties.getDatabase(),
            ReadPreference.primary()
        );
    }

    /**
     * 创建主库MongoTemplate
     * 复用现有的MappingMongoConverter，避免冲突
     */
    @Bean(MASTER_MONGO_TEMPLATE)
    public MongoTemplate masterMongoTemplate(@Qualifier(MASTER_MONGO_DATABASE_FACTORY) MongoDatabaseFactory masterDatabaseFactory,
            MappingMongoConverter mappingMongoConverter) {
        log.info("初始化MongoDB主库MongoTemplate，复用现有MappingMongoConverter");
        return new MongoTemplate(masterDatabaseFactory, mappingMongoConverter);
    }
}
