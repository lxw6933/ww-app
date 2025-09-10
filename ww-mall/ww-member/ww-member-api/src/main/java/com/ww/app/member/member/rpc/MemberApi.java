package com.ww.app.member.member.rpc;

import com.ww.app.common.common.Result;
import com.ww.app.member.member.bo.AddMemberIntegralBO;
import com.ww.app.member.member.constants.ApiConstants;
import com.ww.app.member.member.dto.MemberDTO;
import com.ww.app.member.member.fallback.MemberApiFallBack;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "RPC 服务 - C端 会员")
@FeignClient(value = ApiConstants.NAME, fallbackFactory = MemberApiFallBack.class)
public interface MemberApi {

    String PREFIX = ApiConstants.PREFIX + "/user";

    /**
     * 通过手机号获取用户信息
     *
     * @param mobile 手机号
     * @return MemberDTO
     */
    @GetMapping(PREFIX + "/getMemberByMobile")
    @Schema(description = "通过手机号获取用户信息")
    @Parameter(name = "mobile", description = "手机号", required = true)
    Result<MemberDTO> getMemberByMobile(@RequestParam("mobile") String mobile);

    /**
     * 新增用户积分记录
     *
     * @param addMemberIntegralBO bo
     * @return boolean
     */
    @PostMapping(PREFIX + "/addNewMemberIntegral")
    @Schema(description = "新增用户积分记录")
    Result<Boolean> addMemberIntegral(@RequestBody AddMemberIntegralBO addMemberIntegralBO);

    /**
     * test openFeign pk grpc
     */
    @GetMapping(PREFIX + "/test")
    @Schema(description = "测试")
    Result<String> test();

}
