package com.ww.mall.im.handler.component;

import cn.hutool.extra.spring.SpringUtil;
import com.ww.mall.common.exception.ApiException;
import com.ww.mall.im.common.ImMsg;
import com.ww.mall.im.handler.msg.ImMsgHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * @author ww
 * @create 2024-11-09 20:32
 * @description:
 */
@Slf4j
@Component
public class ImHandlerComponent implements InitializingBean {

    private final Map<Integer, ImMsgHandlerAdapter> imMsgHandlerMap = new HashMap<>();

    @Override
    public void afterPropertiesSet() {
        Map<String, ImMsgHandlerAdapter> matchingAdapterBeans = SpringUtil.getBeansOfType(ImMsgHandlerAdapter.class);
        if (!matchingAdapterBeans.isEmpty()) {
            matchingAdapterBeans.values().forEach(handler -> imMsgHandlerMap.put(handler.getMsgType().getCode(), handler));
        }
    }

    public void handle(ChannelHandlerContext channelHandlerContext, ImMsg imMsg) {
        ImMsgHandlerAdapter imMsgHandlerAdapter = this.imMsgHandlerMap.get(imMsg.getMsgType());
        if (imMsgHandlerAdapter == null) {
            log.error("消息{}未找到相关处理器", imMsg);
            throw new ApiException("未找到消息处理器");
        }
        imMsgHandlerAdapter.handle(channelHandlerContext, imMsg);
    }

}
