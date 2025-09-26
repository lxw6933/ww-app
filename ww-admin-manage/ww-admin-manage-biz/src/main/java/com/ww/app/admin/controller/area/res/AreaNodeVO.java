package com.ww.app.admin.controller.area.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * @author ww
 * @create 2025-09-25 18:25
 * @description:
 */
@Data
public class AreaNodeVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer id;

    @Schema(description = "名字", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Schema(description = "子节点集合")
    private List<AreaNodeVO> children;

}
