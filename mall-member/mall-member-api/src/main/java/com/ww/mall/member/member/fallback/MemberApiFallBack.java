package com.ww.mall.member.member.fallback;

import com.ww.mall.common.common.Result;
import com.ww.mall.common.enums.GlobalResCodeConstants;
import com.ww.mall.member.member.MemberApi;
import com.ww.mall.member.member.bo.AddMemberIntegralBO;
import com.ww.mall.member.member.dto.MemberDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;

/**
 * @author ww
 * @create 2024-11-15- 14:01
 * @description:
 */
@Slf4j
public class MemberApiFallBack implements FallbackFactory<MemberApi> {
    @Override
    public MemberApi create(Throwable cause) {
        log.error("第三方服务【MemberFeignService】调用异常：{}", cause.getMessage());
        return new MemberApi() {
            @Override
            public Result<MemberDTO> getMemberByMobile(String mobile) {
                return Result.error(GlobalResCodeConstants.LIMIT_REQUEST);
            }

            @Override
            public Result<Boolean> addMemberIntegral(AddMemberIntegralBO addMemberIntegralBO) {
                return Result.error(GlobalResCodeConstants.LIMIT_REQUEST);
            }

            @Override
            public Result<String> test() {
                return Result.error(GlobalResCodeConstants.LIMIT_REQUEST);
            }
        };
    }

}
