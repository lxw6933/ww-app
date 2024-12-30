package com.ww.app.member.view.to;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author:         ww
 * @Datetime:       2021\3\12 0012
 * @Description:
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CategoryTo {
    /**
     * 层级
     */
    private Integer catLevel;
}
