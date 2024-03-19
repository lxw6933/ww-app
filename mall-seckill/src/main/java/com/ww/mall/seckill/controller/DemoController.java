package com.ww.mall.seckill.controller;

import com.ww.mall.rabbitmq.exchange.ExchangeConstant;
import com.ww.mall.rabbitmq.routekey.RouteKeyConstant;
import com.ww.mall.seckill.service.SeckillService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author ww
 * @create 2024-02-06- 14:50
 * @description:
 */
@Slf4j
@RestController
@RequestMapping("/seckill")
public class DemoController {

    @Autowired
    private SeckillService seckillService;

    @GetMapping("/order")
    public boolean redisStock() {
        return seckillService.seckillOrder();
    }

    @GetMapping("/traceId")
    public void traceId() {
        seckillService.traceId();
    }

}
