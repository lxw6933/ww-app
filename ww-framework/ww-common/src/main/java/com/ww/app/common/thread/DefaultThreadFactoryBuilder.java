package com.ww.app.common.thread;

import org.apache.commons.lang3.StringUtils;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @description: 线程工程构建
 * @author: ww
 * @create: 2023/7/16 19:23
 **/
public class DefaultThreadFactoryBuilder {

    /**
     * 线程名称前缀
     */
    private String namePrefix;

    /**
     * 是否为守护线程
     */
    private boolean daemon = false;

    /**
     * 线程优先级，值越大优先级越高，默认为5
     */
    private int priority = Thread.NORM_PRIORITY;


    public DefaultThreadFactoryBuilder setNamePrefix(String namePrefix) {
        if (StringUtils.isBlank(namePrefix)) {
            throw new NullPointerException("The namePrefix can not be null");
        }
        this.namePrefix = namePrefix;
        return this;
    }

    public DefaultThreadFactoryBuilder setDaemon(boolean daemon) {
        this.daemon = daemon;
        return this;
    }

    public DefaultThreadFactoryBuilder setPriority(int priority) {
        if (priority < Thread.MIN_PRIORITY){
            throw new IllegalArgumentException(String.format(
                    "Thread priority (%s) must be >= %s", priority, Thread.MIN_PRIORITY));
        }
        if (priority > Thread.MAX_PRIORITY) {
            throw new IllegalArgumentException(String.format(
                    "Thread priority (%s) must be <= %s", priority, Thread.MAX_PRIORITY));
        }
        this.priority = priority;
        return this;
    }

    /**
     * 构建ThreadFactory
     * @return ThreadFactory
     */
    public ThreadFactory build() {
        return build(this);
    }

    /**
     * 根据ThreadFactoryBuilder的属性值来定义构建新线程的方式
     * @param builder ThreadFactoryBuilder
     * @return TreadFactory
     */
    private static ThreadFactory build(DefaultThreadFactoryBuilder builder) {
        final String namePrefix = builder.namePrefix;
        final boolean daemon = builder.daemon;
        final int priority = builder.priority;
        final AtomicLong count = new AtomicLong(0);
        return r -> {
            Thread thread = new Thread(r);
            thread.setName(namePrefix + "-" + count.getAndIncrement());
            thread.setPriority(priority);
            thread.setDaemon(daemon);
            return thread;
        };
    }

}
