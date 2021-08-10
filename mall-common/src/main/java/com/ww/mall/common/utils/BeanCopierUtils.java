package com.ww.mall.common.utils;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.cglib.beans.BeanCopier;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @description: cglib copy bean 工具类
 * @author: ww
 * @create: 2021-04-16 09:51
 */
public class BeanCopierUtils {

    private BeanCopierUtils() {}

    /** 用空间换时间，缓存beanCopier */
    private static final ConcurrentHashMap<String, BeanCopier> BEAN_COPIER_MAP = new ConcurrentHashMap<>();

    /**
     * 拷贝属性
     * @param source 来源
     * @param target 目标
     */
    public static void copyProperties(Object source, Object target) {
        String beanKey = generateKey(source.getClass(), target.getClass());
        BeanCopier copier;
        if (BEAN_COPIER_MAP.containsKey(beanKey)) {
            copier = BEAN_COPIER_MAP.get(beanKey);
        } else {
            copier = BeanCopier.create(source.getClass(), target.getClass(), false);
            BEAN_COPIER_MAP.putIfAbsent(beanKey, copier);
        }
        copier.copy(source, target, null);
    }

    /**
     * 复制list
     * @param list 源目标
     * @param clazz 目标class
     * @param <T> 泛型
     * @return List
     */
    public static <T> List<T> copyList(List<?> list, Class<T> clazz) {
        if (CollectionUtils.isEmpty(list)) {
            return new ArrayList<>();
        }
        List<T> resultList = new ArrayList<>();
        for (Object obj : list) {
            try {
                T source = clazz.newInstance();
                copyProperties(obj, source);
                resultList.add(source);
            } catch (InstantiationException | IllegalAccessException e) {
                return resultList;
            }
        }
        return resultList;
    }

    /**
     * 构建key
     * @param source 源
     * @param target 目标
     * @return String
     */
    private static String generateKey(Class<?> source, Class<?> target) {
        return source.getName() + "_" +  target.getName();
    }

}
