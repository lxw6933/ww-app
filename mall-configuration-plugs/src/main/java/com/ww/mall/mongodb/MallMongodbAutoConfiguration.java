package com.ww.mall.mongodb;

import com.ww.mall.mongodb.handler.CommonStockHandler;
import com.ww.mall.mongodb.handler.MongoBulkDataHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.mongo.MongoProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
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
public class MallMongodbAutoConfiguration {

    @Bean
    public BaseDocListener baseDocListener() {
        log.info("初始化mongodb baseDoc listener...");
        return new BaseDocListener();
    }

    @Bean("mongoTransactionManager")
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
    public CommonStockHandler commonStockHandler() {
        return new CommonStockHandler();
    }

    @Bean
    public <T> MongoBulkDataHandler<T> mongoBulkDataHandler() {
        return new MongoBulkDataHandler<>();
    }

}
