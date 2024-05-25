package com.ww.mall.open.domain.business.adaptor;

import com.alibaba.fastjson.JSONObject;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author ww
 * @create 2024-05-25 10:59
 * @description:
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BaseOpenRequest {

    private BaseOpenRequestHeader header;

    private JSONObject reqBody;

}
