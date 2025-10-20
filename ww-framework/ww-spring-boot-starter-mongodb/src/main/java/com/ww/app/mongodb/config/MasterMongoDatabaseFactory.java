package com.ww.app.mongodb.config;

import com.mongodb.ReadPreference;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;

/**
 * @author ww
 * @create 2023-10-20- 09:18
 * @description: 主库优先的MongoDatabaseFactory实现
 */
@Slf4j
public class MasterMongoDatabaseFactory extends SimpleMongoClientDatabaseFactory {

    private final ReadPreference readPreference;

    public MasterMongoDatabaseFactory(MongoClient mongoClient, String databaseName, ReadPreference readPreference) {
        super(mongoClient, databaseName);
        this.readPreference = readPreference;
    }

    @Override
    @NonNull
    public MongoDatabase getMongoDatabase() throws IllegalStateException {
        MongoDatabase database = super.getMongoDatabase();
        // 设置读偏好为主库优先
        database = database.withReadPreference(readPreference);
        log.debug("使用主库读偏好获取数据库: {}", database.getName());
        return database;
    }

    @Override
    @NonNull
    public MongoDatabase getMongoDatabase(@NonNull String dbName) throws IllegalStateException {
        MongoDatabase database = super.getMongoDatabase(dbName);
        // 设置读偏好为主库优先
        database = database.withReadPreference(readPreference);
        log.debug("使用主库读偏好获取数据库: {}", database.getName());
        return database;
    }
}


