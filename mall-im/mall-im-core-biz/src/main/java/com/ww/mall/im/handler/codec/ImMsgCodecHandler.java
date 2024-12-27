package com.ww.mall.im.handler.codec;

import com.ww.mall.im.common.ImConstant;
import com.ww.mall.im.common.ImMsg;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * @author ww
 * @create 2024-11-09 20:54
 * @description: 消息编解码处理器
 */
@Slf4j
@ChannelHandler.Sharable
public class ImMsgCodecHandler extends MessageToMessageCodec<ByteBuf, ImMsg> {

    @Override
    protected void encode(ChannelHandlerContext ctx, ImMsg msg, List<Object> out) throws Exception {
        ByteBuf byteBuf = ctx.alloc().buffer();
        // 写入4字节的魔数
        byteBuf.writeInt(ImConstant.DEFAULT_MAGIC);
        // 写入1字节的版本,
        byteBuf.writeByte(ImConstant.DEFAULT_VERSION);
        // 写入1字节的序列化方式
        byteBuf.writeByte(ImConstant.DEFAULT_SERIALIZER);
        // 写入4字节的消息类型
        byteBuf.writeInt(msg.getMsgType());
        // 写入2字节对齐填充【无意义】
        byteBuf.writeByte(0xff);
        byteBuf.writeByte(0xff);
        // 获取消息内容的字节数组
        // 7. 写入4字节消息长度
        byteBuf.writeInt(msg.getBody().length);
        // 8. 写入消息内容
        byteBuf.writeBytes(msg.getBody());
        out.add(byteBuf);
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf byteBuf, List<Object> out) throws Exception {
        // 读取4字节的魔数
        int magic = byteBuf.readInt();
        // 读取1字节的版本号
        byte version = byteBuf.readByte();
        // 读取1字节序列化方式
        byte serializerType = byteBuf.readByte();
        // 读取4字节的消息类型
        int msgType = byteBuf.readInt();
        // 读取2字节的填充字段
        byteBuf.readShort();
        // 读取4字节的消息长度
        int length = byteBuf.readInt();
        // 初始化一个byte数组存储消息
        byte[] data = new byte[length];
        byteBuf.readBytes(data, 0, length);
        ImMsg imMsg = new ImMsg();
        imMsg.setMagic(magic);
        imMsg.setVersion(version);
        imMsg.setSerializeType(serializerType);
        imMsg.setMsgType(msgType);
        imMsg.setLength(length);
        imMsg.setBody(data);
        out.add(imMsg);
    }
}
