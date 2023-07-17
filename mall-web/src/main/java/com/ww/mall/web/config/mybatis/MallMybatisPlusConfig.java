package com.ww.mall.web.config.mybatis;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @description: mybatis-plus 配置类
 * @author: ww
 * @create: 2021-04-16 09:59
 */
@Slf4j
@Configuration
@ConditionalOnClass({DataSourceAutoConfiguration.class})
public class MallMybatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        log.info("初始化mybatis plus插件成功...");
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 分页插件配置
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        // 乐观锁配置
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        return interceptor;
    }

    @Bean
    public MallMybatisPlusMetaHandler mallMybatisPlusMetaHandler() {
        log.info("初始化mybatis plus源数据处理器成功...");
        return new MallMybatisPlusMetaHandler();
    }

}
