package com.ww.mall.security;

import com.ww.mall.security.filter.TokenAuthenticationAuthFilter;
import com.ww.mall.security.handler.MallAccessDeniedHandler;
import com.ww.mall.security.handler.MallAuthenticationEntryPoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * @author ww
 * @create 2024-09-20- 14:29
 * @description:
 */
@Configuration
public class MallSecurityAutoConfiguration extends WebSecurityConfigurerAdapter {

    @Bean
    public TokenAuthenticationAuthFilter tokenAuthenticationAuthFilter() {
        return new TokenAuthenticationAuthFilter();
    }

    /**
     * 由于 Spring Security 创建 AuthenticationManager 对象时，没声明 @Bean 注解，导致无法被注入
     */
    @Bean
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }

    /**
     * 自定义认证方式
     */
    @Override
    protected void configure(AuthenticationManagerBuilder auth) {
        // 不使用security 的认证逻辑
    }
//    /**
//     * 密码加密方式
//     */
//    @Bean
//    public PasswordEncoder passwordEncoder() {
//        return new BCryptPasswordEncoder(4);
//    }

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
    @Override
    protected void configure(HttpSecurity httpSecurity) throws Exception {
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
                        .authenticationEntryPoint(new MallAuthenticationEntryPoint())
                        .accessDeniedHandler(new MallAccessDeniedHandler())
                );
        // 认证、授权
        httpSecurity
                .authorizeRequests()
                // 不需要登录认证就能访问的资源
                .antMatchers("/actuator/**", "/websocket/**", "/login/**", "/**/inner/**").permitAll()
                // 所有门户接口不需要登录
                .antMatchers("/portal/**").permitAll()
                // 剩余接口都需要认证
                .antMatchers("/").authenticated()
                // 认证通过后，授权校验
                // 由于使用的Expression表达统一处理授权，不需要所有接口贴上注解，
                // 要求菜单数据配置的权限标识都是接口请求路径，不要使用restful风格，否则需要特殊处理
                .anyRequest().access("@acl.hasPermission(authentication)");

        // Token Filter
        httpSecurity.addFilterBefore(tokenAuthenticationAuthFilter(), UsernamePasswordAuthenticationFilter.class);
    }

}
