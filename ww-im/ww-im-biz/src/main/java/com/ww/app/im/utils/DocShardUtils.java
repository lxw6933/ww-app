package com.ww.app.im.utils;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.HashUtil;
import cn.hutool.core.util.StrUtil;

import java.util.Date;

/**
 * @author ww
 * @create 2025-01-16- 17:00
 * @description:
 */
public class DocShardUtils {

    private static final int SINGLE_MSG_SHARD = 100;
    private static final int GROUP_MSG_SHARD = 100;
    private static final int GROUP_SHARD = 100;

    private static final String SINGLE_CHAT_DOC = "chat_single_message";
    private static final String GROUP_CHAT_DOC = "chat_group_message";
    private static final String GROUP_MEMBER_DOC = "group_member";

    public static String getSingleChatDocName(long userId, Date sendTime) {
        int docIndex = (int) (userId % SINGLE_MSG_SHARD);
        return StrUtil.join(StrUtil.UNDERLINE, SINGLE_CHAT_DOC, docIndex, DateUtil.format(sendTime, DatePattern.SIMPLE_MONTH_PATTERN));
    }

    public static String getGroupChatDocName(String groupId, Date sendTime) {
        int docIndex = (HashUtil.apHash(groupId) % GROUP_MSG_SHARD);
        return StrUtil.join(StrUtil.UNDERLINE, GROUP_CHAT_DOC, docIndex, DateUtil.format(sendTime, DatePattern.SIMPLE_MONTH_PATTERN));
    }

    public static String getGroupMemberDocName(String groupId) {
        int docIndex = (HashUtil.apHash(groupId) % GROUP_SHARD);
        return StrUtil.join(StrUtil.UNDERLINE, GROUP_MEMBER_DOC, docIndex);
    }

}
