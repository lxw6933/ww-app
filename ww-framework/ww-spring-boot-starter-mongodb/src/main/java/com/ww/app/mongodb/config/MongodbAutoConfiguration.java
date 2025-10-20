package com.ww.app.mongodb.config;

import com.ww.app.mongodb.handler.MongoBulkDataHandler;
import com.ww.app.mongodb.listener.BaseDocListener;
import com.ww.app.mongodb.test.MongoTestComponent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.mongo.MongoProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.DefaultDbRefResolver;
import org.springframework.data.mongodb.core.convert.DefaultMongoTypeMapper;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;

/**
 * @author ww
 * @create 2023-07-15- 16:15
 * @description:
 */
@Slf4j
@ConditionalOnClass({MongoTemplate.class})
@EnableConfigurationProperties(MongoProperties.class)
@Import(MongoMasterReadConfiguration.class)
public class MongodbAutoConfiguration {

    public static final String DEFAULT_TRANSACTION_MONGODB_MANAGER = "mongoTransactionManager";

    @Bean
    public BaseDocListener baseDocListener() {
        log.info("初始化mongodb baseDoc listener...");
        return new BaseDocListener();
    }

    @Bean(DEFAULT_TRANSACTION_MONGODB_MANAGER)
    public MongoTransactionManager transactionManager(MongoDatabaseFactory factory) {
        // 单机MongoDB无法执行涉及到事务的操作。为了使用事务，你需要设置一个 MongoDB 副本集
        log.info("初始化mongodb事务管理器...");
        return new MongoTransactionManager(factory);
    }

    @Bean
    public MappingMongoConverter mappingMongoConverter(MongoDatabaseFactory mongoDatabaseFactory, MongoMappingContext mongoMappingContext) {
        DefaultDbRefResolver dbRefResolver = new DefaultDbRefResolver(mongoDatabaseFactory);
        MappingMongoConverter converter = new MappingMongoConverter(dbRefResolver, mongoMappingContext);
        // 去除写入mongodb时的_class字段
        converter.setTypeMapper(new DefaultMongoTypeMapper(null));
        return converter;
    }

    @Bean
    public <T> MongoBulkDataHandler<T> mongoBulkDataHandler() {
        return new MongoBulkDataHandler<>();
    }

    @Bean
    public MongoTestComponent mongoTestComponent() {
        return new MongoTestComponent();
    }

}
