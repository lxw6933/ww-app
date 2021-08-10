package com.ww.mall.config.security.entity;

import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * @description: 登陆用户对象
 * @author: ww
 * @create: 2021/6/26 上午7:27
 **/
@Data
public class MyUserDetails implements UserDetails {

    private Long id;
    private Long centerId;
    private String username;
    private String password;
    /**
     * 账号是否没过期
     */
    private boolean accountNonExpired = true;
    /**
     * 账号是否没被冻结
     */
    private boolean accountNonLocked = true;
    /**
     * 密码是否没过期
     */
    private boolean credentialsNonExpired = true;
    /**
     * 是否可用
     */
    private boolean enabled = true;
    /**
     * 用户权限集合
     */
    Collection<? extends GrantedAuthority> authorities;

    public MyUserDetails(Long id, Long centerId,String username, String password, List<GrantedAuthority> authorities) {
        this(id, centerId, username, password, authorities, true);
    }

    public MyUserDetails(Long id, Long centerId, String username, String password, List<GrantedAuthority> authorities, boolean accountNonLocked) {
        this.id = id;
        this.centerId = centerId;
        this.username = username;
        this.password = password;
        this.authorities = authorities;
        this.accountNonLocked = accountNonLocked;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return this.authorities;
    }

    @Override
    public String getPassword() {
        return this.password;
    }

    @Override
    public String getUsername() {
        return this.username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return this.accountNonExpired;
    }

    @Override
    public boolean isAccountNonLocked() {
        return this.accountNonLocked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return this.credentialsNonExpired;
    }

    @Override
    public boolean isEnabled() {
        return this.enabled;
    }
}
