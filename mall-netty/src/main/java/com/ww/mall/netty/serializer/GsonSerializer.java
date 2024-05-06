package com.ww.mall.netty.serializer;

import com.google.gson.*;
import com.google.gson.JsonSerializer;
import com.ww.mall.netty.enums.SerializerTypeEnum;
import org.springframework.stereotype.Component;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;

/**
 * @author ww
 * @create 2024-05-06 22:40
 * @description:
 */
@Component
public class GsonSerializer implements MallSerializer {
    @Override
    public SerializerTypeEnum type() {
        return SerializerTypeEnum.GSON;
    }

    @Override
    public <T> byte[] serialize(T object) {
        Gson gson = new GsonBuilder().registerTypeAdapter(Class.class, new ClassCodec()).create();
        String json = gson.toJson(object);
        return json.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public <T> T deserialize(Class<T> clazz, byte[] bytes) {
        Gson gson = new GsonBuilder().registerTypeAdapter(Class.class, new ClassCodec()).create();
        String json = new String(bytes, StandardCharsets.UTF_8);
        return gson.fromJson(json, clazz);
    }

    /**
     * 处理gson转class类型异常
     */
    static class ClassCodec implements JsonSerializer<Class<?>>, JsonDeserializer<Class<?>> {

        @Override
        public Class<?> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            try {
                String str = json.getAsString();
                return Class.forName(str);
            } catch (ClassNotFoundException e) {
                throw new JsonParseException(e);
            }
        }

        @Override
        public JsonElement serialize(Class<?> src, Type typeOfSrc, JsonSerializationContext context) {
            // class -> json
            return new JsonPrimitive(src.getName());
        }
    }
}
