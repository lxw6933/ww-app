package com.ww.mall.mvc.view.form.admin;

import com.ww.mall.common.valid.group.UpdateGroup;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * @description:
 * @author: ww
 * @create: 2021-05-18 16:58
 */
@Data
public class SysDictForm {

    /**
     * id
     */
    @NotNull(groups = {UpdateGroup.class}, message = "id不能为空")
    private Long id;

    /**
     * 名称
     */
    @NotBlank(message = "字典label不能为空")
    private String label;

    /**
     * 字典码
     */
    @NotBlank(message = "字典码不能为空")
    private String code;

    /**
     * 字典类型
     */
    private String dictType;

}

