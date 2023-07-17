package com.ww.mall.web.cmmon;

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

    private Integer pageNum;

    private Integer pageSize;
}
