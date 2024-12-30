package com.ww.app.security.component;

import org.springframework.security.core.GrantedAuthority;

import java.util.List;

/**
 * @author ww
 * @create 2024-09-21 23:18
 * @description:
 */
public interface AuthorityStore {

    /**
     * 获取当前用户资源权限
     *
     * @return List<GrantedAuthority>
     */
    List<GrantedAuthority> loadCurrentUserAuthorities(Long userId);

}
