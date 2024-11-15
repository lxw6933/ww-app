package com.ww.mall.member.controller.inner;

import com.ww.mall.member.service.MemberIntegralRecordService;
import com.ww.mall.member.service.MemberService;
import com.ww.mall.member.member.bo.AddMemberIntegralBO;
import com.ww.mall.member.member.dto.MemberDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * @description:
 * @author: ww
 * @create: 2023/7/22 11:16
 **/
@RestController
@RequestMapping("/member/inner")
public class MemberInnerController {

    @Autowired
    private MemberService memberService;

    @Autowired
    private MemberIntegralRecordService memberIntegralRecordService;

    @GetMapping("/getMemberByMobile")
    public MemberDTO getMemberByMobile(@RequestParam("mobile") String mobile) {
        return memberService.getMemberByMobile(mobile);
    }

    @PostMapping("/addNewMemberIntegral")
    public Boolean addNewMemberIntegral(@RequestBody AddMemberIntegralBO addMemberIntegralBO) {
        return memberIntegralRecordService.addNewMemberIntegral(addMemberIntegralBO);
    }

    @GetMapping("/test")
    public String test() {
        return "member openFeign hello";
    }

}
