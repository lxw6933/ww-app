package com.ww.mall.mvc.view.query.admin;

import lombok.Data;

/**
 * 任务详情 - Query
 *
 * @author ww
 * @date 2021-05-14 09:30:37
 */
@Data
public class SysJobDetailQuery {
    /**
     * 状态， 0:关闭，1：启用
     */
    private Boolean enabled;

    /**
     * 上次执行状态，0：失败，1：成功
     */
    private Boolean lastExecutionStatus;
}