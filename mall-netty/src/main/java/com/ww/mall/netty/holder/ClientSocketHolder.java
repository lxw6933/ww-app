package com.ww.mall.netty.holder;

import io.netty.channel.ChannelId;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author ww
 * @create 2024-05-09- 10:30
 * @description:
 */
public class ClientSocketHolder {

    private static final Map<ChannelId, NioSocketChannel> clientSocketMap = new ConcurrentHashMap<>(16);

    public static void put(ChannelId channelId, NioSocketChannel socketChannel) {
        clientSocketMap.put(channelId, socketChannel);
    }

    public static NioSocketChannel getClientSocket(ChannelId channelId) {
        return clientSocketMap.get(channelId);
    }

    public static Map<ChannelId, NioSocketChannel> getAllClientSocket() {
        return clientSocketMap;
    }

    public static void removeClientSocket(ChannelId channelId) {
        clientSocketMap.remove(channelId);
    }

    public static void removeClientSocket(NioSocketChannel clientSocket) {
        clientSocketMap.entrySet().stream().filter(entry -> entry.getValue() == clientSocket).forEach(entry -> clientSocketMap.remove(entry.getKey()));
    }

}
