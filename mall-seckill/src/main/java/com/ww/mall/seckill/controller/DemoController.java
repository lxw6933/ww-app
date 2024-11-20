package com.ww.mall.seckill.controller;

import com.github.houbb.sensitive.word.bs.SensitiveWordBs;
import com.ww.mall.common.common.Result;
import com.ww.mall.common.exception.ApiException;
import com.ww.mall.member.member.MemberApi;
import com.ww.mall.member.member.bo.MemberLoginBO;
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
import java.util.List;

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

    @GetMapping("/testBeanCopy")
    public void testBeanCopy(@RequestParam int type) {
        demoService.testBeanCopy(type);
    }

    @GetMapping("/generatorCode")
    public int generatorCode(@RequestParam String batchNo, @RequestParam int length, @RequestParam int total) {
        return demoService.generatorCode(batchNo, length, total);
    }

    @GetMapping("/issueCode")
    public List<String> issueCode(@RequestParam String outOrderCode, @RequestParam int quantity) {
        return demoService.issueCode(outOrderCode, quantity);
    }

    @PostMapping("testEncryptReqData")
    public void testEncryptReqData(@RequestBody MemberLoginBO memberLoginBO) {
        // 请求参数1【text】：encryptStr
        // 请求参数2【json】：encryptStr:""
        demoService.testEncryptReqData(memberLoginBO);
    }

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
    public String helloGrpc() {
        return grpcClientService.sendMessage("mall-seckill");
    }

    @Resource
    private MemberApi memberApi;

    @GetMapping("/openFeign/hello")
    public String helloOpenFeign() {
        Result<String> testResult = memberApi.test();
        if (testResult.isSuccess()) {
            return memberApi.test().getData();
        }
        throw new ApiException(testResult.getCode(), testResult.getMsg());
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
