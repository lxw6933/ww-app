package com.ww.mall.es;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;

/**
 * @author ww
 * @create 2023-07-15- 17:35
 * @description:
 */
@Slf4j
@Configuration
@ConditionalOnClass({ElasticsearchRestTemplate.class})
@EnableConfigurationProperties(ElasticsearchProperties.class)
public class MallElasticsearchAutoConfiguration {
}
