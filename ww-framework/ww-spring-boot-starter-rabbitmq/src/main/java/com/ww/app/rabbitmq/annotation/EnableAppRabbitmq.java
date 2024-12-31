package com.ww.app.rabbitmq.annotation;

import com.ww.app.rabbitmq.config.RabbitmqAutoConfiguration;
import com.ww.app.rabbitmq.bind.BindingConfiguration;
import com.ww.app.rabbitmq.exchange.ExchangeConfiguration;
import com.ww.app.rabbitmq.queue.QueueConfiguration;
import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @description:
 * @author: ww
 * @create: 2023/7/15 22:11
 **/
@Deprecated
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Import({RabbitmqAutoConfiguration.class,
    QueueConfiguration.class,
    ExchangeConfiguration.class,
    BindingConfiguration.class})
public @interface EnableAppRabbitmq {
}
