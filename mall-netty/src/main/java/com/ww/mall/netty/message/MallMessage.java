package com.ww.mall.netty.message;

import lombok.Data;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * @author ww
 * @create 2024-05-06 22:07
 * @description:
 */
@Data
public abstract class MallMessage implements Serializable {

    private int sequenceId;

    private int messageType;

    /**
     * 获取消息类型
     */
    public abstract int getMessageType();

    // 登录message
    public static final int LOGIN_REQUEST_MESSAGE_TYPE = 0;
    public static final int LOGIN_RESPONSE_MESSAGE_TYPE = 1;
    // 聊天message
    public static final int CHAT_REQUEST_MESSAGE_TYPE = 2;
    public static final int CHAT_RESPONSE_MESSAGE_TYPE = 3;
    // 创建群组message
    public static final int GROUP_CREATE_REQUEST_MESSAGE_TYPE = 4;
    public static final int GROUP_CREATE_RESPONSE_MESSAGE_TYPE = 5;
    // 加入群组message
    public static final int GROUP_JOIN_REQUEST_MESSAGE_TYPE = 6;
    public static final int GROUP_JOIN_RESPONSE_MESSAGE_TYPE = 7;
    // 离开群组message
    public static final int GROUP_QUIT_REQUEST_MESSAGE_TYPE = 8;
    public static final int GROUP_QUIT_RESPONSE_MESSAGE_TYPE = 9;
    // 群组聊天message
    public static final int GROUP_CHAT_REQUEST_MESSAGE_TYPE = 10;
    public static final int GROUP_CHAT_RESPONSE_MESSAGE_TYPE = 11;
    //
    public static final int GROUP_MEMBERS_REQUEST_MESSAGE_TYPE = 12;
    public static final int GROUP_MEMBERS_RESPONSE_MESSAGE_TYPE = 13;
    // 心跳message
    public static final int PING_MESSAGE_TYPE = 14;
    public static final int PONG_MESSAGE_TYPE = 15;
    // rpc请求message
    public static final int RPC_MESSAGE_REQUEST_TYPE = 101;
    public static final int RPC_MESSAGE_RESPONSE_TYPE = 102;

    /**
     * 所有消息类型map
     */
    private static final Map<Integer, Class<? extends MallMessage>> messageClasses = new HashMap<>();

    /**
     * 根据消息类型字节，获得对应的消息 class
     *
     * @param messageType 消息类型字节
     * @return 消息 class
     */
    public static Class<? extends MallMessage> getMessageClass(int messageType) {
        return messageClasses.get(messageType);
    }

    static {
        messageClasses.put(LOGIN_REQUEST_MESSAGE_TYPE, LoginRequestMessage.class);
        messageClasses.put(LOGIN_RESPONSE_MESSAGE_TYPE, LoginResponseMessage.class);
        messageClasses.put(CHAT_REQUEST_MESSAGE_TYPE, ChatRequestMessage.class);
        messageClasses.put(CHAT_RESPONSE_MESSAGE_TYPE, ChatResponseMessage.class);
        messageClasses.put(GROUP_CREATE_REQUEST_MESSAGE_TYPE, GroupCreateRequestMessage.class);
        messageClasses.put(GROUP_CREATE_RESPONSE_MESSAGE_TYPE, GroupCreateResponseMessage.class);
        messageClasses.put(GROUP_JOIN_REQUEST_MESSAGE_TYPE, GroupJoinRequestMessage.class);
        messageClasses.put(GROUP_JOIN_RESPONSE_MESSAGE_TYPE, GroupJoinResponseMessage.class);
        messageClasses.put(GROUP_QUIT_REQUEST_MESSAGE_TYPE, GroupQuitRequestMessage.class);
        messageClasses.put(GROUP_QUIT_RESPONSE_MESSAGE_TYPE, GroupQuitResponseMessage.class);
        messageClasses.put(GROUP_CHAT_REQUEST_MESSAGE_TYPE, GroupChatRequestMessage.class);
        messageClasses.put(GROUP_CHAT_RESPONSE_MESSAGE_TYPE, GroupChatResponseMessage.class);
        messageClasses.put(GROUP_MEMBERS_REQUEST_MESSAGE_TYPE, GroupMembersRequestMessage.class);
        messageClasses.put(GROUP_MEMBERS_RESPONSE_MESSAGE_TYPE, GroupMembersResponseMessage.class);
        messageClasses.put(RPC_MESSAGE_REQUEST_TYPE, RpcRequestMessage.class);
        messageClasses.put(RPC_MESSAGE_RESPONSE_TYPE, RpcResponseMessage.class);
    }

}
