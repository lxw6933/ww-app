package com.ww.mall.ip2region;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.ClassLoaderUtil;
import lombok.extern.slf4j.Slf4j;
import org.lionsoul.ip2region.xdb.Searcher;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author ww
 * @create 2023-11-01- 10:34
 * @description:
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(Ip2RegionProperties.class)
@ConditionalOnProperty(prefix = Ip2RegionProperties.PREFIX, name = "enabled", havingValue = "true", matchIfMissing = true)
public class Ip2RegionConfiguration {

    @Bean
    public Ip2RegionProperties ip2RegionProperties() {
        return new Ip2RegionProperties();
    }

    @Bean
    public Ip2regionSearcher ip2regionSearcher(Ip2RegionProperties ip2RegionProperties) {
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
