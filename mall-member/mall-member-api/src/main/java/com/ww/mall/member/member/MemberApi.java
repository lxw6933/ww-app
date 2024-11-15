package com.ww.mall.member.member;

import com.ww.mall.common.common.Result;
import com.ww.mall.member.member.bo.AddMemberIntegralBO;
import com.ww.mall.member.member.dto.MemberDTO;
import com.ww.mall.member.member.fallback.MemberApiFallBack;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * @author ww
 * @create 2024-11-15- 13:59
 * @description:
 */
@FeignClient(value = "mall-member", fallbackFactory = MemberApiFallBack.class)
public interface MemberApi {

    /**
     * 通过手机号获取用户信息
     *
     * @param mobile 手机号
     * @return MemberDTO
     */
    @GetMapping("/mall-member/member/inner/getMemberByMobile")
    Result<MemberDTO> getMemberByMobile(@RequestParam("mobile") String mobile);

    /**
     * 新增用户积分记录
     *
     * @param addMemberIntegralBO bo
     * @return boolean
     */
    @PostMapping("/mall-member/member/inner/addNewMemberIntegral")
    Result<Boolean> addMemberIntegral(@RequestBody AddMemberIntegralBO addMemberIntegralBO);

    /**
     * test openFeign pk grpc
     */
    @GetMapping("/mall-member/member/inner/test")
    Result<String> test();

}
