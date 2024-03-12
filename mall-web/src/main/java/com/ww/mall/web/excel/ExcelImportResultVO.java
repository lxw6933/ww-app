package com.ww.mall.web.excel;

import lombok.Data;

/**
 * @author ww
 * @create 2024-03-12- 11:09
 * @description:
 */
@Data
public class ExcelImportResultVO {

    /**
     * 成功数量
     */
    private Integer successNum = 0;

    /**
     * 失败数量
     */
    private Integer failNum = 0;

    /**
     * 导入总条数
     */
    private Integer totalNum = 0;

    /**
     * 失败文件下载地址路径,失败条数为0时为空
     */
    private String failDataFileUrl;

}
