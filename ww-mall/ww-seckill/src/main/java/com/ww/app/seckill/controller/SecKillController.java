package com.ww.app.seckill.controller;

import com.ww.app.seckill.service.SeckillService;
import com.ww.app.seckill.view.bo.SecKillOrderReqBO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;

/**
 * @author ww
 * @create 2024-05-14- 15:35
 * @description:
 */
@Slf4j
@RestController
@RequestMapping("/seckill")
public class SecKillController {

    @Resource
    private SeckillService seckillService;

    @GetMapping("/captcha")
    public void captcha(HttpServletResponse response, @RequestParam("activityCode") String activityCode, @RequestParam("skuId") Long skuId) {
        seckillService.captcha(response, activityCode, skuId);
    }

    @GetMapping("/path")
    public String getPath(@RequestParam("activityCode") String activityCode, @RequestParam("skuId") Long skuId) {
        return seckillService.getSecKillPath(activityCode, skuId);
    }

    @PostMapping("/{userSecKillPath}/order")
    public Boolean doSecKillOrder(@PathVariable String userSecKillPath, @Validated @RequestBody SecKillOrderReqBO secKillReqBO){
        return seckillService.doSecKillOrder(userSecKillPath, secKillReqBO);
    }

}
