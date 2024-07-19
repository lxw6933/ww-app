package com.ww.mall.seckill.controller;

import com.github.houbb.sensitive.word.bs.SensitiveWordBs;
import com.ww.mall.seckill.grpc.GrpcClientService;
import com.ww.mall.seckill.service.DemoService;
import com.ww.mall.seckill.view.bo.SensitiveWordBO;
import com.ww.mall.seckill.view.bo.UserInfoVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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

    @GetMapping("testInsertMongo")
    public void testInsertMongo() {
        demoService.testInsertMongo();
    }

    @GetMapping("/testLua")
    public boolean testLuaScript(Integer type) {
        return demoService.testLuaScript(type);
    }

    @GetMapping("/order")
    public boolean redisStock() {
        return demoService.secKillOrder();
    }

    @GetMapping("/hashStock")
    public boolean redisHashStock(Integer type) {return demoService.secKillHashStock(type);}

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
    public void boomFilter(@RequestParam Integer type, @RequestParam(required = false) Long ele) {
        demoService.boomFilter(type, ele);
    }

    @GetMapping("/liteFlow")
    public void liteFlow() {
        demoService.liteFlow();
    }

    @GetMapping("/sensitiveData")
    public UserInfoVO sensitiveData() {
        UserInfoVO vo = new UserInfoVO();
        vo.setUsername("王二狗");
        vo.setPassword("123456789");
        vo.setEmail("ww123456789@gamil.com");
        vo.setMobile("1562358569856");
        vo.setIdCard("360782166908157114");
        return vo;
    }

    @Resource
    private GrpcClientService grpcClientService;

    @GetMapping("/grpc/hello")
    public String hello() {
        return grpcClientService.sendMessage("mall-seckill");
    }

    @GetMapping("/sensitiveWord")
    public String sensitiveWord(SensitiveWordBO content) {
        return demoService.sensitiveWord(content);
    }

    @Resource
    private SensitiveWordBs sensitiveWordBs;

    @GetMapping("/refreshSensitiveData")
    public void refreshSensitiveData() {
        sensitiveWordBs.init();
    }

    @PostMapping("import")
    public void importData(@RequestParam("file") MultipartFile file) {
        demoService.importData(file);
    }

    @PostMapping("/export")
    public void exportData(HttpServletResponse response) {
        demoService.exportDate(response);
    }

    @GetMapping("/ip")
    public String ip2region(HttpServletRequest request) {
        return demoService.ip2region(request);
    }

}
