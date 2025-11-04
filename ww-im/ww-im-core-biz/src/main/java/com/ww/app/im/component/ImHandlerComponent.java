package com.ww.app.im.component;

import cn.hutool.extra.spring.SpringUtil;
import com.ww.app.im.common.ImMsg;
import com.ww.app.im.handler.msg.ImMsgHandlerAdapter;
import com.ww.app.im.utils.ImContextUtils;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * @author ww
 * @create 2024-11-09 20:32
 * @description: IM消息处理器组件 - 已优化
 */
@Slf4j
@Component
public class ImHandlerComponent implements InitializingBean {

    private final Map<Integer, ImMsgHandlerAdapter> imMsgHandlerMap = new HashMap<>();

    @Override
    public void afterPropertiesSet() {
        Map<String, ImMsgHandlerAdapter> matchingAdapterBeans = SpringUtil.getBeansOfType(ImMsgHandlerAdapter.class);
        if (!matchingAdapterBeans.isEmpty()) {
            matchingAdapterBeans.values().forEach(handler -> {
                imMsgHandlerMap.put(handler.getMsgType().getCode(), handler);
                log.info("注册消息处理器: msgType={}, handler={}", 
                        handler.getMsgType().getCode(), handler.getClass().getSimpleName());
            });
        }
    }

    public void handle(ChannelHandlerContext ctx, ImMsg imMsg) {
        ImMsgHandlerAdapter adapter = this.imMsgHandlerMap.get(imMsg.getMsgType());
        if (adapter == null) {
            Long userId = ImContextUtils.getUserId(ctx);
            log.error("消息处理器未找到: msgType={}, userId={}, channel={}", 
                    imMsg.getMsgType(), userId, ctx.channel());
            // 不抛出异常，避免导致连接关闭
            // 可以考虑发送错误响应给客户端
            return;
        }
        
        try {
            adapter.handle(ctx, imMsg);
        } catch (Exception e) {
            log.error("消息处理失败: msgType={}, userId={}, error={}", 
                    imMsg.getMsgType(), ImContextUtils.getUserId(ctx), e.getMessage(), e);
            // 异常不向上抛，避免影响其他消息处理
        }
    }

}
