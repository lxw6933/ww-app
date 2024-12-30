package com.ww.app.im.protocol;

import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

/**
 * @author ww
 * @create 2024-11-10 16:43
 * @description:
 */
public class ImProtocolFrameDecoder extends LengthFieldBasedFrameDecoder {

    public ImProtocolFrameDecoder() {
        this(1024, 12, 4, 0, 0);
    }

    public ImProtocolFrameDecoder(int maxFrameLength, int lengthFieldOffset, int lengthFieldLength, int lengthAdjustment, int initialBytesToStrip) {
        super(maxFrameLength, lengthFieldOffset, lengthFieldLength, lengthAdjustment, initialBytesToStrip);
    }

}
