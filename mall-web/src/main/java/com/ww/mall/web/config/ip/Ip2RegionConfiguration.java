package com.ww.mall.web.config.ip;

/**
 * @author ww
 * @create 2023-11-01- 10:34
 * @description:
 */
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.ClassLoaderUtil;
import lombok.extern.slf4j.Slf4j;
import org.lionsoul.ip2region.xdb.Searcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.io.InputStream;

@Slf4j
@Configuration
@ConditionalOnClass(Ip2regionSearcher.class)
@EnableConfigurationProperties(Ip2RegionProperties.class)
@ConditionalOnProperty(prefix = Ip2RegionProperties.PREFIX, name = "enabled", havingValue = "true")
public class Ip2RegionConfiguration {

    @Bean
    public Ip2regionSearcher ip2regionSearcher(@Autowired Ip2RegionProperties ip2RegionProperties) {
        ClassLoader classLoader = ClassLoaderUtil.getClassLoader();
        try (InputStream inputStream = classLoader.getResourceAsStream(ip2RegionProperties.getDbFile())) {
            Searcher searcher = Searcher.newWithBuffer(IoUtil.readBytes(inputStream));
            log.info("加载ip2regionSearcher成功");
            return new Ip2regionSearcher(searcher);
        } catch (IOException e) {
            log.error("加载ip2regionSearcher失败", e);
            throw new RuntimeException(e);
        }
    }

}
