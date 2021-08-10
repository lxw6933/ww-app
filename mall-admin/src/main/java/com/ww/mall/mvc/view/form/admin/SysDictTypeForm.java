package com.ww.mall.mvc.view.form.admin;

import com.ww.mall.common.valid.group.UpdateGroup;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

/**
 * @description:
 * @author: ww
 * @create: 2021-05-18 17:45
 */
@Data
public class SysDictTypeForm {

    /**
     * id
     */
    @NotNull(groups = {UpdateGroup.class}, message = "id不能为空")
    private Long id;

    /**
     * 字典名称
     */
    @NotEmpty(message = "name不能为空")
    @Length(max = 100, message = "name字符长度不能大于100")
    private String name;

    /**
     * 字典类型
     */
    @NotEmpty(message = "type不能为空")
    @Length(max = 100, message = "type字符长度不能大于100")
    private String type;

    /**
     * 状态（0：停用，1：正常）
     */
    @NotNull(message = "状态不能为空")
    private Boolean status;

    /**
     * 备注
     */
    @Length(max = 500, message = "remarks字符长度不能大于500")
    private String remarks;

}

