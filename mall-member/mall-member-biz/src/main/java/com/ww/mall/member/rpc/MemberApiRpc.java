package com.ww.mall.member.rpc;

import com.ww.mall.common.common.Result;
import com.ww.mall.member.member.rpc.MemberApi;
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
public class MemberApiRpc implements MemberApi {

    @Autowired
    private MemberService memberService;

    @Autowired
    private MemberIntegralRecordService memberIntegralRecordService;

    @Override
    @GetMapping("/getMemberByMobile")
    public Result<MemberDTO> getMemberByMobile(@RequestParam("mobile") String mobile) {
        return Result.success(memberService.getMemberByMobile(mobile));
    }

    @Override
    @PostMapping("/addNewMemberIntegral")
    public Result<Boolean> addMemberIntegral(AddMemberIntegralBO addMemberIntegralBO) {
        return Result.success(memberIntegralRecordService.addNewMemberIntegral(addMemberIntegralBO));
    }

    @Override
    @GetMapping("/test")
    public Result<String> test() {
        return Result.success("member openFeign hello");
    }

}
