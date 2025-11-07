package com.ww.app.common.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import java.io.Serializable;

/**
 * @author ww
 * @create 2023-07-17- 15:39
 * @description:
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AppPage implements Serializable {

    @Schema(description = "页码（从1开始）", example = "1")
    @Min(value = 1, message = "页码必须大于等于1")
    private Integer pageNum = 1;

    @Schema(description = "每页数量", example = "10")
    @Min(value = 1, message = "每页数量必须大于等于1")
    @Max(value = 100, message = "每页数量不能超过100")
    private Integer pageSize = 10;

    public Integer getPageNum() {
        if (this.pageNum < 1) {
            return 1;
        }
        return this.pageNum;
    }

    public Integer getPageSize() {
        if (this.pageSize < 1) {
            return 10;
        }
        return this.pageSize;
    }

}
