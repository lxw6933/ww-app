package com.ww.app.common.common;

import com.ww.app.common.valid.group.BatchDeleteGroup;
import com.ww.app.common.valid.group.DeleteGroup;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * @author ww
 * @create 2024-05-21- 16:24
 * @description:
 */
@Data
public class IdForm {

    @NotNull(groups = {DeleteGroup.class}, message = "id不能为空")
    private Long id;

    @NotEmpty(groups = {BatchDeleteGroup.class}, message = "至少选择一条记录")
    private List<Long> ids;

}
