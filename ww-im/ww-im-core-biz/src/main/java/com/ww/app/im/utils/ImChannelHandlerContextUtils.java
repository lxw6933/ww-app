package com.ww.app.im.utils;

import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author ww
 * @create 2024-11-09 23:56
 * @description: IM连接管理器 - 已优化
 */
@Slf4j
public class ImChannelHandlerContextUtils {

    /**
     * 当前im服务器 ip
     */
    private static String IM_SERVER_IP = "";

    /**
     * im server 用户 channel 关联缓存 - 优化：增大初始容量
     */
    private static final Map<Long, ChannelHandlerContext> channelHandlerContextMap = 
            new ConcurrentHashMap<>(100000, 0.75f);
    
    /**
     * 连接数统计
     */
    private static final AtomicInteger connectionCount = new AtomicInteger(0);

    public static String getImServerIp() {
        return IM_SERVER_IP;
    }

    public static void setImServerIp(String serverIpAddress) {
        IM_SERVER_IP = serverIpAddress;
    }

    public static ChannelHandlerContext get(Long userId) {
        if (userId == null) {
            return null;
        }
        return channelHandlerContextMap.get(userId);
    }

    public static void set(Long userId, ChannelHandlerContext ctx) {
        if (userId == null || ctx == null) {
            return;
        }
        
        // 修复并发问题：判断是新连接还是替换连接
        ChannelHandlerContext oldCtx = channelHandlerContextMap.put(userId, ctx);
        
        if (oldCtx == null) {
            // 新连接
            int count = connectionCount.incrementAndGet();
            
            // 监控告警
            if (count % 10000 == 0) {
                log.info("当前连接数: {}", count);
            }
            
            if (count > 80000) {
                log.warn("⚠️ 连接数过高: {}, 接近上限", count);
            }
        } else {
            // 已存在的连接被替换（可能是重复登录）
            log.warn("用户{}的连接被替换: oldChannel={}, newChannel={}", 
                    userId, oldCtx.channel().id().asShortText(), ctx.channel().id().asShortText());
            // 关闭旧连接
            if (oldCtx.channel().isActive()) {
                oldCtx.close();
            }
        }
    }

    public static void remove(Long userId) {
        if (userId == null) {
            return;
        }
        
        ChannelHandlerContext removed = channelHandlerContextMap.remove(userId);
        if (removed != null) {
            connectionCount.decrementAndGet();
        }
    }
    
    /**
     * 获取当前连接数
     */
    public static int getConnectionCount() {
        return connectionCount.get();
    }
    
    /**
     * 定期清理无效连接（建议在定时任务中调用）
     */
    public static void cleanInactiveConnections() {
        log.info("开始清理无效连接...");
        int cleanedCount = 0;
        
        Iterator<Map.Entry<Long, ChannelHandlerContext>> iterator = 
                channelHandlerContextMap.entrySet().iterator();
        
        while (iterator.hasNext()) {
            Map.Entry<Long, ChannelHandlerContext> entry = iterator.next();
            ChannelHandlerContext ctx = entry.getValue();
            
            if (ctx == null || !ctx.channel().isActive()) {
                iterator.remove();
                connectionCount.decrementAndGet();
                cleanedCount++;
            }
        }
        
        log.info("清理完成，移除 {} 个无效连接，当前连接数: {}", 
                cleanedCount, connectionCount.get());
    }
}
