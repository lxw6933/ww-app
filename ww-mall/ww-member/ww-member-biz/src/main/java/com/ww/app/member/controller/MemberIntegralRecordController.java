package com.ww.app.member.controller;

import com.ww.app.member.entity.mongo.MemberIntegralRecord;
import com.ww.app.member.service.MemberIntegralRecordService;
import com.ww.app.member.view.bo.MemberIntegralRecordBO;
import com.ww.app.common.common.AppPage;
import com.ww.app.common.common.AppPageResult;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * @author ww
 * @create 2023-07-21- 16:45
 * @description:
 */
@Validated
@RestController
@RequestMapping("/integral")
public class MemberIntegralRecordController {

    @Autowired
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
