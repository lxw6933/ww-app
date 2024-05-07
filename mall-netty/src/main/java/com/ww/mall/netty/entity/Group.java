package com.ww.mall.netty.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.Set;

/**
 * @author ww
 * @create 2024-05-07 21:14
 * @description: 聊天室
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Group {

    /**
     * 聊天室名称
     */
    private String name;

    /**
     * 聊天室成员
     */
    private Set<String> members;

    /**
     * 聊天室集合
     */
    public static final Group EMPTY_GROUP = new Group("empty", Collections.emptySet());
}
