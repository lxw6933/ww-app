package com.ww.app.es.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;

/**
 * @author ww
 * @create 2023-07-15- 17:35
 * @description:
 */
@Slf4j
@ConditionalOnClass({ElasticsearchRestTemplate.class})
@EnableConfigurationProperties(ElasticsearchProperties.class)
public class ElasticsearchAutoConfiguration {
}
