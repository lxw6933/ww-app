package com.ww.app.ssh.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 主机指标快照响应。
 * <p>
 * 用于让前端区分：
 * 1. 尚未准备完成；<br>
 * 2. 已有可用快照；<br>
 * 3. 正在展示旧缓存（stale）；<br>
 * 4. 最近一次采集失败。<br>
 * </p>
 */
@Data
public class MetricsSnapshotResponse {

    /**
     * 是否已有成功快照。
     */
    private boolean ready;

    /**
     * 当前展示的数据是否为旧缓存。
     */
    private boolean stale;

    /**
     * 最近一次成功更新时间。
     */
    private Long updatedAt;

    /**
     * 最近一次采集尝试时间。
     */
    private Long lastAttemptAt;

    /**
     * 最近一次采集失败信息。
     */
    private String lastError;

    /**
     * 指标列表。
     */
    private List<HostMetricSnapshot> metrics = new ArrayList<>();
}
