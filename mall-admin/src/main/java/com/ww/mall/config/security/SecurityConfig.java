package com.ww.mall.config.security;

import com.ww.mall.config.security.filter.JwtTokenFilter;
import com.ww.mall.config.security.handler.MyAccessDeniedHandler;
import com.ww.mall.config.security.handler.MyAuthenticationEntryPoint;
import com.ww.mall.config.security.service.MyUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import javax.annotation.Resource;

/**
 * @description: SpringSecurity 核心配置
 * EnableWebSecurity //开启security
 * EnableGlobalMethodSecurity //开启方法注解权限控制（可有可无）
 * @author: ww
 * @create: 2021/6/26 上午9:05
 **/
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Resource
    MyUserDetailsService myUserDetailsService;
    @Resource
    MyAccessDeniedHandler myAccessDeniedHandler;
    @Resource
    MyAuthenticationEntryPoint myAuthenticationEntryPoint;
    @Resource
    JwtTokenFilter jwtTokenFilter;

    /**
     * 密码加密方式
     *
     * @return 加密算法
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 授权
     *
     * @param http http请求
     * @throws Exception exception
     */
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.cors();
        http
//            .addFilterBefore(validCodeFilter,UsernamePasswordAuthenticationFilter.class)
            // 添加token过滤器
            .addFilterBefore(jwtTokenFilter, UsernamePasswordAuthenticationFilter.class)
            // 关闭csrf
            .csrf().disable()
            // 自定义异常处理器
            .exceptionHandling()
            // 自定义处理无权限访问资源异常
            .accessDeniedHandler(myAccessDeniedHandler)
            //自定义登录失败异常
            .authenticationEntryPoint(myAuthenticationEntryPoint)
            .and()
            .authorizeRequests()
            // 不需要登录认证就能访问的资源
            .antMatchers("/actuator/**", "/websocket/**", "/admin/sys/login/**")
            .permitAll()
            // 需要登录认证才能访问的资源
            .antMatchers("/")
            .authenticated()
            // 登录认证后，还需要资源权限才能访问的资源
            .anyRequest().access("@urlAuth.hasPermission(request, authentication)")
            .and()
            //开启session无状态
            .sessionManagement()
            .sessionCreationPolicy(SessionCreationPolicy.STATELESS);
    }

    /**
     * 认证方式
     *
     * @param auth AuthenticationManagerBuilder
     * @throws Exception exception
     */
    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(myUserDetailsService).passwordEncoder(passwordEncoder());
//        auth.authenticationProvider(myAuthenticationProvider);
    }

    /**
     * 将项目中静态资源路径开放出来不被拦截
     *
     * @param web web
     */
    @Override
    public void configure(WebSecurity web) {
        web.ignoring().antMatchers("/static/**");
    }

    @Bean
    @Override
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }

}
