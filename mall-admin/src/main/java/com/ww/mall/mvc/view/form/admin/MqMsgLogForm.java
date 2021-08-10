package com.ww.mall.mvc.view.form.admin;

import com.ww.mall.common.valid.group.UpdateGroup;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.Date;

/**
 * @description:
 * @author: ww
 * @create: 2021/6/30 上午10:04
 **/
@Data
public class MqMsgLogForm {

    /**
     * id
     */
    @NotNull(groups = {UpdateGroup.class}, message = "id不能为空")
    private Long id;

    /**
     * ack消息id
     */
    @NotBlank(message = "ack消息id不能为空")
    private String msgId;

    /**
     * 发送信息体
     */
    private String message;

    /**
     * 交换机
     */
    private String exchange;

    /**
     * 路由key
     */
    private String routingKey;

    /**
     * 消息状态(0投递中 1投递成功 2投递失败 3已消费)
     */
    private Integer status;

    /**
     * 重试次数
     */
    private Integer tryCount;

    /**
     * 下一次重试时间
     */
    private Date nextTryTime;

}
