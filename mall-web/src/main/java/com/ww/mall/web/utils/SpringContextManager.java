package com.ww.mall.web.utils;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * @author: ww
 * @create: 2023-07-22 13:38
 * @description: spring bean util
 */
@Component
public class SpringContextManager implements ApplicationContextAware {

    private static ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        SpringContextManager.applicationContext = applicationContext;
    }

    public static Object getBean(String name) {
        return applicationContext.getBean(name);
    }

    public static <T> T getBean(Class<T> clzName) throws BeansException {
        return applicationContext.getBean(clzName);
    }

    public static <T> T getBean(String name, Class<T> requiredType) {
        return applicationContext.getBean(name, requiredType);
    }

    public static boolean containsBean(String name) {
        return applicationContext.containsBean(name);
    }

    public static <T> boolean containsBean(Class<T> clazz) {
        Map<String, T> map =  applicationContext.getBeansOfType(clazz);
        return !map.isEmpty();
    }

}
