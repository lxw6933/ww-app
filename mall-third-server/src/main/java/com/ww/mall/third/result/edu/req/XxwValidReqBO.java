package com.ww.mall.third.result.edu.req;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * @author ww
 * @create 2023-03-27- 17:30
 * @description:
 */
@Data
public class XxwValidReqBO {

    @JSONField(ordinal = 1)
    @NotNull(message = "学信⽹验证码不能为空")
    private String xxwCheckCode;

    @JSONField(ordinal = 2)
    private String cmd;

}
