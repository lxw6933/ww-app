package com.ww.mall.common.utils;

import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

/**
 * @description: json工具类
 * @author: ww
 * @create: 2021-05-14 10:12
 */
@Slf4j
public class JsonUtils {

    private static final ObjectMapper JSON = new ObjectMapper();

    private static final String ERROR_MESSAGE = "parse obj json fail";

    private JsonUtils() {
    }

    static {
        JSON.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        // 忽略空Bean转json的错误
        JSON.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        JSON.configure(SerializationFeature.INDENT_OUTPUT, Boolean.TRUE);
        // 忽略转换没有的字段
        JSON.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /**
     * 对象转换json字符串
     *
     * @param obj 对象
     * @return String
     */
    public static String toJson(Object obj) {
        try {
//            return JSON.writeValueAsString(obj);   // 带格式化的json
            return JSONObject.toJSON(obj).toString();
        } catch (Exception e) {
            log.error(ERROR_MESSAGE, e);
            return null;
        }
    }

    /**
     * 字符串转换
     *
     * @param json  json字符串
     * @param clazz 类
     * @param <T>   泛型
     * @return T
     */
    public static <T> T parse(String json, Class<T> clazz) {
        try {
            return JSON.readValue(json, clazz);
        } catch (IOException e) {
            log.error(ERROR_MESSAGE, e);
            return null;
        }
    }

    /**
     * 字符串转换
     *
     * @param obj   对象
     * @param clazz 类
     * @param <T>   泛型
     * @return T
     */
    public static <T> T parse(Object obj, Class<T> clazz) {
        String json = toJson(obj);
        try {
            return JSON.readValue(json, clazz);
        } catch (IOException e) {
            log.error(ERROR_MESSAGE, e);
            return null;
        }
    }

    /**
     * json格式字符串转化Java对象
     *
     * @param jsonStr         json格式字符串
     * @param collectionClazz 集合 class对象
     * @param nodeClazz       集合泛型对象
     * @return T
     */
    public static <T> T parse(String jsonStr, Class<T> collectionClazz, Class<?>... nodeClazz) {
        JavaType javaType = JSON.getTypeFactory().constructParametricType(collectionClazz, nodeClazz);
        try {
            return JSON.readValue(jsonStr, javaType);
        } catch (IOException e) {
            log.error(ERROR_MESSAGE, e);
            return null;
        }
    }

    /**
     * json格式字符串转化Java对象
     *
     * @param obj             对象信息
     * @param collectionClazz 集合 class对象
     * @param nodeClazz       集合泛型对象
     * @return T
     */
    public static <T> T parse(Object obj, Class<T> collectionClazz, Class<?>... nodeClazz) {
        JavaType javaType = JSON.getTypeFactory().constructParametricType(collectionClazz, nodeClazz);
        String json = toJson(obj);
        try {
            return JSON.readValue(json, javaType);
        } catch (IOException e) {
            log.error(ERROR_MESSAGE, e);
            return null;
        }
    }

    /**
     * json格式字符串转化Java对象
     *
     * @param jsonStr json格式字符串
     * @param clazz   Java class
     * @return T
     */
    public static <T> T parseObject(String jsonStr, Class<T> clazz) throws IOException {
        if (jsonStr == null || jsonStr.trim().length() == 0) {
            return null;
        }
        return clazz.equals(String.class) ? (T) jsonStr : JSON.readValue(jsonStr, clazz);
    }

}
