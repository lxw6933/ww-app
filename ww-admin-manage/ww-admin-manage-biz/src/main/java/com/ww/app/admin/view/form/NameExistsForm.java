package com.ww.app.admin.view.form;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @author NineSu
 */
@Data
public class NameExistsForm {

    @Schema(title = "名称")
    private String name;
    @Schema(title = "主键")
    private Long id;
}
