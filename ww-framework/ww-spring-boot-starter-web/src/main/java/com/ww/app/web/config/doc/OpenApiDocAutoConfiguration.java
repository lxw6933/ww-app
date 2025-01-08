package com.ww.app.web.config.doc;

import com.ww.app.web.properties.AppProperties;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;

import java.util.HashMap;
import java.util.Map;

/**
 * @author ww
 * @create 2025-01-08- 15:11
 * @description: openapi doc configuration
 */
@AutoConfiguration
@EnableConfigurationProperties(AppProperties.class)
public class OpenApiDocAutoConfiguration {

    @Bean
    public OpenAPI openApi(AppProperties appProperties) {
        Map<String, SecurityScheme> securitySchemas = buildSecuritySchemes();
        // 构建API文档基本信息
        Info info = buildInfo(appProperties);
        // 创建OpenAPI对象
        OpenAPI openApi = new OpenAPI()
                // 接口信息
                .info(info)
                // 接口安全配置
                .components(new Components().securitySchemes(securitySchemas))
                .addSecurityItem(new SecurityRequirement().addList(HttpHeaders.AUTHORIZATION));
        securitySchemas.keySet().forEach(key -> openApi.addSecurityItem(new SecurityRequirement().addList(key)));
        return openApi;
    }

    /**
     * API 摘要信息
     */
    private Info buildInfo(AppProperties appProperties) {
        Info info = new Info()
                .title(String.format("%s %s", appProperties.getName(), "API 文档"))
                .version(appProperties.getVersion())
                .description(appProperties.getDescription());
        AppProperties.Contact contact = appProperties.getContact();
        if (contact != null) {
            info.contact(new Contact()
                    .name(contact.getName())
                    .url(contact.getUrl())
                    .email(contact.getEmail())
            );
        }
        AppProperties.License license = appProperties.getLicense();
        if (license != null) {
            info.license(new License()
                    .name(appProperties.getLicense().getName())
                    .url(appProperties.getLicense().getUrl())
            );
        }
        return info;
    }

    private Map<String, SecurityScheme> buildSecuritySchemes() {
        Map<String, SecurityScheme> securitySchemes = new HashMap<>();
        SecurityScheme securityScheme = new SecurityScheme()
                // 类型
                .type(SecurityScheme.Type.APIKEY)
                // 请求头的 name
                .name(HttpHeaders.AUTHORIZATION)
                // token 所在位置
                .in(SecurityScheme.In.HEADER);
        securitySchemes.put(HttpHeaders.AUTHORIZATION, securityScheme);
        return securitySchemes;
    }

}
