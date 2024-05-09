package com.ww.mall.netty.handler.chat;

import cn.hutool.extra.spring.SpringUtil;
import com.alibaba.fastjson.JSON;
import com.ww.mall.netty.config.MallSerializerConfiguration;
import com.ww.mall.netty.holder.MessageTypeHolder;
import com.ww.mall.netty.message.chat.MallChatMessage;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author ww
 * @create 2024-05-06 22:19
 * @description: 必须和 MallProtocolFrameDecoder 一起使用，确保接到的 ByteBuf 消息是完整的
 */
@Slf4j
@Component
@ChannelHandler.Sharable
public class MessageCodecHandler extends MessageToMessageCodec<ByteBuf, MallChatMessage> {

    private final MallSerializerConfiguration serializeConfig = SpringUtil.getBean(MallSerializerConfiguration.class);

    @Override
    protected void encode(ChannelHandlerContext ctx, MallChatMessage msg, List<Object> outList) throws Exception {
        ByteBuf out = ctx.alloc().buffer();
        // 写入4字节的魔数
        out.writeBytes(new byte[]{6, 9, 3, 3});
        // 写入1字节的版本,
        out.writeByte(1);
        // 写入1字节的序列化方式
//        out.writeByte(serializeConfig.getDefaultSerializeType());
        out.writeByte(1);
        // 写入1字节的指令类型
        out.writeByte(msg.getMessageType());
        // 写入4个字节序列号
        out.writeInt(msg.getSequenceId());
        // 写入1字节对齐填充【无意义】
        out.writeByte(0xff);
        // 获取消息内容的字节数组
        byte[] bytes = serializeConfig.getSerializer().serialize(msg);
//        byte[] bytes = JSON.toJSONBytes(msg);
        // 7. 写入4字节消息长度
        out.writeInt(bytes.length);
        // 8. 写入消息内容
        out.writeBytes(bytes);
        outList.add(out);
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        // 读取4字节的魔数
        int magicNum = in.readInt();
        // 读取1字节的版本号
        byte version = in.readByte();
        // 读取1字节序列化方式
        byte serializerType = in.readByte();
        // 读取1字节的消息类型
        byte messageType = in.readByte();
        // 读取4字节的序列号
        int sequenceId = in.readInt();
        // 读取1字节的填充字段
        in.readByte();
        // 读取4字节的消息长度
        int length = in.readInt();
        // 初始化一个byte数组存储消息
        byte[] bytes = new byte[length];
        in.readBytes(bytes, 0, length);
        // 确定具体消息类型
        Class<? extends MallChatMessage> messageClass = MessageTypeHolder.getMessageClass(messageType);
        MallChatMessage mallChatMessage = serializeConfig.getSerializer().deserialize(messageClass, bytes);
//        MallChatMessage mallChatMessage = JSON.parseObject(bytes, messageClass);
        log.debug("魔数：{}, 版本：{}, 序列化类型：{}, 消息类型：{}, 序列化id：{}, 消息长度：{}", magicNum, version, serializerType, messageType, sequenceId, length);
        log.debug("消息：{}", mallChatMessage);
        out.add(mallChatMessage);
    }

}
