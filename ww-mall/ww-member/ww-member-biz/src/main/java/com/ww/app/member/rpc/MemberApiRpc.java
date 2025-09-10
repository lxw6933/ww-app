package com.ww.app.member.rpc;

import com.ww.app.common.common.Result;
import com.ww.app.member.member.bo.AddMemberIntegralBO;
import com.ww.app.member.member.dto.MemberDTO;
import com.ww.app.member.member.rpc.MemberApi;
import com.ww.app.member.service.MemberIntegralRecordService;
import com.ww.app.member.service.MemberService;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * @description:
 * @author: ww
 * @create: 2023/7/22 11:16
 **/
@RestController
public class MemberApiRpc implements MemberApi {

    @Resource
    private MemberService memberService;

    @Resource
    private MemberIntegralRecordService memberIntegralRecordService;

    @Override
    public Result<MemberDTO> getMemberByMobile(@RequestParam("mobile") String mobile) {
        return Result.success(memberService.getMemberByMobile(mobile));
    }

    @Override
    public Result<Boolean> addMemberIntegral(AddMemberIntegralBO addMemberIntegralBO) {
        return Result.success(memberIntegralRecordService.addNewMemberIntegral(addMemberIntegralBO));
    }

    @Override
    public Result<String> test() {
        return Result.success("member openFeign hello");
    }

}
