package com.ww.mall.open.domain.business.entity;

import com.ww.mall.open.domain.business.adaptor.BaseOpenRequestHeader;
import lombok.Data;

/**
 * @author ww
 * @create 2024-05-25 13:47
 * @description:
 */
@Data
public class BaseOpenResponse {

    private BaseOpenRequestHeader header;

    private String resBody;

}
