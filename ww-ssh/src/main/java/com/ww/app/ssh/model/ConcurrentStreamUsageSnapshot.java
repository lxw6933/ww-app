package com.ww.app.ssh.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 实时日志并发流占用快照。
 * <p>
 * 该模型用于向前端返回当前 {@code maxConcurrentStreams} 的使用情况，
 * 并按客户端来源 IP 聚合展示每条活跃流的明细。
 * </p>
 */
@Data
public class ConcurrentStreamUsageSnapshot {

    /**
     * 并发流总上限。
     */
    private int maxConcurrentStreams;

    /**
     * 当前已占用的并发流数量。
     */
    private int activeConcurrentStreams;

    /**
     * 当前剩余可用的并发流数量。
     */
    private int remainingConcurrentStreams;

    /**
     * 当前活跃来源 IP 分组数量。
     */
    private int activeClientIpCount;

    /**
     * 快照生成时间戳。
     */
    private long updatedAt;

    /**
     * 按客户端 IP 聚合后的流占用分组。
     */
    private List<IpUsageGroup> ipGroups = new ArrayList<>();

    /**
     * 设置按客户端 IP 聚合后的流占用分组。
     *
     * @param ipGroups IP 分组列表
     */
    public void setIpGroups(List<IpUsageGroup> ipGroups) {
        this.ipGroups = ipGroups == null ? new ArrayList<>() : ipGroups;
    }

    /**
     * 客户端 IP 维度的流占用分组。
     */
    @Data
    public static class IpUsageGroup {

        /**
         * 客户端来源 IP。
         */
        private String clientIp;

        /**
         * 当前 IP 对应的活跃流数量。
         */
        private int streamCount;

        /**
         * 当前 IP 对应的活跃 WebSocket 会话数量。
         */
        private int sessionCount;

        /**
         * 当前 IP 最早开始占用流的时间戳。
         */
        private long firstStartedAt;

        /**
         * 当前 IP 下的流明细列表。
         */
        private List<StreamUsageItem> streams = new ArrayList<>();

        /**
         * 设置当前 IP 下的流明细列表。
         *
         * @param streams 流明细列表
         */
        public void setStreams(List<StreamUsageItem> streams) {
            this.streams = streams == null ? new ArrayList<>() : streams;
        }
    }

    /**
     * 单条活跃流明细。
     */
    @Data
    public static class StreamUsageItem {

        /**
         * 流唯一标识。
         */
        private String streamId;

        /**
         * WebSocket 会话 ID。
         */
        private String sessionId;

        /**
         * 项目名称。
         */
        private String project;

        /**
         * 环境名称。
         */
        private String env;

        /**
         * 服务名称或实例键。
         */
        private String service;

        /**
         * 目标主机地址。
         */
        private String host;

        /**
         * 实际读取的日志文件路径。
         */
        private String filePath;

        /**
         * 读取模式。
         */
        private String readMode;

        /**
         * 流开始时间戳。
         */
        private long startedAt;
    }
}
