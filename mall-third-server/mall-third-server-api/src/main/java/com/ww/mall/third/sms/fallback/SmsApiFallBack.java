package com.ww.mall.third.sms.fallback;

import com.ww.mall.common.common.Result;
import com.ww.mall.common.enums.GlobalResCodeConstants;
import com.ww.mall.third.sms.SmsApi;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;

/**
 * @author ww
 * @create 2024-11-15- 14:40
 * @description:
 */
@Slf4j
public class SmsApiFallBack implements FallbackFactory<SmsApi> {
    @Override
    public SmsApi create(Throwable cause) {
        log.error("第三方服务【ThirdServerFeignService】调用异常：{}", cause.getMessage());
        return new SmsApi() {
            @Override
            public Result<Boolean> sendSms(String mobile, String code) {
                return Result.error(GlobalResCodeConstants.LIMIT_REQUEST);
            }
        };
    }
}
