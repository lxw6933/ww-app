package com.ww.app.mongodb.config;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.ReadPreference;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.mongo.MongoProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
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
     * 公用 MongoClient（连接池）
     */
    @Bean
    @Primary
    public MongoClient mongoClient(MongoProperties mongoProperties) {
        ConnectionString connectionString = new ConnectionString(mongoProperties.getUri());
        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(connectionString)
                // 默认客户端设置，不指定 readPreference，这样模板可自行覆盖
                .readPreference(ReadPreference.primary())
                .build();
        return MongoClients.create(settings);
    }

    /**
     * 默认 MongoTemplate（从库优先，无从库则自动回退主库）
     */
    @Bean
    @Primary
    public MongoTemplate mongoTemplate(MongoProperties mongoProperties,
                                       MappingMongoConverter mappingMongoConverter) {
        String database = extractDatabaseName(mongoProperties);
        ConnectionString connectionString = new ConnectionString(mongoProperties.getUri());
        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(connectionString)
                // 从库优先
                .readPreference(ReadPreference.secondaryPreferred())
                .build();
        MongoClient mongoClient = MongoClients.create(settings);
        SimpleMongoClientDatabaseFactory factory = new SimpleMongoClientDatabaseFactory(mongoClient, database);
        return new MongoTemplate(factory, mappingMongoConverter);
    }

    /**
     * 主库 MongoTemplate（强制所有操作走主库）
     */
    @Bean(name = MASTER_MONGO_TEMPLATE)
    public MongoTemplate masterMongoTemplate(MongoProperties mongoProperties,
                                             MappingMongoConverter mappingMongoConverter) {
        String database = extractDatabaseName(mongoProperties);
        ConnectionString connectionString = new ConnectionString(mongoProperties.getUri());
        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(connectionString)
                // 主库
                .readPreference(ReadPreference.primary())
                .build();
        MongoClient mongoClient = MongoClients.create(settings);
        SimpleMongoClientDatabaseFactory factory =
                new SimpleMongoClientDatabaseFactory(mongoClient, database);

        return new MongoTemplate(factory, mappingMongoConverter);
    }

    /**
     * 提取数据库名的安全方法
     */
    private String extractDatabaseName(MongoProperties mongoProperties) {
        ConnectionString connectionString = new ConnectionString(mongoProperties.getUri());
        String database = connectionString.getDatabase();
        if (database == null || database.isEmpty()) {
            database = mongoProperties.getDatabase();
        }
        if (database == null || database.isEmpty()) {
            throw new IllegalArgumentException("MongoDB database name must not be empty! 请在配置中指定 spring.data.mongodb.database 或 URI 中的数据库名");
        }
        return database;
    }

}
