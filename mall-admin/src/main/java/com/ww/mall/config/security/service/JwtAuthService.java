package com.ww.mall.config.security.service;

import com.ww.mall.utils.JwtTokenUtils;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * @description: Jwt 管理(生成和刷新)
 * @author: ww
 * @create: 2021/6/26 上午7:51
 **/
@Component
public class JwtAuthService {

    @Resource
    private AuthenticationManager authenticationManager;

    @Resource
    private JwtTokenUtils jwtTokenUtils;

    /**
     * 登录认证成功后生成对应的token
     *
     * @return token
     */
    public String login(String username, String password) throws AuthenticationException {
        UsernamePasswordAuthenticationToken userToken = new UsernamePasswordAuthenticationToken(username, password);
        /**
         * 将token放入AuthenticationManager.authenticate()中进行认证判断（是否认证成功）
         * AuthenticationManager.authenticate()方法的实现类 ProviderManager
         * ProviderManager.authenticate()里面实际上是调用实现了AuthenticationProvider认证接口的认证类authenticate()
         * AbstractUserDetailsAuthenticationProvider.authenticate()真正认证实现类
         * 判断缓存中是否存在UserDetails ===>  this.userCache.getUserFromCache(username);
         * 如果缓存中不存在 ===> 子类DaoAuthenticationProvider实现了方法加载UserDetails信息  ===> retrieveUser(username,(UsernamePasswordAuthenticationToken) authentication);
         *                                                                             ===> UserDetails loadedUser = this.getUserDetailsService().loadUserByUsername(username);
         *                                                                             ===> 我们实现UserDetailsService接口，重写loadUserByUsername();  从数据库中加载用户信息
         * 内部类DefaultPreAuthenticationChecks  ===>  preAuthenticationChecks.check(user);      检查UserDetails 1：账号是否锁定；2：账号是否可用；3：账号是否过期
         * 子类DaoAuthenticationProvider实现了方法检查密码的准确性  ===>  additionalAuthenticationChecks(user,(UsernamePasswordAuthenticationToken) authentication);
         * AbstractUserDetailsAuthenticationProvider.authenticate() 最后返回 createSuccessAuthentication(principalToReturn, authentication, user);
         *                                                                   ===> new UsernamePasswordAuthenticationToken(principal, authentication.getCredentials(),authoritiesMapper.mapAuthorities(user.getAuthorities()));
         *                                                                   ===> super(authorities);this.principal = principal;this.credentials = credentials;super.setAuthenticated(true);   验证成功
         *  认证失败会抛出异常
         */
        Authentication authenticate = authenticationManager.authenticate(userToken);
        SecurityContextHolder.getContext().setAuthentication(authenticate);
//        UserDetails userDetails = myUserDetailsService.loadUserByUsername(username);
        UserDetails userDetails = (UserDetails) authenticate.getPrincipal();
        //根据UserDetails生成token
        return jwtTokenUtils.generateToken(userDetails);
    }

    /**
     * 刷新token
     *
     * @param oldToken oldToken
     */
    public String refreshToken(String oldToken) throws Exception {
        if (!jwtTokenUtils.isTokenExpired(oldToken)) {
            return jwtTokenUtils.refreshToken(oldToken);
        }
        return null;
    }

}
