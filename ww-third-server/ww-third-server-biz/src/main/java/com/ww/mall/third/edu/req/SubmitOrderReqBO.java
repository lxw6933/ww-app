package com.ww.mall.third.edu.req;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.Collections;
import java.util.List;

/**
 * @author ww
 * @create 2023-03-28- 16:45
 * @description:
 */
@Data
public class SubmitOrderReqBO {

    @JSONField(ordinal = 1)
    @NotNull(message = "活动编码不能为空")
    private String campainNo;

    @JSONField(ordinal = 2)
    @NotNull(message = "预订单保留时⻓不能为空")
    private Integer reserveTime;

    @JSONField(ordinal = 3)
    @NotNull(message = "学信⽹验证码不能为空")
    private String xxwCheckCode;

    @JSONField(ordinal = 4)
    @NotNull(message = "交易id不能为空")
    private String transcationID;

    @JSONField(ordinal = 5)
    private String cmd;

    @JSONField(ordinal = 6)
    @NotNull(message = "下单mpn不能为空")
    private List<Mpn> items;

    public SubmitOrderReqBO() {}

    public SubmitOrderReqBO(String mpn) {
        Mpn item = new Mpn();
        item.setMpn(mpn);
        this.items = Collections.singletonList(item);
    }

    @Data
    static class Mpn {
        private String mpn;
    }

}
