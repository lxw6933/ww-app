package com.ww.app.redis.codec;

import com.ww.app.common.utils.json.JacksonUtils;
import lombok.Getter;
import org.redisson.client.codec.BaseCodec;
import org.redisson.client.codec.StringCodec;
import org.redisson.client.protocol.Decoder;
import org.redisson.client.protocol.Encoder;
import org.redisson.codec.JsonJacksonCodec;
import org.redisson.codec.MsgPackJacksonCodec;

/**
 * @author ww
 * @create 2024-08-27 22:42
 * @description:
 */
public class RedissonJsonCodec extends BaseCodec {

    private final StringCodec keyCodec = new StringCodec();
    private final JsonJacksonCodec valueCodec;

    public RedissonJsonCodec() {
        this(CodecType.JACKSON);
    }

    public RedissonJsonCodec(CodecType type) {
        switch (type) {
            case MSGPACK:
                valueCodec = new MsgPackJacksonCodec();
                JacksonUtils.configureObjectMapper(valueCodec.getObjectMapper());
                break;
            case JACKSON:
            default:
                valueCodec = new JsonJacksonCodec(JacksonUtils.getObjectMapper());
        }
    }

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

    @Getter
    public enum CodecType {

        /**
         * Jackson JSON 序列化（默认）
         */
        JACKSON("jackson", "Jackson JSON 序列化"),

        /**
         * MessagePack 二进制序列化
         */
        MSGPACK("msgpack", "MessagePack 二进制序列化");

        /**
         * 类型标识
         */
        private final String type;

        /**
         * 类型描述
         */
        private final String description;

        CodecType(String type, String description) {
            this.type = type;
            this.description = description;
        }

        /**
         * 根据类型字符串获取枚举
         *
         * @param type 类型字符串
         * @return CodecType 枚举，未找到返回默认值 JACKSON
         */
        public static CodecType fromType(String type) {
            if (type == null || type.trim().isEmpty()) {
                return JACKSON;
            }

            String normalizedType = type.trim().toLowerCase();

            for (CodecType codecType : values()) {
                if (codecType.type.equals(normalizedType)) {
                    return codecType;
                }
            }

            // 兼容其他别名
            if ("json".equals(normalizedType)) {
                return JACKSON;
            }
            if ("messagepack".equals(normalizedType)) {
                return MSGPACK;
            }

            return JACKSON;
        }
    }

}
