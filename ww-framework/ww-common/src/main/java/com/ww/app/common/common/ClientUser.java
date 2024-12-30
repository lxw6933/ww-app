package com.ww.app.common.common;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author ww
 * @create 2023-07-18- 14:31
 * @description:
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ClientUser extends BaseUser {

    /**
     * 手机号
     */
    private String mobile;

    /**
     * 渠道id
     */
    private Long channelId;

}
