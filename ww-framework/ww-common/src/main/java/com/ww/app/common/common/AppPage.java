package com.ww.app.common.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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

    @Schema(description = "当前页数")
    private Integer pageNum = 1;

    @Schema(description = "每页数量")
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
