package com.ww.app.ssh.config;

import com.ww.app.ssh.ws.LogStreamWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket 注册配置。
 * <p>
 * 将日志流处理器挂载到固定路径，供前端页面建立实时连接。
 * </p>
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    /**
     * 日志流处理器。
     */
    private final LogStreamWebSocketHandler logStreamWebSocketHandler;

    /**
     * 构造方法。
     *
     * @param logStreamWebSocketHandler 日志流处理器
     */
    public WebSocketConfig(LogStreamWebSocketHandler logStreamWebSocketHandler) {
        this.logStreamWebSocketHandler = logStreamWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(logStreamWebSocketHandler, "/log-stream").setAllowedOrigins("*");
    }
}
