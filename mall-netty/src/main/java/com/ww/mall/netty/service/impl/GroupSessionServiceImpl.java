package com.ww.mall.netty.service.impl;

import com.ww.mall.netty.entity.Group;
import com.ww.mall.netty.service.GroupSessionService;
import com.ww.mall.netty.service.SessionService;
import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.ww.mall.common.utils.CollectionUtils.convertList;

/**
 * @author ww
 * @create 2024-05-07 21:41
 * @description: 群聊会话实现
 */
@Slf4j
@Service
public class GroupSessionServiceImpl implements GroupSessionService {

    @Resource
    private SessionService sessionService;

    /**
     * 聊天室集合
     */
    private final Map<String, Group> groupMap = new ConcurrentHashMap<>();

    @Override
    public Group createGroup(String name, Set<String> members) {
        Group group = new Group(name, members);
        return groupMap.putIfAbsent(name, group);
    }

    @Override
    public Group joinMember(String name, String member) {
        return groupMap.computeIfPresent(name, (key, value) -> {
            value.getMembers().add(member);
            return value;
        });
    }

    @Override
    public Group removeMember(String name, String member) {
        return groupMap.computeIfPresent(name, (key, value) -> {
            value.getMembers().remove(member);
            return value;
        });
    }

    @Override
    public Group removeGroup(String name) {
        return groupMap.remove(name);
    }

    @Override
    public Set<String> getMembers(String name) {
        return groupMap.getOrDefault(name, Group.EMPTY_GROUP).getMembers();
    }

    @Override
    public List<Channel> getMembersChannel(String name) {
        return convertList(getMembers(name), member -> sessionService.getChannel(member));
    }

}
