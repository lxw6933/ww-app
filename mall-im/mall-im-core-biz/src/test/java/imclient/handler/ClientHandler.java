package imclient.handler;

import cn.hutool.extra.spring.SpringUtil;
import com.alibaba.fastjson.JSON;
import com.ww.mall.im.common.ImMsg;
import com.ww.mall.im.common.ImMsgBody;
import com.ww.mall.im.dto.MessageDTO;
import com.ww.mall.im.enums.ImMsgCodeEnum;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ClientHandler extends SimpleChannelInboundHandler<ImMsg> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ImMsg imMsg) {
        ImMsgBody respBody = JSON.parseObject(new String(imMsg.getBody()), ImMsgBody.class);
        if (imMsg.getMsgType() == ImMsgCodeEnum.IM_BIZ_MSG.getCode()) {
            ImMsgBody ackBody = new ImMsgBody();
            ackBody.setSeqId(respBody.getSeqId());
            ackBody.setAppId(respBody.getAppId());
            ackBody.setUserId(respBody.getUserId());
            ImMsg ackMsg = ImMsg.build(ImMsgCodeEnum.IM_ACK_MSG.getCode(), JSON.toJSONString(ackBody));
            ctx.writeAndFlush(ackMsg);
        }
        if (imMsg.getMsgType() == ImMsgCodeEnum.IM_BIZ_MSG.getCode()) {
            String bizMsg = respBody.getBizMsg();
            MessageDTO message = JSON.parseObject(bizMsg, MessageDTO.class);
            System.out.println("[" + message.getUserId() + "]: " + message.getContent());
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        ImTestClientStart imClientStarter = SpringUtil.getBean(ImTestClientStart.class);
        imClientStarter.reConnection(ctx.channel().eventLoop(), null);
    }

}
