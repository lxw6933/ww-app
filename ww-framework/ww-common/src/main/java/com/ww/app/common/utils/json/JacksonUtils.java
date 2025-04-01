package com.ww.app.common.utils.json;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ww.app.common.exception.ApiException;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author ww
 * @create 2025-01-14- 18:30
 * @description: 基于Jackson实现JSON序列化和反序列化
 */
@Slf4j
public class JacksonUtils {

    /**
     * 默认日期格式
     */
    private static final String DEFAULT_DATE_FORMAT = DatePattern.NORM_DATETIME_PATTERN;

    /**
     * 默认数字格式
     */
    private static final String DEFAULT_NUMBER_FORMAT = "0.00";

    /**
     * 类型引用缓存
     */
    private static final ConcurrentHashMap<Class<?>, TypeReference<?>> TYPE_REFERENCE_CACHE = new ConcurrentHashMap<>();

    @Getter
    private static final ObjectMapper objectMapper = new ObjectMapper();

    static {
        configureObjectMapper();
    }

    private JacksonUtils() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * 配置ObjectMapper
     */
    private static void configureObjectMapper() {
        // 对象为空，也不会抛出异常，而是返回一个空的 JSON 对象 {}
        objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        // 忽略 JSON 中多余的属性，不会抛出异常
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        // 某个属性的值为 null，则该属性不会出现在生成的 JSON 字符串中
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        // 处理date字符串类型转换Date类型问题
        objectMapper.setDateFormat(new SimpleDateFormat(DEFAULT_DATE_FORMAT));
        // 处理BigDecimal精度问题
        SimpleModule simpleModule = new SimpleModule();
        simpleModule.addSerializer(BigDecimal.class, new BigDecimalSerializer());
        objectMapper.registerModule(simpleModule);
        // 解决 LocalDateTime 的序列化
        objectMapper.registerModules(new JavaTimeModule());
    }

    /**
     * BigDecimal序列化器
     */
    public static class BigDecimalSerializer extends JsonSerializer<BigDecimal> {
        private final DecimalFormat decimalFormat = new DecimalFormat(DEFAULT_NUMBER_FORMAT);

        @Override
        public void serialize(BigDecimal value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            if (value != null) {
                gen.writeString(decimalFormat.format(value));
            } else {
                gen.writeNull();
            }
        }
    }

    /**
     * 将对象转换为JSON字符串
     *
     * @param object 待转换对象
     * @return JSON字符串
     */
    @SneakyThrows
    public static String toJsonString(Object object) {
        if (object == null) {
            return null;
        }
        return objectMapper.writeValueAsString(object);
    }

    /**
     * 将对象转换为JSON字节数组
     *
     * @param object 待转换对象
     * @return JSON字节数组
     */
    @SneakyThrows
    public static byte[] toJsonByte(Object object) {
        if (object == null) {
            return null;
        }
        return objectMapper.writeValueAsBytes(object);
    }

    /**
     * 将JSON字符串转换为指定类型的对象
     *
     * @param text  JSON字符串
     * @param clazz 目标类型
     * @return 转换后的对象
     */
    public static <T> T parseObject(String text, Class<T> clazz) {
        if (StrUtil.isEmpty(text) || clazz == null) {
            return null;
        }
        try {
            return objectMapper.readValue(text, clazz);
        } catch (IOException e) {
            log.error("JSON解析错误, json: {}", text, e);
            throw new ApiException("JSON解析错误: " + e.getMessage());
        }
    }

    /**
     * 将JSON字节数组转换为指定类型的对象
     *
     * @param bytes JSON字节数组
     * @param clazz 目标类型
     * @return 转换后的对象
     */
    public static <T> T parseObject(byte[] bytes, Class<T> clazz) {
        if (ArrayUtil.isEmpty(bytes) || clazz == null) {
            return null;
        }
        try {
            return objectMapper.readValue(bytes, clazz);
        } catch (IOException e) {
            log.error("JSON解析错误, bytes: {}", bytes, e);
            throw new ApiException("JSON解析错误: " + e.getMessage());
        }
    }

    /**
     * 将JSON字符串转换为指定类型的对象
     *
     * @param text         JSON字符串
     * @param typeReference 类型引用
     * @return 转换后的对象
     */
    public static <T> T parseObject(String text, TypeReference<T> typeReference) {
        if (StrUtil.isEmpty(text) || typeReference == null) {
            return null;
        }
        try {
            return objectMapper.readValue(text, typeReference);
        } catch (IOException e) {
            log.error("JSON解析错误, json: {}", text, e);
            throw new ApiException("JSON解析错误: " + e.getMessage());
        }
    }

    /**
     * 获取类型引用
     *
     * @param clazz 目标类型
     * @return 类型引用
     */
    @SuppressWarnings("unchecked")
    public static <T> TypeReference<T> getTypeReference(Class<T> clazz) {
        return (TypeReference<T>) TYPE_REFERENCE_CACHE.computeIfAbsent(clazz, 
            k -> new TypeReference<T>() {});
    }

    /**
     * 判断字符串是否为有效的JSON
     *
     * @param text 待判断字符串
     * @return 是否为有效的JSON
     */
    public static boolean isValidJson(String text) {
        if (StrUtil.isEmpty(text)) {
            return false;
        }
        try {
            objectMapper.readTree(text);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * 格式化JSON字符串
     *
     * @param text JSON字符串
     * @return 格式化后的JSON字符串
     */
    public static String formatJson(String text) {
        if (!isValidJson(text)) {
            return text;
        }
        try {
            Object json = objectMapper.readValue(text, Object.class);
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);
        } catch (IOException e) {
            log.error("JSON格式化错误, json: {}", text, e);
            return text;
        }
    }
}
