package com.ww.mall.redis.codec;

import org.redisson.client.codec.BaseCodec;
import org.redisson.client.codec.StringCodec;
import org.redisson.client.protocol.Decoder;
import org.redisson.client.protocol.Encoder;
import org.redisson.codec.JsonJacksonCodec;

/**
 * @author ww
 * @create 2024-08-27 22:42
 * @description:
 */
public class RedissonCodec extends BaseCodec {

    private final StringCodec keyCodec = new StringCodec();
    private final JsonJacksonCodec valueCodec = new JsonJacksonCodec();

    @Override
    public Decoder<Object> getMapKeyDecoder() {
        return keyCodec.getValueDecoder();
    }

    @Override
    public Encoder getMapKeyEncoder() {
        return keyCodec.getValueEncoder();
    }

    @Override
    public Decoder<Object> getMapValueDecoder() {
        return valueCodec.getValueDecoder();
    }

    @Override
    public Encoder getMapValueEncoder() {
        return valueCodec.getValueEncoder();
    }

    @Override
    public Decoder<Object> getValueDecoder() {
        return valueCodec.getValueDecoder();
    }

    @Override
    public Encoder getValueEncoder() {
        return valueCodec.getValueEncoder();
    }

}
