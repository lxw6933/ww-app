package com.ww.mall.member.controller;

import com.ww.mall.member.entity.mongo.MemberIntegralRecord;
import com.ww.mall.member.service.MemberIntegralRecordService;
import com.ww.mall.member.view.bo.MemberIntegralRecordBO;
import com.ww.mall.web.cmmon.MallPage;
import com.ww.mall.web.cmmon.MallPageResult;
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
    public MallPageResult<MemberIntegralRecord> page(MallPage page) {
        return memberIntegralRecordService.page(page);
    }

    @GetMapping("/getDetail")
    public MemberIntegralRecord getDetail(@RequestParam("integralId") String integralId) {
        return memberIntegralRecordService.memberIntegralRecordDetail(integralId);
    }

}
