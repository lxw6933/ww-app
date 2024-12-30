package com.ww.app.common.exception;

import com.ww.app.common.common.ResCode;
import com.ww.app.common.enums.GlobalResCodeConstants;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/**
 * @author ww
 * @create 2023-07-15- 10:18
 * @description: 接口异常类
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ApiException extends RuntimeException {

    @Setter
    @Getter
    private Integer code;

    private String message;

    public ApiException() {}

    public ApiException(String message) {
        this.code = GlobalResCodeConstants.SYSTEM_ERROR.getCode();
        this.message = message;
    }

    public ApiException(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    public ApiException(ResCode resCode) {
        this.code = resCode.getCode();
        this.message = resCode.getMsg();
    }

}
