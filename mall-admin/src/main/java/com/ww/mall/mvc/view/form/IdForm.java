package com.ww.mall.mvc.view.form;

import com.ww.mall.common.valid.group.BatchDeleteGroup;
import com.ww.mall.common.valid.group.DeleteGroup;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * @description:
 * @author: ww
 * @create: 2021-05-19 10:42
 */
@Data
public class IdForm {

    @NotNull(groups = {DeleteGroup.class}, message = "id不能为空")
    private Long id;

    @NotEmpty(groups = {BatchDeleteGroup.class}, message = "至少选择一条记录")
    private List<Long> ids;

}
