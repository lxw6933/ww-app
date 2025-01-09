package com.ww.app.security.config;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.ww.app.common.constant.Constant;
import com.ww.app.common.utils.CollectionUtils;
import com.ww.app.security.filter.TokenAuthenticationAuthFilter;
import com.ww.app.security.handler.AppAccessDeniedHandler;
import com.ww.app.security.handler.AppAuthenticationEntryPoint;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.util.pattern.PathPattern;

import javax.annotation.security.PermitAll;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author ww
 * @create 2024-09-20- 14:29
 * @description:
 */
@Slf4j
@Configuration
public class SecurityAutoConfiguration {

    @Bean
    public TokenAuthenticationAuthFilter tokenAuthenticationAuthFilter() {
        return new TokenAuthenticationAuthFilter();
    }

    /**
     * 由于 Spring Security 创建 AuthenticationManager 对象时，没声明 @Bean 注解，导致无法被注入
     */
    @Bean
    public AuthenticationManager authenticationManagerBean(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    /**
     * 认证、授权、基础 配置
     * anyRequest          |   匹配所有请求路径
     * access              |   SpringEl表达式结果为true时可以访问
     * anonymous           |   匿名可以访问
     * denyAll             |   用户不能访问
     * fullyAuthenticated  |   用户完全认证可以访问（非remember-me下自动登录）
     * hasAnyAuthority     |   如果有参数，参数表示权限，则其中任何一个权限可以访问
     * hasAnyRole          |   如果有参数，参数表示角色，则其中任何一个角色可以访问
     * hasAuthority        |   如果有参数，参数表示权限，则其权限可以访问
     * hasIpAddress        |   如果有参数，参数表示IP地址，如果用户IP和参数匹配，则可以访问
     * hasRole             |   如果有参数，参数表示角色，则其角色可以访问
     * permitAll           |   用户可以任意访问
     * rememberMe          |   允许通过remember-me登录的用户访问
     * authenticated       |   用户登录后可访问
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity httpSecurity) throws Exception {
        // 获取@PermitAll的接口url
        Multimap<HttpMethod, String> permitAllUrls = getPermitAllUrlsFromAnnotations();
        // 基础配置
        httpSecurity
                // 开启跨域
                .cors(Customizer.withDefaults())
                // CSRF 禁用，因为不使用 Session
                .csrf(AbstractHttpConfigurer::disable)
                // 基于 token 机制，所以不需要 Session
                .sessionManagement(c -> c.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(c -> c.frameOptions(HeadersConfigurer.FrameOptionsConfig::disable))
                // 自定义认证、授权失败处理器
                .exceptionHandling(c -> c
                        .authenticationEntryPoint(new AppAuthenticationEntryPoint())
                        .accessDeniedHandler(new AppAccessDeniedHandler())
                );
        // 认证、授权
        httpSecurity
                .authorizeRequests()
                // 不需要登录认证就能访问的资源
                .antMatchers(Constant.IGNORE_APP_URL_ARRAY).permitAll()
                // open-api
                .antMatchers(Constant.IGNORE_KNIFE4J_URL_ARRAY).permitAll()
                // 所有门户接口不需要登录
                .antMatchers("/portal/**").permitAll()
                // 1.2 设置 @PermitAll 无需认证
                .antMatchers(HttpMethod.GET, permitAllUrls.get(HttpMethod.GET).toArray(new String[0])).permitAll()
                .antMatchers(HttpMethod.POST, permitAllUrls.get(HttpMethod.POST).toArray(new String[0])).permitAll()
                .antMatchers(HttpMethod.PUT, permitAllUrls.get(HttpMethod.PUT).toArray(new String[0])).permitAll()
                .antMatchers(HttpMethod.DELETE, permitAllUrls.get(HttpMethod.DELETE).toArray(new String[0])).permitAll()
                // 剩余接口都需要认证
                .antMatchers("/").authenticated()
                // 认证通过后，授权校验
                // 由于使用的Expression表达统一处理授权，不需要所有接口贴上注解，
                // 要求菜单数据配置的权限标识都是接口请求路径，不要使用restful风格，否则需要特殊处理
                .anyRequest().access("@acl.hasPermission(authentication)");

        // Token Filter
        httpSecurity.addFilterBefore(tokenAuthenticationAuthFilter(), UsernamePasswordAuthenticationFilter.class);
        return httpSecurity.build();
    }

    private Multimap<HttpMethod, String> getPermitAllUrlsFromAnnotations() {
        Multimap<HttpMethod, String> result = HashMultimap.create();
        // 获得接口对应的 HandlerMethod 集合
        RequestMappingHandlerMapping requestMappingHandlerMapping = SpringUtil.getBean("requestMappingHandlerMapping");
        Map<RequestMappingInfo, HandlerMethod> handlerMethodMap = requestMappingHandlerMapping.getHandlerMethods();
        // 获得有 @PermitAll 注解的接口
        for (Map.Entry<RequestMappingInfo, HandlerMethod> entry : handlerMethodMap.entrySet()) {
            HandlerMethod handlerMethod = entry.getValue();
            if (!handlerMethod.hasMethodAnnotation(PermitAll.class)) {
                continue;
            }
            log.info("permitAll method：{}", handlerMethod.getMethod().getName());
            Set<String> urls = new HashSet<>();
            if (entry.getKey().getPatternsCondition() != null) {
                urls.addAll(entry.getKey().getPatternsCondition().getPatterns());
            }
            if (entry.getKey().getPathPatternsCondition() != null) {
                urls.addAll(CollectionUtils.convertList(entry.getKey().getPathPatternsCondition().getPatterns(), PathPattern::getPatternString));
            }
            if (urls.isEmpty()) {
                continue;
            }

            // 特殊：使用 @RequestMapping 注解，并且未写 method 属性，此时认为都需要免登录
            Set<RequestMethod> methods = entry.getKey().getMethodsCondition().getMethods();
            if (CollUtil.isEmpty(methods)) {
                result.putAll(HttpMethod.GET, urls);
                result.putAll(HttpMethod.POST, urls);
                result.putAll(HttpMethod.PUT, urls);
                result.putAll(HttpMethod.DELETE, urls);
                result.putAll(HttpMethod.HEAD, urls);
                result.putAll(HttpMethod.PATCH, urls);
                continue;
            }
            // 根据请求方法，添加到 result 结果
            entry.getKey().getMethodsCondition().getMethods().forEach(requestMethod -> {
                switch (requestMethod) {
                    case GET:
                        result.putAll(HttpMethod.GET, urls);
                        break;
                    case POST:
                        result.putAll(HttpMethod.POST, urls);
                        break;
                    case PUT:
                        result.putAll(HttpMethod.PUT, urls);
                        break;
                    case DELETE:
                        result.putAll(HttpMethod.DELETE, urls);
                        break;
                    case HEAD:
                        result.putAll(HttpMethod.HEAD, urls);
                        break;
                    case PATCH:
                        result.putAll(HttpMethod.PATCH, urls);
                        break;
                }
            });
        }
        return result;
    }

}
