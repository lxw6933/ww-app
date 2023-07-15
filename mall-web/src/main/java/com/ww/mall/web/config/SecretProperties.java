package com.ww.mall.web.config;

import com.ww.mall.common.constant.Constant;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * @author ww
 * @create 2023-07-15- 11:14
 * @description: 加密配置类
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "secret")
public class SecretProperties {

    /**
     * 响应结果是否开启加密
     */
    private boolean enabled;

    /**
     * 加密key
     */
    private String secretKey = Constant.SECRET_KEY;

    /**
     * 返回报文需要加密的接口路径
     */
    private List<String> encryptUriList;

    /**
     * 需要排除返回报文加密的接口
     */
    private List<String> excludeUriList;

}
