package com.ww.app.admin.view.form;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author NineSu
 */
@Data
public class PathExistsForm {

    @Schema(title = "路径")
    private String path;
    @Schema(title = "主键")
    private Long id;
}
