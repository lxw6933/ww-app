package imclient.handler;

import cn.hutool.extra.spring.SpringUtil;
import com.alibaba.fastjson.JSON;
import com.ww.app.im.common.ImConstant;
import com.ww.app.im.common.ImMsg;
import com.ww.app.im.common.ImMsgBody;
import com.ww.app.im.dto.MessageDTO;
import com.ww.app.im.enums.ImMsgCodeEnum;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ClientHandler extends SimpleChannelInboundHandler<ImMsg> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ImMsg imMsg) {

        ImMsgBody respBody = JSON.parseObject(imMsg.getBody(), ImMsgBody.class);
        if (imMsg.getMsgType() == ImMsgCodeEnum.IM_BIZ_MSG.getCode()) {
            // 获取业务消息
            String bizMsg = respBody.getBizMsg();
            MessageDTO message = JSON.parseObject(bizMsg, MessageDTO.class);
            // 发送用户id
            Long userId = message.getUserId();
            // 发送内容
            String content = message.getContent();
            // 消息id
            String seqId = respBody.getSeqId();
            System.out.println("[" + userId + "][" + seqId + "]: " + content);
            ImMsgBody ackBody = new ImMsgBody();
            ackBody.setAppId(respBody.getAppId());
            ackBody.setUserId(userId);
            ackBody.setSeqId(seqId);
            ImMsg ackMsg = ImMsg.buildTestClient(ImMsgCodeEnum.IM_ACK_MSG.getCode(), ImConstant.DEFAULT_SERIALIZER, JSON.toJSONBytes(ackBody));
            ctx.writeAndFlush(ackMsg);
        }
        if (imMsg.getMsgType() == ImMsgCodeEnum.IM_ACK_MSG.getCode()) {
            // 消息id
            String seqId = respBody.getSeqId();
            System.out.println("[" + seqId + "]消息发送成功");
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        ImTestClientStart imClientStarter = SpringUtil.getBean(ImTestClientStart.class);
        imClientStarter.reConnection(ctx.channel().eventLoop(), null);
    }

}
