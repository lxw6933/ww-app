package com.ww.app.member.controller.app.sign;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.ww.app.common.context.AuthorizationContext;
import com.ww.app.member.service.sign.SignService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import java.util.Date;
import java.util.List;

/**
 * @author ww
 * @create 2023-07-21- 09:15
 * @description: 签到
 */
@Tag(name = "用户签到", description = "用户签到相关接口")
@Validated
@RestController
@RequestMapping("/member/sign")
public class SignController {

    @Resource
    private SignService signService;

    @Operation(summary = "当日签到", description = "执行当日签到操作")
    @GetMapping("/in")
    public int doSign() {
        return signService.doSign(null, AuthorizationContext.getClientUser());
    }

    @Operation(summary = "补签", description = "补签指定日期")
    @GetMapping("/back")
    public int doBackSign(@RequestParam("date")
                          @NotBlank(message = "补签日期不能为空")
                          @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}", message = "日期格式必须为 yyyy-MM-dd")
                          String date) {
        return signService.doSign(date, AuthorizationContext.getClientUser());
    }

    @Operation(summary = "获取签到次数", description = "获取指定月份的签到总次数")
    @GetMapping("/count")
    public int signCount(@RequestParam(name = "date", required = false)
                         @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}", message = "日期格式必须为 yyyy-MM-dd")
                         String date) {
        if (StrUtil.isBlank(date)) {
            date = DateUtil.formatDate(new Date());
        }
        return signService.getSignCount(date, AuthorizationContext.getClientUser());
    }

    @Operation(summary = "获取连续签到次数", description = "获取当前连续签到的天数")
    @GetMapping("/continuousSignCount")
    public int continuousSignCount(@RequestParam(name = "date", required = false)
                                   @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}", message = "日期格式必须为 yyyy-MM-dd")
                                   String date) {
        if (StrUtil.isBlank(date)) {
            date = DateUtil.formatDate(new Date());
        }
        return signService.getContinuousSignCount(date, AuthorizationContext.getClientUser());
    }

    @Operation(summary = "获取签到详情", description = "获取当前周期的签到详情列表")
    @GetMapping("/detail")
    public List<Boolean> signInfo() {
        return signService.getSignDetailInfo(AuthorizationContext.getClientUser());
    }

}
