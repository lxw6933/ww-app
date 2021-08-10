package com.ww.mall.mvc.view.query.admin;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ww.mall.mvc.entity.MqMsgLogEntity;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

/**
 * @description:
 * @author: ww
 * @create: 2021/6/30 上午9:57
 **/
@Data
public class MqMsgLogQuery {

    /**
     * ack消息id
     */
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

    public QueryWrapper<MqMsgLogEntity> getQueryWrapper() {
        QueryWrapper<MqMsgLogEntity> wrapper = new QueryWrapper<>();
        if (StringUtils.isNotBlank(this.msgId)) {
            wrapper.eq("`msg_id`", this.msgId);
        }
        if (StringUtils.isNotBlank(this.message)) {
            wrapper.like("`message`", this.message);
        }
        if (StringUtils.isNotBlank(this.exchange)) {
            wrapper.like("`exchange`", this.exchange);
        }
        if (StringUtils.isNotBlank(this.routingKey)) {
            wrapper.like("`routing_key`", this.routingKey);
        }
        if (this.status != null) {
            wrapper.eq("`status`", this.status);
        }
        if (this.tryCount != null) {
            wrapper.eq("`try_count`", this.tryCount);
        }
        return wrapper;
    }

}
