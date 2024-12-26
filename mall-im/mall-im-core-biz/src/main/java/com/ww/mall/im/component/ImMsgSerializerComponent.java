package com.ww.mall.im.component;

import cn.hutool.core.lang.Assert;
import cn.hutool.extra.spring.SpringUtil;
import com.ww.mall.im.common.ImMsg;
import com.ww.mall.im.common.ImMsgBody;
import com.ww.mall.im.configuration.ImProperties;
import com.ww.mall.im.serialize.ImMsgSerializer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

/**
 * @author ww
 * @create 2024-12-26- 09:59
 * @description:
 */
@Slf4j
@Component
public class ImMsgSerializerComponent implements InitializingBean {

    @Resource
    private ImProperties imProperties;

    private final Map<Integer, ImMsgSerializer> imMsgSerializerMap = new HashMap<>();

    @Override
    public void afterPropertiesSet() {
        Map<String, ImMsgSerializer> matchingSerializeBeans = SpringUtil.getBeansOfType(ImMsgSerializer.class);
        if (!matchingSerializeBeans.isEmpty()) {
            matchingSerializeBeans.values().forEach(res -> imMsgSerializerMap.put(res.type().getType(), res));
        }
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
