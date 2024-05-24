package com.ww.mall.seckill.service.impl;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import com.alibaba.fastjson.JSON;
import com.github.houbb.sensitive.word.bs.SensitiveWordBs;
import com.ww.mall.common.constant.RedisChannelConstant;
import com.ww.mall.common.enums.SensitiveWordHandlerType;
import com.ww.mall.common.utils.IdUtil;
import com.ww.mall.rabbitmq.MallPublisher;
import com.ww.mall.rabbitmq.exchange.ExchangeConstant;
import com.ww.mall.rabbitmq.queue.QueueConstant;
import com.ww.mall.rabbitmq.routekey.RouteKeyConstant;
import com.ww.mall.redis.MallRedisTemplate;
import com.ww.mall.seckill.entity.SecKillOrder;
import com.ww.mall.seckill.manager.MallCacheManager;
import com.ww.mall.seckill.node.executor.DemoFlowExecutor;
import com.ww.mall.seckill.service.DemoService;
import com.ww.mall.seckill.view.bo.SensitiveWordBO;
import com.ww.mall.seckill.view.bo.UserInfoVO;
import com.ww.mall.sensitive.annotation.MallSensitiveWordHandler;
import com.ww.mall.web.feign.ThirdServerFeignService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author ww
 * @create 2024-05-14 23:24
 * @description:
 */
@Slf4j
@Service
public class DemoServiceImpl implements DemoService {

    @Autowired
    private MallRedisTemplate mallRedisTemplate;

    @Autowired
    private ThirdServerFeignService thirdServerFeignService;

    @Autowired
    private ThreadPoolExecutor defaultThreadPoolExecutor;

    @Autowired
    private MallPublisher mallPublisher;

    @Autowired
    private MongoTemplate mongoTemplate;

    private final AtomicInteger num = new AtomicInteger(0);

    @Override
    public boolean seckillOrder() {
        if (mallRedisTemplate.decrementStock("skuStock", 1) >= 0) {
            String orderDate = DateUtil.format(new Date(), DatePattern.NORM_DATETIME_PATTERN);
            String orderNo = IdUtil.generatorIdStr();
            int totalOrderNum = num.incrementAndGet();
            mallPublisher.publishMsg(ExchangeConstant.MALL_OMS_EXCHANGE, RouteKeyConstant.MALL_CREATE_ORDER_KEY, orderNo);
            log.info("下单总数【{}】订单【{}】下单成功【{}】", totalOrderNum, orderNo, orderDate);
        }
        return true;
    }

    @Override
    public void traceId() {
        // interface 日志
        log.info("interface start log");
        // thread pool日志
        for (int i = 0; i < 3; i++) {
            defaultThreadPoolExecutor.submit(() -> log.info("thread pool log"));
        }
        // mq日志
        mallPublisher.publishMsg(ExchangeConstant.MALL_MEMBER_EXCHANGE, RouteKeyConstant.MALL_MEMBER_REGISTER_KEY, 1);
        // feign日志
        thirdServerFeignService.sendSms("15970191157", "9527");
        log.info("interface end log");
    }

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Override
    public void msg() {
        log.info("seckill msg");
        rabbitTemplate.convertAndSend(QueueConstant.MALL_TEST_QUEUE, "1");
    }

    @Override
    public void cache(String msg) {
        mallRedisTemplate.publishMessage(RedisChannelConstant.MALL_SPU_CHANNEL, msg);
    }

    @Override
    public void boomFilter() {
        SecKillOrder secKillOrder = new SecKillOrder();
        secKillOrder.setUserId(0L);
        secKillOrder.setOrderType(0);
        secKillOrder.setOrderNo(IdUtil.generatorIdStr());
        secKillOrder.setCreateTime(DateUtil.format(new Date(), DatePattern.NORM_DATETIME_PATTERN));
        mongoTemplate.save(secKillOrder);
        MallCacheManager.spuCache.get("1", res -> null);
        log.info("执行完毕filter数量");
    }

    @Autowired
    private DemoFlowExecutor demoFlowExecutor;

    @Override
    public void liteFlow() {
        demoFlowExecutor.testConfig();
    }

    @Resource
    private SensitiveWordBs sensitiveWordBs;

    @Override
    @MallSensitiveWordHandler(content = {"#content.word", "#content.content"}, handlerType = SensitiveWordHandlerType.REPLACE)
    public String sensitiveWord(SensitiveWordBO content) {
        return JSON.toJSONString(content);
    }

}
