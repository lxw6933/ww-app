package com.ww.app.ip.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author ww
 * @create 2023-11-01- 10:36
 * @description:
 */
@Data
@ConfigurationProperties(prefix = Ip2RegionProperties.PREFIX)
public class Ip2RegionProperties {

    public static final String PREFIX = "ip2region";

    /**
     * 是否开启自动配置
     */
    private boolean enabled = false;

    /**
     * db数据文件位置
     * <p>
     * ClassPath目录下
     * </p>
     */
    private String dbFile = "ip2region/ip2region.xdb";

}
