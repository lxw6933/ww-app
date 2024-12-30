package com.ww.app.admin.component;

import com.ww.app.admin.component.key.AuthorityRedisKeyBuilder;
import com.ww.app.security.component.AuthorityStore;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;

/**
 * @author ww
 * @create 2024-09-21 23:17
 * @description:
 */
@Component
public class AdminAuthorityStoreComponent implements AuthorityStore {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private AuthorityRedisKeyBuilder authorityRedisKeyBuilder;

    @Override
    public List<GrantedAuthority> loadCurrentUserAuthorities(Long userId) {
        List<String> authorities = (List<String>) redisTemplate.opsForValue().get(authorityRedisKeyBuilder.buildUserAuthoritiesKey(userId));

        return CollectionUtils.isEmpty(authorities) ?
                Collections.emptyList() :
                AuthorityUtils.commaSeparatedStringToAuthorityList(String.join(",", authorities));
    }

}
