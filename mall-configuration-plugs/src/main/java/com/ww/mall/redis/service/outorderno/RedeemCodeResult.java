package com.ww.mall.redis.service.outorderno;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author ww
 * @create 2024-08-27- 14:18
 * @description:
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RedeemCodeResult {

    private String status;

    private List<String> redeemCodes;

    private int remaining;

}
