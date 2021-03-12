package com.ww.mall.member.to;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;

/**
 * Author:         ww
 * Datetime:       2021\3\12 0012
 * Description:
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CategoryTO {
    /**
     * 层级
     */
    private Integer catLevel;
}
