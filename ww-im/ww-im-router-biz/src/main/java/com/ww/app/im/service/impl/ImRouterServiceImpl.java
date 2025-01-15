package com.ww.app.im.service.impl;

import cn.hutool.core.util.StrUtil;
import com.ww.app.im.core.api.common.ImMsgBody;
import com.ww.app.im.core.api.rpc.ImMsgRouterApi;
import com.ww.app.im.service.ImRouterService;
import com.ww.app.im.core.api.utils.ImUtils;
import com.ww.app.web.holder.ServerIpContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

import static com.ww.app.common.utils.CollectionUtils.convertMap;

/**
 * @author ww
 * @create 2024-12-24 17:28
 * @description:
 */
@Slf4j
@Service
public class ImRouterServiceImpl implements ImRouterService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private ImMsgRouterApi imMsgRouterApi;

    @Override
    public boolean sendMsg(ImMsgBody imMsgBody) {
        // 获取接收消息用户所在的im server ip
        String imServerIpCache = stringRedisTemplate.opsForValue().get(ImUtils.buildImBindIpKey(imMsgBody.getUserId(), imMsgBody.getAppId()));
        if (StrUtil.isBlank(imServerIpCache)) {
            log.error("未找到接收者连接的im server");
            return false;
        }
        String imServerIp = ImUtils.getImBindIp(imServerIpCache);
        // 远程调用im server ip给用户发送消息
        ServerIpContextHolder.set(imServerIp);
        imMsgRouterApi.sendMsg(imMsgBody);
        return true;
    }

    @Override
    public void batchSendMsg(List<ImMsgBody> imMsgBodyList) {
        // 批量发送消息，上游校验userId唯一、appId相同
        Map<Long, ImMsgBody> userIdMsgMap = convertMap(imMsgBodyList, ImMsgBody::getUserId);
        Set<Long> userIdList = userIdMsgMap.keySet();
        // 业务id
        Integer appId = imMsgBodyList.get(0).getAppId();

        // 获取所有用户绑定的im server ip
        List<String> imServerBindIpKeyList = new ArrayList<>();
        userIdList.forEach(userId -> imServerBindIpKeyList.add(ImUtils.buildImBindIpKey(userId, appId)));
        // 批量取出每个用户绑定的ip地址缓存信息
        List<String> imServerBindIpCacheList = stringRedisTemplate.opsForValue().multiGet(imServerBindIpKeyList);
        // 过滤掉下线用户
        imServerBindIpCacheList = imServerBindIpCacheList != null ?
                imServerBindIpCacheList.stream().filter(Objects::nonNull).collect(Collectors.toList()) :
                new ArrayList<>();

        // 分组整合im server ip 下所有的用户
        Map<String, List<Long>> imServerIpUserIdsMap = new HashMap<>();
        imServerBindIpCacheList.forEach(imServerIpCache -> {
            String imServerIp = ImUtils.getImBindIp(imServerIpCache);
            Long userId = Long.valueOf(ImUtils.getImBindUserId(imServerIpCache));

            List<Long> currentUserIdList = imServerIpUserIdsMap.get(imServerIp);
            if (currentUserIdList == null) {
                currentUserIdList = new ArrayList<>();
            }
            currentUserIdList.add(userId);
            imServerIpUserIdsMap.put(imServerIp, currentUserIdList);
        });

        // 将连接同一台ip地址的imMsgBody组装到同一个list集合中，然后进行统一的发送
        for (String imServerIp : imServerIpUserIdsMap.keySet()) {
            List<ImMsgBody> batchSendMsgGroupByIpList = new ArrayList<>();
            // 当前imServerIp下所有的用户
            List<Long> ipBindUserIdList = imServerIpUserIdsMap.get(imServerIp);
            for (Long userId : ipBindUserIdList) {
                // 需要发送的消息
                ImMsgBody imMsgBody = userIdMsgMap.get(userId);
                batchSendMsgGroupByIpList.add(imMsgBody);
            }
            ServerIpContextHolder.set(imServerIp);
            // 批量发送im server下的用户消息
            imMsgRouterApi.batchSendMsg(batchSendMsgGroupByIpList);
        }
    }
}
