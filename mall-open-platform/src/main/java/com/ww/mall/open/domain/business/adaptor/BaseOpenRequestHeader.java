package com.ww.mall.open.domain.business.adaptor;

import lombok.Data;

/**
 * @author ww
 * @create 2024-05-25 10:59
 * @description:
 */
@Data
public class BaseOpenRequestHeader {

    private String transId;

    private String openServerId;

    private String serviceCode;

    private String openUser;

    private String openPwd;

    private String reqTime;

    private String resTime;

}
