package com.ww.mall.im.service.impl;

import com.ww.mall.im.service.OnLineService;
import com.ww.mall.im.utils.ImUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author ww
 * @create 2024-12-25 11:41
 * @description:
 */
@Slf4j
@Service
public class OnLineServiceImpl implements OnLineService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public boolean isOnline(Long userId, Integer appId) {
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(ImUtils.buildImBindIpKey(userId, appId)));
    }

}
