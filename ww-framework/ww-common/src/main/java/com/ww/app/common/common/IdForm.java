package com.ww.app.common.common;

import com.ww.app.common.valid.group.BatchDeleteGroup;
import com.ww.app.common.valid.group.DeleteGroup;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * @author ww
 * @create 2024-05-21- 16:24
 */
@Data
@Schema(description = "id表单")
public class IdForm {

    @NotNull(groups = {DeleteGroup.class}, message = "id不能为空")
    @Schema(description = "id")
    private Long id;

    @NotEmpty(groups = {BatchDeleteGroup.class}, message = "至少选择一条记录")
    @Schema(description = "id集合")
    private List<Long> ids;

}
