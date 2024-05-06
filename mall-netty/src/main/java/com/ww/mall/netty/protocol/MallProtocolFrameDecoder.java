package com.ww.mall.netty.protocol;

import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import org.springframework.stereotype.Component;

/**
 * @author ww
 * @create 2024-05-06 21:35
 * @description: 自定义消息协议
 */
public class MallProtocolFrameDecoder extends LengthFieldBasedFrameDecoder {

    public MallProtocolFrameDecoder() {
        this(1024, 12, 4, 0, 0);
    }

    public MallProtocolFrameDecoder(int maxFrameLength, int lengthFieldOffset, int lengthFieldLength, int lengthAdjustment, int initialBytesToStrip) {
        super(maxFrameLength, lengthFieldOffset, lengthFieldLength, lengthAdjustment, initialBytesToStrip);
    }

}
