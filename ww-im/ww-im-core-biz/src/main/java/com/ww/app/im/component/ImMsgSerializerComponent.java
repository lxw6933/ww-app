package com.ww.app.im.component;

import cn.hutool.core.lang.Assert;
import cn.hutool.extra.spring.SpringUtil;
import com.ww.app.im.common.ImMsg;
import com.ww.app.im.common.ImMsgBody;
import com.ww.app.im.serializer.ImMsgSerializer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

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

    private final Map<Integer, ImMsgSerializer> imMsgSerializerMap = new HashMap<>();

    @Override
    public void afterPropertiesSet() {
        Map<String, ImMsgSerializer> matchingSerializeBeans = SpringUtil.getBeansOfType(ImMsgSerializer.class);
        if (!matchingSerializeBeans.isEmpty()) {
            matchingSerializeBeans.values().forEach(res -> imMsgSerializerMap.put(res.type().getType(), res));
        }
    }

    public ImMsgBody deserializeMsg(ImMsg imMsg) {
        ImMsgSerializer imMsgSerializer = getSerializer(imMsg.getSerializeType());
        return imMsgSerializer.deserialize(ImMsgBody.class, imMsg.getBody());
    }

    public byte[] serializeMsg(short serializerType, ImMsgBody imMsgBody) {
        ImMsgSerializer imMsgSerializer = getSerializer(serializerType);
        return imMsgSerializer.serialize(imMsgBody);
    }

    public ImMsgSerializer getSerializer(short serializerType) {
        ImMsgSerializer imMsgSerializer = this.imMsgSerializerMap.get((int) serializerType);
        Assert.notNull(imMsgSerializer, () -> new IllegalArgumentException("不支持消息序列化方式"));
        return imMsgSerializer;
    }
}
