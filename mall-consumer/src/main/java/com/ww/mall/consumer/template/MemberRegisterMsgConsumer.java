package com.ww.mall.consumer.template;

import com.ww.mall.common.common.Result;
import com.ww.mall.common.exception.ApiException;
import com.ww.mall.web.feign.MemberFeignService;
import com.ww.mall.web.utils.SpringContextManager;
import com.ww.mall.web.view.bo.AddMemberIntegralBO;
import lombok.extern.slf4j.Slf4j;

/**
 * @description:
 * @author: ww
 * @create: 2023/7/22 23:13
 **/
@Slf4j
public class MemberRegisterMsgConsumer extends MsgConsumerTemplate {
    private final MemberFeignService memberFeignService = SpringContextManager.getBean(MemberFeignService.class);

    @Override
    boolean serverHandler(Object msg) {
        AddMemberIntegralBO addMemberIntegralBO = new AddMemberIntegralBO();
        addMemberIntegralBO.setMemberId((Long) msg);
        addMemberIntegralBO.setIntegralType(true);
        addMemberIntegralBO.setIntegralNum(100);
        Result<Boolean> booleanResult = memberFeignService.addMemberIntegral(addMemberIntegralBO);
        if (Boolean.TRUE.equals(booleanResult.isSuccess())) {
            return true;
        } else {
            // 远程调用失败
            log.error("【新用户注册】消费者远程调用失败：{}", booleanResult.getMessage());
            throw new ApiException("消费者远程调用失败");
        }
    }
}
