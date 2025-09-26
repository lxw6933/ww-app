package com.ww.app.member.controller.app.integral;

import com.ww.app.common.common.AppPage;
import com.ww.app.common.common.AppPageResult;
import com.ww.app.member.entity.mongo.MemberIntegralRecord;
import com.ww.app.member.service.integral.MemberIntegralRecordService;
import com.ww.app.member.view.bo.MemberIntegralRecordBO;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.BeanUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * @author ww
 * @create 2023-07-21- 16:45
 * @description:
 */
@Tag(name = "用户 APP - 用户积分记录")
@Validated
@RestController
@RequestMapping("/member/integral")
public class MemberIntegralRecordController {

    @Resource
    private MemberIntegralRecordService memberIntegralRecordService;

    @PostMapping("/add")
    public boolean add(@RequestBody @Validated MemberIntegralRecordBO memberIntegralRecordBO) {
        MemberIntegralRecord memberIntegralRecord = new MemberIntegralRecord();
        BeanUtils.copyProperties(memberIntegralRecordBO, memberIntegralRecord);
        return memberIntegralRecordService.save(memberIntegralRecord);
    }

    @GetMapping("/page")
    public AppPageResult<MemberIntegralRecord> page(AppPage page) {
        return memberIntegralRecordService.page(page);
    }

    @GetMapping("/getDetail")
    public MemberIntegralRecord getDetail(@RequestParam("integralId") String integralId) {
        return memberIntegralRecordService.memberIntegralRecordDetail(integralId);
    }

}
