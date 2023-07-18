package com.ww.mall.auth.feign;

import com.ww.mall.auth.feign.inner.MemberFeignServiceFallBack;
import com.ww.mall.common.common.Result;
import com.ww.mall.web.view.dto.MemberDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * @author ww
 * @create 2023-07-18- 10:31
 * @description:
 */
@FeignClient(value = "mall-member", fallbackFactory = MemberFeignServiceFallBack.class)
public interface MemberFeignService {

    @GetMapping("/mall-member/member/getMemberByMobile")
    Result<MemberDTO> getMemberByMobile(@RequestParam("mobile") String mobile);

}
