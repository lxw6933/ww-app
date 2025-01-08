
package com.ww.app.web.properties;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties("ww.app")
public class AppProperties {

    /** 名称 */
    private String name;
    /** 版本 */
    private String version;
    /** 描述 */
    private String description;
    /** URL */
    private String url;
    /** 基本包 */
    private String basePackage;
    /** 联系人 */
    private Contact contact;
    /** 许可协议 */
    private License license;
    /** 是否为生产环境 */
    private boolean production;

    /** 联系人配置属性 */
    @Data
    public static class Contact {
        /** 名称 */
        private String name;
        /** 邮箱 */
        private String email;
        /** URL */
        private String url;
    }

    /** 许可协议配置属性 */
    @Data
    public static class License {
        /** 名称 */
        private String name;
        /** URL */
        private String url;
    }
}
