package com.ww.app.common.common;

import lombok.Data;

/**
 * @author ww
 * @create 2024-09-18- 15:18
 * @description:
 */
@Data
public class ResCode {

    private final Integer code;

    private final String msg;

    public ResCode(Integer code, String msg) {
        this.code = code;
        this.msg = msg;
    }

}
