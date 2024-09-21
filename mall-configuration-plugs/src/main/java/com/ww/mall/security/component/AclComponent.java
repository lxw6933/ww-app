package com.ww.mall.security.component;

import com.ww.mall.utils.HttpContextUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import javax.servlet.http.HttpServletRequest;
import java.util.Collection;

/**
 * @author ww
 * @create 2024-09-20- 18:04
 * @description:
 */
@Slf4j
@Component("acl")
public class AclComponent {

    private final AntPathMatcher antPathMatcher = new AntPathMatcher();

    public boolean hasPermission(Authentication authentication) {
        log.info("acl 权限控制: {}", authentication);
        HttpServletRequest request = HttpContextUtils.getHttpServletRequest();
        // 获取请求uri
        String uri = request.getRequestURI();
        // 获取登录用户所有权限
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        boolean hasPermission = false;
        // 判断是否有权限访问uri
        for (GrantedAuthority url : authorities) {
            if (antPathMatcher.match(url.getAuthority(), uri)) {
                hasPermission = true;
                break;
            }
        }
        return hasPermission;
    }
}
