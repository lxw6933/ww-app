package com.ww.app.third.sms.rpc;

import com.ww.app.common.common.Result;
import com.ww.app.third.sms.constants.ApiConstants;
import com.ww.app.third.sms.fallback.SmsApiFallBack;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * @author ww
 * @create 2024-11-15- 14:39
 * @description:
 */
@Tag(name = "RPC 服务 - 短信")
@FeignClient(value = ApiConstants.NAME, fallbackFactory = SmsApiFallBack.class)
public interface SmsApi {

    String PREFIX = ApiConstants.PREFIX + "/sms";

    /**
     * 发送短信验证码
     *
     * @param mobile 发送手机号
     * @param code 验证码
     * @return boolean
     */
    @GetMapping(PREFIX + "/sendCode")
    @Schema(description = "发送短信验证码")
    @Parameters({
            @Parameter(name = "mobile", description = "手机号码", required = true, in = ParameterIn.QUERY),
            @Parameter(name = "code", description = "验证码", required = true, in = ParameterIn.QUERY),
    })
    Result<Boolean> sendSms(@RequestParam("mobile") String mobile, @RequestParam("code") String code);

}
