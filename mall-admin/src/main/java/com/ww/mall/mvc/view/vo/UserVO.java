package com.ww.mall.mvc.view.vo;

import lombok.Data;

import java.util.Date;

/**
 * @description:
 * @author: ww
 * @create: 2021-05-12 18:15
 */
@Data
public class UserVO {

    private String name;
    private Date createTime;
    private Long createBy;
}
