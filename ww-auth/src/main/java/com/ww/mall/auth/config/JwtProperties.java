package com.ww.mall.auth.config;

import com.ww.mall.common.constant.Constant;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author ww
 * @create 2023-07-18- 10:17
 * @description:
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    /**
     * 过期时间 单位：小时
     */
    private Integer expire = 7 * 24;

    /**
     * 发行人
     */
    private String iss = "ww";

    /**
     * jwt秘钥
     */
    private String secret = Constant.SECRET_KEY;

}
