package com.ww.mall.websocket.annotation;

import com.ww.mall.websocket.config.WebSocketServerAutoConfiguration;
import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author ww
 * @create 2024-05-15- 15:24
 * @description:
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Import({WebSocketServerAutoConfiguration.class})
public @interface EnableAppWebSocket {
}
