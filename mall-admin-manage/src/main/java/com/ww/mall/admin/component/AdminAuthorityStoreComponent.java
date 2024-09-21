package com.ww.mall.admin.component;

import com.alibaba.fastjson.JSON;
import com.ww.mall.common.constant.RedisKeyConstant;
import com.ww.mall.security.component.AuthorityStore;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * @author ww
 * @create 2024-09-21 23:17
 * @description:
 */
@Component
public class AdminAuthorityStoreComponent implements AuthorityStore {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Override
    public List<GrantedAuthority> loadCurrentUserAuthorities(Long userId) {
        String userAuthoritiesStr = redisTemplate.opsForValue().get(RedisKeyConstant.USER_AUTHORITIES + userId);
        List<String> authorities = JSON.parseArray(userAuthoritiesStr, String.class);

        return CollectionUtils.isEmpty(authorities) ?
                Collections.emptyList() :
                AuthorityUtils.commaSeparatedStringToAuthorityList(String.join(",", authorities));
    }

}
