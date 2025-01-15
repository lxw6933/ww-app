package com.ww.app.im.common;

import cn.hutool.extra.spring.SpringUtil;
import com.ww.app.im.component.ImMsgSerializerComponent;
import com.ww.app.im.core.api.common.ImConstant;
import com.ww.app.im.core.api.common.ImMsgBody;
import lombok.Data;

import java.io.Serializable;

/**
 * @author ww
 * @create 2024-11-09 19:36
 * @description: 客户端发送给服务端的对象
 */
@Data
public class ImMsg implements Serializable {

    /**
     * 魔数
     */
    private int magic;

    /**
     * 消息版本号
     */
    private short version;

    /**
     * 消息序列化类型
     */
    private short serializeType;

    /**
     * 消息类型
     */
    private int msgType;

    /**
     * 消息长度
     */
    private int length;

    /**
     * 消息内容[byte]
     */
    private byte[] body;

    public static ImMsg buildTestClient(int msgType, short serializeType, byte[] bytes) {
        ImMsg imMsg = new ImMsg();
        imMsg.setMagic(ImConstant.DEFAULT_MAGIC);
        imMsg.setVersion(ImConstant.DEFAULT_VERSION);
        imMsg.setSerializeType(serializeType);
        imMsg.setMsgType(msgType);
        imMsg.setLength(bytes.length);
        imMsg.setBody(bytes);
        return imMsg;
    }

    public static ImMsg build(int msgType, ImMsgBody msgBody) {
        ImMsg imMsg = new ImMsg();
        imMsg.setMagic(ImConstant.DEFAULT_MAGIC);
        imMsg.setVersion(ImConstant.DEFAULT_VERSION);
        imMsg.setSerializeType(ImConstant.DEFAULT_SERIALIZER);
        imMsg.setMsgType(msgType);
        ImMsgSerializerComponent imMsgSerializerComponent = SpringUtil.getBean(ImMsgSerializerComponent.class);
        byte[] bytes = imMsgSerializerComponent.serializeMsg(ImConstant.DEFAULT_SERIALIZER, msgBody);
        imMsg.setLength(bytes.length);
        imMsg.setBody(bytes);
        return imMsg;
    }

    public boolean validData() {
        return this.body != null && this.body.length > 0;
    }
}
