package com.ww.mall.netty.holder;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelId;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
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

    public static String getClientIp(ChannelHandlerContext ctx) {
        // 获取客户端的IP地址和端口
        SocketAddress clientSocketAddress = ctx.channel().remoteAddress();
        // 从客户端地址中解析出IP地址
        return ((InetSocketAddress) clientSocketAddress).getAddress().getHostAddress();
    }

    public static int getClientPort(ChannelHandlerContext ctx) {
        // 获取客户端的IP地址和端口
        SocketAddress clientSocketAddress = ctx.channel().remoteAddress();
        // 从客户端地址中解析出IP地址
        return ((InetSocketAddress) clientSocketAddress).getPort();
    }

}
