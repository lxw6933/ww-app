package com.ww.mall.auth.feign.inner;

import com.ww.mall.auth.feign.MemberFeignService;
import com.ww.mall.common.common.Result;
import com.ww.mall.common.enums.CodeEnum;
import com.ww.mall.web.view.dto.MemberDTO;
import org.springframework.stereotype.Component;

/**
 * @author ww
 * @create 2023-07-18- 10:32
 * @description:
 */
@Component
public class MemberFeignServiceFallBack implements MemberFeignService {

    @Override
    public Result<MemberDTO> getMemberByMobile(String mobile) {
        return new Result<>(CodeEnum.LIMIT_ERROR.getCode(), CodeEnum.LIMIT_ERROR.getMessage());
    }
}
