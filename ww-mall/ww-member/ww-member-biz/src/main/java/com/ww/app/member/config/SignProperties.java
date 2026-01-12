package com.ww.app.member.config;

import com.ww.app.member.enums.SignPeriod;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 签到配置
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "sign")
public class SignProperties {

    /**
     * 默认签到周期
     */
    private SignPeriod defaultPeriod = SignPeriod.MONTHLY;
}
