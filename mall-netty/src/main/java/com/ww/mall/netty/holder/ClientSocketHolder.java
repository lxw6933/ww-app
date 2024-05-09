package com.ww.mall.netty.holder;

import io.netty.channel.socket.nio.NioSocketChannel;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author ww
 * @create 2024-05-09- 10:30
 * @description:
 */
public class ClientSocketHolder {

    private static final Map<String, NioSocketChannel> clientSocketMap = new ConcurrentHashMap<>(16);

    public static void put(String id, NioSocketChannel socketChannel) {
        clientSocketMap.put(id, socketChannel);
    }

    public static NioSocketChannel getClientSocket(String id) {
        return clientSocketMap.get(id);
    }

    public static Map<String, NioSocketChannel> getAllClientSocket() {
        return clientSocketMap;
    }

    public static void removeClientSocket(NioSocketChannel clientSocket) {
        clientSocketMap.entrySet().stream().filter(entry -> entry.getValue() == clientSocket).forEach(entry -> clientSocketMap.remove(entry.getKey()));
    }

}
