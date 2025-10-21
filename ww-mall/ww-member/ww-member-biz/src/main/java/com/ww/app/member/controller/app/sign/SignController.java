package com.ww.app.member.controller.app.sign;

import com.ww.app.common.context.AuthorizationContext;
import com.ww.app.member.service.sign.SignServiceImpl;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author ww
 * @create 2023-07-21- 09:15
 * @description: 签到
 */
@Tag(name = "用户 APP - 用户签到信息")
@Validated
@RestController
@RequestMapping("/member/sign")
public class SignController {

    @Resource
    private SignServiceImpl signService;

    /**
     * 当日签到
     *
     * @return 连续签到天数
     */
    @GetMapping("/in")
    public int doSign() {
        return signService.doSign(null, AuthorizationContext.getClientUser());
    }

    /**
     * 补签
     *
     * @param date 日期
     * @return 连续签到天数
     */
    @GetMapping("/back")
    public int doBackSign(@RequestParam("date") String date) {
        return signService.doSign(date, AuthorizationContext.getClientUser());
    }

    /**
     * 获取签到次数
     *
     * @param date 日期
     * @return 签到次数
     */
    @GetMapping("/count")
    public int signCount(@RequestParam("date") String date) {
        return signService.getSignCount(date, AuthorizationContext.getClientUser());
    }

    /**
     * 获取连续签到次数
     *
     * @param date 日期
     * @return 连续签到次数
     */
    @GetMapping("/continuousSignCount")
    public int continuousSignCount(@RequestParam("date") String date) {
        return signService.getContinuousSignCount(date, AuthorizationContext.getClientUser());
    }

    /**
     * 获取签到详情
     *
     * @return 签到详情
     */
    @GetMapping("/detail")
    public List<Boolean> signInfo() {
        return signService.getSignDetailInfo(AuthorizationContext.getClientUser());
    }

}
