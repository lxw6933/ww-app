package com.ww.app.third.edu;

import lombok.Data;

/**
 * @author ww
 * @create 2023-03-27- 17:25
 * @description:
 */
@Data
public class BaseAppleEduResultVO<T> {

    private String code;

    private String error;

    private String status;

    private T payload;

}
