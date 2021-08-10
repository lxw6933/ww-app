package com.ww.mall.config.security.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ww.mall.config.security.entity.MyUserDetails;
import com.ww.mall.mvc.entity.SysUserEntity;
import com.ww.mall.mvc.service.SysUserService;
import com.ww.mall.mvc.view.vo.admin.SysPermissionVO;
import com.ww.mall.mvc.view.vo.admin.SysRoleVO;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @description:
 * @author: ww
 * @create: 2021/6/26 上午7:38
 **/
@Component
public class MyUserDetailsService implements UserDetailsService {

    @Resource
    private SysUserService sysUserService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        SysUserEntity user = sysUserService.getOne(new QueryWrapper<SysUserEntity>()
                .eq("username", username)
        );
        if (user != null) {
            List<String> authorities = loadUserAllInfo(username);
            if (user.getStatus() == 1) {
                return new MyUserDetails(user.getId(), user.getCenterId(), username, user.getPassword(), AuthorityUtils.commaSeparatedStringToAuthorityList(String.join(",", authorities)));
            } else {
                return new MyUserDetails(user.getId(), user.getCenterId(), username, user.getPassword(), AuthorityUtils.commaSeparatedStringToAuthorityList(String.join(",", authorities)), false);
            }
        } else {
            return null;
        }
    }

    /**
     * 获取登录用户的角色和权限信息
     *
     * @param username 登录账号
     * @return List<String> 权限url和角色标识符
     */
    public List<String> loadUserAllInfo(String username) {
        // 封装用户角色权限和资源权限
        List<String> authorities = new ArrayList<>();
        // 获取用户角色信息
        List<SysRoleVO> roles = sysUserService.queryUserOfRole(username);
        List<String> roleNos = null;
        if (CollectionUtils.isNotEmpty(roles)) {
            roleNos = roles.stream()
                    .map(SysRoleVO::getRoleNo)
                    .collect(Collectors.toList());
        }

        if (CollectionUtils.isNotEmpty(roleNos)) {
            authorities.addAll(roleNos);
        }
        // 获取用户资源权限信息
        List<SysPermissionVO> permissions = sysUserService.queryUserOfPermission(username);
        List<String> urls = null;
        if (CollectionUtils.isNotEmpty(permissions)) {
            urls = permissions.stream()
                    .map(SysPermissionVO::getUrl)
                    .collect(Collectors.toList());
        }
        if (CollectionUtils.isNotEmpty(urls)) {
            authorities.addAll(urls);
        }
        return authorities;
    }

}
