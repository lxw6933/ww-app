package com.ww.mall.mvc.view.form.admin;

import com.ww.mall.common.valid.group.UpdateGroup;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

/**
 * @description:
 * @author: ww
 * @create: 2021-05-17 13:46
 */
@Data
public class SysPrintTypeForm {

    /**
     * ID
     */
    @NotNull(groups = {UpdateGroup.class}, message = "id不能为空")
    private Long id;

    /**
     * 类型名称
     */
    @NotEmpty(message = "name不能为空")
    @Length(max = 50, message = "name字符长度不能大于50")
    private String name;

    /**
     * 所属平台（0：平台，1：租户）
     */
    @NotNull(message = "systemType不能为空")
    private Integer systemType;

}

