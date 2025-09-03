package com.ww.app.pay.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 支付宝配置属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "pay.alipay")
public class AliPayProperties {
    /**
     * 应用ID
     */
    private String appId;

    /**
     * 商户私钥
     */
    private String privateKey;

    /**
     * 支付宝公钥
     */
    private String publicKey;

    /**
     * 服务器URL
     */
    private String serverUrl = "https://openapi.alipay.com/gateway.do";

    /**
     * 签名类型
     */
    private String signType = "RSA2";

    /**
     * 编码
     */
    private String charset = "UTF-8";

    /**
     * 接口版本号
     */
    private String version = "1.0";

    /**
     * 支付结果异步通知地址
     */
    private String notifyUrl;

    /**
     * 支付完成后的跳转地址
     */
    private String returnUrl;

    public String getDomain() {
        return "";
    }

    public boolean getUseCer() {
        return false;
    }

    public String getAliPayCertPath() {
        return "";
    }
}
