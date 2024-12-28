package com.ww.mall.third.constant;

import cn.hutool.core.collection.CollUtil;
import com.ww.mall.common.exception.ApiException;
import lombok.Data;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import javax.validation.constraints.NotNull;
import java.net.URI;
import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author ww
 * @create 2023-07-21- 17:13
 * @description:
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "http")
public class HttpProperties implements InitializingBean {

    private Duration connectTimeout = Duration.ofSeconds(30L);

    private Duration readTimeout = Duration.ofMinutes(2L);

    private Duration writeTimeout = Duration.ofMinutes(2L);

    private Integer maxIdleConnections = 50;

    private Duration keepAliveDuration = Duration.ofMinutes(30L);

    private List<CustomProp> customPropList;

    @Override
    public void afterPropertiesSet() {
        if (CollUtil.isNotEmpty(customPropList)) {
            Set<String> customPaths = new HashSet<>();
            for (CustomProp prop : customPropList) {
                String path = prop.getUri().getPath();
                if (!customPaths.add(path)) {
                    throw new ApiException("存在重复的uri：" + path);
                }
            }
        }
    }

    /**
     * 自定义prop【需要针对不同请求不同配置】如需使用自定义一个factory即可
     */
    @Data
    public static class CustomProp {

        @NotNull
        private URI uri;

        private Duration connectTimeout = Duration.ofSeconds(30L);

        private Duration readTimeout = Duration.ofMinutes(2L);

        private Duration writeTimeout = Duration.ofMinutes(2L);

        private Integer maxIdleConnections = 50;

        private Duration keepAliveDuration = Duration.ofMinutes(30L);

    }

}
