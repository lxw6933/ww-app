package com.ww.mall.security.component;

import com.ww.mall.utils.HttpContextUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author ww
 * @create 2024-09-20- 18:04
 * @description:
 */
@Slf4j
@Component("ss")
public class MallAuthComponent {

    private final AntPathMatcher antPathMatcher = new AntPathMatcher();

    public boolean hasPermission(Authentication authentication) {
        HttpServletRequest request = HttpContextUtils.getHttpServletRequest();
        // 获取请求uri
        String uri = request.getRequestURI();
        // TODO 获取所有需要授权的资源【redis获取】
        List<String> urlList = new ArrayList<>();
        // 判断请求的url是否存在
        boolean flag = urlList.stream().anyMatch(res -> res.equals(uri));

        if (flag) {
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
        } else {
            return true;
        }

    }

}
