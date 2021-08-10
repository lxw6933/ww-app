package com.ww.mall.mvc.view.form.admin;

import com.ww.mall.common.valid.group.UpdateGroup;
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * @description:
 * @author: ww
 * @create: 2021-05-18 16:58
 */
@Data
public class SysConfigForm {

    /**
     * id
     */
    @NotNull(groups = {UpdateGroup.class}, message = "id不能为空")
    private Long id;

    /**
     * key
     */
    private String paramKey;

    /**
     * value
     */
    private String paramValue;

    /**
     * 状态 0：隐藏 1：显示
     */
    @NotNull(message = "状态不能为空")
    private Boolean status;

    /**
     * 备注
     */
    private String remark;

}

