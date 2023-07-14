package com.ww.mall.config.excel.listener;

import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ww.mall.common.exception.ValidatorException;
import com.ww.mall.config.rabbitmq.publisher.UserPublisher;
import com.ww.mall.mvc.base.BaseEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import sun.reflect.generics.reflectiveObjects.ParameterizedTypeImpl;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * @description: 有个很重要的点 DemoDataListener 不能被spring管理，要每次读取excel都要new,然后里面用到spring可以构造方法传进去
 * @author: ww
 * @create: 2021-05-14 17:32
 */
@Slf4j
public class UploadDataModelMqListener<T, E extends BaseEntity> extends AnalysisEventListener<T> {

    /**
     * 每隔100条存储数据库，实际使用中可以3000条，然后清理list ，方便内存回收
     */
    private static final int BATCH_COUNT = 1000;
    List<T> list = new ArrayList<>();

    private final IService<E> service;

    private final RabbitTemplate rabbitTemplate;

    private final MessagePostProcessor messagePostProcessor;

    private final UserPublisher userPublisher;

    /**
     * 如果使用了spring,请使用这个构造方法。每次创建Listener的时候需要把spring管理的类传进来
     *
     * @param rabbitTemplate rabbitTemplate
     */
    public UploadDataModelMqListener(IService<E> service,
                                     RabbitTemplate rabbitTemplate,
                                     MessagePostProcessor messagePostProcessor,
                                     UserPublisher userPublisher) {
        this.service = service;
        this.rabbitTemplate = rabbitTemplate;
        this.messagePostProcessor = messagePostProcessor;
        this.userPublisher = userPublisher;
    }

    /**
     * 这个每一条数据解析都会来调用
     *
     * @param data    userDataModel
     * @param context context
     */
    @Override
    public void invoke(T data, AnalysisContext context) {
        log.info("解析到一条数据:{}", JSON.toJSONString(data));
        list.add(data);
        // 达到BATCH_COUNT了，需要去存储一次数据库，防止数据几万条数据在内存，容易OOM
        if (list.size() >= BATCH_COUNT) {
            saveData();
            // 存储完成清理 list
            list.clear();
        }
    }

    /**
     * 所有数据解析完成了 都会来调用
     *
     * @param context context
     */
    @Override
    public void doAfterAllAnalysed(AnalysisContext context) {
        // 这里也要保存数据，确保最后遗留的数据也存储到数据库
        saveData();
        log.info("所有数据解析完成！");
    }

    public Class<E> getRealType() {
        try {
            Type type = ((ParameterizedTypeImpl) ((Class<?>) service.getClass().getGenericSuperclass()).getGenericSuperclass()).getActualTypeArguments()[1];
            return (Class<E>) Class.forName(type.getTypeName());
        } catch (Exception e) {
            throw new ValidatorException("类型获取异常，读取Excel失败");
        }
    }

    /**
     * 加上存储数据库或者MQ
     */
    private void saveData() {
        log.info("{}条数据，开始存储数据库！", list.size());
        // 入MQ
        for (T res : list) {
            try {
                E obj = getRealType().newInstance();
                log.info(obj.toString());
                BeanCopierUtils.copyProperties(res, obj);
                userPublisher.publishDemoMsg(obj);
            } catch (Exception e){
                log.error("存储MQ失败！" + e.getMessage());
            }
        }
    }

}
