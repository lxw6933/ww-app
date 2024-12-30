package com.ww.app.third.edu.res;

import lombok.Data;

/**
 * @author ww
 * @create 2023-03-27- 17:33
 * @description:
 */
@Data
public class XxwValidResultVO {

    /**
     * 学信⽹验证码
     */
    private String xxwCheckCode;

    /**
     * 学⽣学籍 （InValid：毕业、Valid：在读）
     */
    private String status;

}
