package com.ww.mall.mvc.view.query.admin;

import lombok.Data;

/**
 * @description:
 * @author: ww
 * @create: 2021-05-18 16:59
 */
@Data
public class SysDictQuery {

    /**
     * 知识类型
     */
    private String label;

    /**
     * 字典类型
     */
    private String dictType;

    /**
     * 状态 0：隐藏 1：显示
     */
    private Boolean status;

}
