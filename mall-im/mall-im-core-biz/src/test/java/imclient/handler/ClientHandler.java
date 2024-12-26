package imclient.handler;

import com.alibaba.fastjson.JSON;
import com.ww.mall.im.common.ImMsg;
import com.ww.mall.im.common.ImMsgBody;
import com.ww.mall.im.enums.ImMsgCodeEnum;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class ClientHandler extends SimpleChannelInboundHandler<ImMsg> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ImMsg imMsg) {
        if (imMsg.getMsgType() == ImMsgCodeEnum.IM_BIZ_MSG.getCode()) {
            ImMsgBody respBody = JSON.parseObject(new String(imMsg.getBody()), ImMsgBody.class);
            ImMsgBody ackBody = new ImMsgBody();
            ackBody.setSeqId(respBody.getSeqId());
            ackBody.setAppId(respBody.getAppId());
            ackBody.setUserId(respBody.getUserId());
            ImMsg ackMsg = ImMsg.build(ImMsgCodeEnum.IM_ACK_MSG.getCode(), JSON.toJSONString(ackBody));
            ctx.writeAndFlush(ackMsg);
        }
        System.out.println("【服务端响应数据】result is " + new String(imMsg.getBody()));
    }

}
