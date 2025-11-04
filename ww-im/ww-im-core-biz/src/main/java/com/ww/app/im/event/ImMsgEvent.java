package com.ww.app.im.event;

import com.ww.app.im.common.ImMsg;
import io.netty.channel.ChannelHandlerContext;
import lombok.Data;

/**
 * IM消息事件
 * @author ww
 */
@Data
public class ImMsgEvent {
    /**
     * Netty Channel上下文
     */
    private ChannelHandlerContext ctx;
    
    /**
     * IM消息
     */
    private ImMsg imMsg;
    
    /**
     * 接收时间
     */
    private long receiveTime;
    
    /**
     * 获取处理延迟
     */
    public long getProcessDelay() {
        return System.currentTimeMillis() - receiveTime;
    }
}
