package com.ww.mall.seckill.controller;

import com.ww.mall.seckill.service.DemoService;
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
    private DemoService demoService;

    @GetMapping("/order")
    public boolean redisStock() {
        return demoService.seckillOrder();
    }

    @GetMapping("/traceId")
    public void traceId() {
        demoService.traceId();
    }

    @GetMapping("/msg")
    public void msg() {
        demoService.msg();
    }

    @GetMapping("/cache")
    public void cache(String msg) {
        demoService.cache(msg);
    }

    @GetMapping("/boomFilter")
    public void boomFilter() {
        demoService.boomFilter();
    }

    @GetMapping("/liteFlow")
    public void liteFlow() {
        demoService.liteFlow();
    }

}
