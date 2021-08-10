package com.ww.mall.mvc.manager;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * @description: spring bean util
 * @author: ww
 * @create: 2021-05-18 13:38
 */
@Component
public class SpringContextManager implements ApplicationContextAware {

    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    public Object getBean(String name) {
        return this.applicationContext.getBean(name);
    }

    public <T> T getBean(Class<T> clzName) throws BeansException {
        return this.applicationContext.getBean(clzName);
    }

    public <T> T getBean(String name, Class<T> requiredType) {
        return this.applicationContext.getBean(name, requiredType);
    }

    public boolean containsBean(String name) {
        return this.applicationContext.containsBean(name);
    }

    public <T> boolean containsBean(Class<T> clazz) {
        Map<String, T> map =  this.applicationContext.getBeansOfType(clazz);
        return !map.isEmpty();
    }

    public boolean isSingleton(String name) {
        return this.applicationContext.isSingleton(name);
    }

    public Class<?> getType(String name) {
        return this.applicationContext.getType(name);
    }

}
