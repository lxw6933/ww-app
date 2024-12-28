package com.ww.mall.common.common;

import lombok.Data;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * @description: 批量操作结果
 * @author: ww
 * @create: 2021-05-17 13:26
 */
@Data
public class BatchProcessingResult {

    /**
     * 成功条数
     */
    private int success;

    /**
     * 错误条数
     */
    private int error;

    /**
     * 错误原因
     */
    private Set<String> errorMsg;

    public BatchProcessingResult() {
        this.success = 0;
        this.error = 0;
        this.errorMsg = new LinkedHashSet<>();
    }

    public BatchProcessingResult addError(int error) {
        this.error = this.error + error;
        return this;
    }

    public BatchProcessingResult addSuccess(int success) {
        this.success = this.success + success;
        return this;
    }

    public BatchProcessingResult addErrorMsg(String errorMsg) {
        this.errorMsg.add(errorMsg);
        return this;
    }
}
