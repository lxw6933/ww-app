package com.ww.app.auth.view.vo;

import lombok.Data;

/**
 * @author ww
 * @create 2023-07-18- 10:23
 * @description:
 */
@Data
public class LoginResultVO {

    /**
     * token
     */
    private String accessToken;

    /**
     * token过期时间
     */
    private Long accessTokenExpTime;

}
