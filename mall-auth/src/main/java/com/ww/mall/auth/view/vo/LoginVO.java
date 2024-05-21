package com.ww.mall.auth.view.vo;

import lombok.Data;

/**
 * @author ww
 * @create 2023-07-18- 10:23
 * @description:
 */
@Data
public class LoginVO {

    private String token;

    private Long tokenExpTime;

}
