package com.ww.mall.common.common;

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
public class MallPage implements Serializable {

    private Integer pageNum = 1;

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
