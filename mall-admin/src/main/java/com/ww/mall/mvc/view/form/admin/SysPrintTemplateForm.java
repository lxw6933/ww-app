package com.ww.mall.mvc.view.form.admin;

import com.ww.mall.common.valid.group.UpdateGroup;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

/**
 * @description:
 * @author: ww
 * @create: 2021-05-17 13:27
 */
@Data
public class SysPrintTemplateForm {

    /**
     * ID
     */
    @NotNull(groups = {UpdateGroup.class}, message = "id不能为空")
    private Long id;

    /**
     * 中心端ID
     */
    @NotNull(message = "centerId不能为空")
    private Long centerId;

    /**
     * 模板名称
     */
    @NotEmpty(message = "name不能为空")
    @Length(max = 50, message = "name字符长度不能大于50")
    private String name;

    /**
     * 模板类型，关联sys_print_type
     */
    @NotNull(message = "typeId不能为空")
    private Long typeId;

    /**
     * 模板内容
     */
    @NotEmpty(message = "content不能为空")
    @Length(max = 65535, message = "content字符长度不能大于65535")
    private String content;

    /**
     * 是否系统默认（0：否，1：是）
     */
    @NotNull(message = "isDefault不能为空")
    private Boolean isDefault;

}
