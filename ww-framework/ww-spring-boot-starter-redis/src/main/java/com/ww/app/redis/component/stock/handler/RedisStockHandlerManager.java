package com.ww.app.redis.component.stock.handler;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author ww
 * @create 2024-06-27- 10:09
 * @description:
 */
@Component
public class RedisStockHandlerManager implements ApplicationContextAware {

    private ApplicationContext applicationContext;

    private final List<IRedisStockHandler> handlerList = new ArrayList<>();

    @PostConstruct
    public void init() {
        Collection<IRedisStockHandler> handlers = this.applicationContext.getBeansOfType(IRedisStockHandler.class).values();
        handlers.forEach(this::addHandler);
    }

    public void addHandler(IRedisStockHandler handler) {
        handlerList.add(handler);
    }

    public void handleFailRollbackStock(String hashKey, int number, int type) {
        for (IRedisStockHandler handler : handlerList) {
            handler.handleFailRollbackStock(hashKey, number, type);
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
