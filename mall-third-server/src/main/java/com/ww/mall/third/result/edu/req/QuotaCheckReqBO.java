package com.ww.mall.third.result.edu.req;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * @author ww
 * @create 2023-03-27- 17:55
 * @description:
 */
@Data
public class QuotaCheckReqBO {

    @JSONField(ordinal = 1)
    @NotNull(message = "活动编码不能为空")
    private String campainNo;

    @JSONField(ordinal = 2)
    private String sublob;

    @JSONField(ordinal = 3)
    @NotNull(message = "学信⽹验证码不能为空")
    private String xxwCheckCode;

    @JSONField(ordinal = 4)
    private String mpn;

    @JSONField(ordinal = 5)
    private String cmd;

    @JSONField(ordinal = 6)
    private String lob;

}
