package com.ww.mall.member.controller;

import com.ww.mall.member.service.SignService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * @author ww
 * @create 2023-07-21- 09:15
 * @description: 签到
 */
@Validated
@RestController
@RequestMapping("/sing")
public class SignController {

    @Autowired
    private SignService signService;

    @GetMapping("/in")
    public int doSign() {
        return signService.doSign(null);
    }

    @GetMapping("/back")
    public int doSign(@RequestParam("date") String date) {
        return signService.doSign(date);
    }

    @GetMapping("/count")
    public int signCount(@RequestParam("date") String date) {
        return signService.getSignCount(date);
    }

    @GetMapping("/continuousSignCount")
    public int continuousSignCount(@RequestParam("date") String date) {
        return signService.getContinuousSignCount(date);
    }

    @GetMapping("/detail")
    public Map<String, Boolean> signInfo(@RequestParam("date") String date) {
        return signService.getSignInfo(date);
    }

}
