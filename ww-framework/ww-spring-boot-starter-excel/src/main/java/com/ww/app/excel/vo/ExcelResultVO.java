package com.ww.app.excel.vo;

import lombok.Data;

/**
 * @author ww
 * @create 2024-03-12- 11:09
 * @description:
 */
@Data
public class ExcelResultVO {

    /**
     * 成功数量
     */
    private int successNum;

    /**
     * 失败数量
     */
    private int failNum;

    /**
     * 导入总条数
     */
    private int totalNum;

    /**
     * 失败文件下载地址路径,失败条数为0时为空
     */
    private String failDataFileUrl;

}
