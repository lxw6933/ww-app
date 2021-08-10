package com.ww.mall.config.security.service;

import com.ww.mall.common.constant.RedisKeyConstant;
import com.ww.mall.config.redis.RedisManager;
import com.ww.mall.utils.LoginUserUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.List;

/**
 * @description: 权限认证
 * @author: ww
 * @create: 2021/6/26 上午7:58
 **/
@Component("urlAuth")
public class MyUrlAuthService {

    private AntPathMatcher antPathMatcher = new AntPathMatcher();

    @Resource
    private RedisManager redisManager;

    public boolean hasPermission(HttpServletRequest request, Authentication authentication){
        // 获取请求uri
        String uri = request.getRequestURI();
        // 获取所有权限url（如果请求uri不在所有权限中，则不拦截，直接放行）
        List<String> urlList = (List<String>) redisManager.get(RedisKeyConstant.ALL_PERMISSIONS_URL);
        // 获取访问人账号
        String currentUser = LoginUserUtils.getCurrentUserName();
        // root用户无需认证
        if(StringUtils.equals("root",currentUser)) {
            return true;
        }
        // 判断请求的url是否存在
        boolean flag = urlList.stream().anyMatch(res -> res.equals(uri));

        if(flag) {
            // 获取登录用户所有权限
            Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
            boolean hasPermission = false;
            //IP、sessionId
            Object details = authentication.getDetails();
            // 判断是否有权限访问uri
            for (GrantedAuthority url : authorities) {
                if (antPathMatcher.match(url.getAuthority(), uri)) {
                    hasPermission = true;
                    break;
                }
            }
            return hasPermission;
        }else{
            return true;
        }

    }

}
