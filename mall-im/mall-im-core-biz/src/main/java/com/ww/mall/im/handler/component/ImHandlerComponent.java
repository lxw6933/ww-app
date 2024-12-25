package com.ww.mall.im.handler.component;

import cn.hutool.core.lang.Assert;
import cn.hutool.extra.spring.SpringUtil;
import com.ww.mall.common.exception.ApiException;
import com.ww.mall.im.common.ImMsg;
import com.ww.mall.im.common.ImMsgBody;
import com.ww.mall.im.configuration.ImProperties;
import com.ww.mall.im.handler.msg.ImMsgHandlerAdapter;
import com.ww.mall.im.serialize.ImMsgSerializer;
import com.ww.mall.im.serialize.JsonSerializer;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
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

    private ImMsgSerializer defaultSerializer;

    private final Map<Integer, ImMsgSerializer> imMsgSerializerMap = new HashMap<>();

    private final Map<Integer, ImMsgHandlerAdapter> imMsgHandlerMap = new HashMap<>();

    @Resource
    private ImProperties imProperties;

    @Override
    public void afterPropertiesSet() {
        Map<String, ImMsgHandlerAdapter> matchingAdapterBeans = SpringUtil.getBeansOfType(ImMsgHandlerAdapter.class);
        if (!matchingAdapterBeans.isEmpty()) {
            matchingAdapterBeans.values().forEach(handler -> imMsgHandlerMap.put(handler.getMsgType().getCode(), handler));
        }
        Map<String, ImMsgSerializer> matchingSerializeBeans = SpringUtil.getBeansOfType(ImMsgSerializer.class);
        if (!matchingSerializeBeans.isEmpty()) {
            matchingSerializeBeans.values().forEach(res -> imMsgSerializerMap.put(res.type().getType(), res));
        }
        if (defaultSerializer == null) {
            defaultSerializer = new JsonSerializer();
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

    public ImMsgBody deserializeMsg(ImMsg imMsg) {
        ImMsgSerializer imMsgSerializer = this.imMsgSerializerMap.get((int) imMsg.getSerializeType());
        Assert.notNull(imMsgSerializer, () -> new IllegalArgumentException("不支持消息序列化方式"));
        return imMsgSerializer.deserialize(ImMsgBody.class, imMsg.getBody());
    }

    public ImMsgSerializer getDefaultSerializer() {
        ImMsgSerializer defaultSerializer = this.imMsgSerializerMap.get(imProperties.getSerializerType());
        return defaultSerializer == null ? this.imMsgSerializerMap.get(1) : defaultSerializer;
    }

}
