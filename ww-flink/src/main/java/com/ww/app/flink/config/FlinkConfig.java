package com.ww.app.flink.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.restartstrategy.RestartStrategies;
import org.apache.flink.runtime.state.filesystem.FsStateBackend;
import org.apache.flink.runtime.state.memory.MemoryStateBackend;
import org.apache.flink.streaming.api.CheckpointingMode;
import org.apache.flink.streaming.api.environment.CheckpointConfig;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Flink配置类
 */
@Slf4j
@Configuration
public class FlinkConfig {

    @Value("${ww.flink.parallelism:1}")
    private int parallelism;

    @Value("${ww.flink.checkpoint-interval:60000}")
    private long checkpointInterval;

    @Value("${ww.flink.checkpoint-timeout:30000}")
    private long checkpointTimeout;

    @Value("${ww.flink.min-pause-between-checkpoints:10000}")
    private long minPauseBetweenCheckpoints;

    @Value("${ww.flink.max-concurrent-checkpoints:1}")
    private int maxConcurrentCheckpoints;

    @Value("${ww.flink.enable-checkpoint:true}")
    private boolean enableCheckpoint;

    @Value("${ww.flink.enable-restart:true}")
    private boolean enableRestart;

    @Value("${ww.flink.restart-attempts:3}")
    private int restartAttempts;

    @Value("${ww.flink.restart-delay-ms:10000}")
    private long restartDelayMs;

    @Value("${ww.flink.state-backend:memory}")
    private String stateBackend;

    @Value("${ww.flink.state-backend-path:}")
    private String stateBackendPath;

    /**
     * 创建Flink流处理执行环境
     */
    @Bean
    public StreamExecutionEnvironment streamExecutionEnvironment() {
        // 创建执行环境
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        
        // 设置并行度
        env.setParallelism(parallelism);
        
        // 配置检查点
        if (enableCheckpoint) {
            // 开启检查点，设置检查点间隔时间和模式
            env.enableCheckpointing(checkpointInterval, CheckpointingMode.EXACTLY_ONCE);
            
            // 获取检查点配置
            CheckpointConfig checkpointConfig = env.getCheckpointConfig();
            
            // 设置检查点超时时间
            checkpointConfig.setCheckpointTimeout(checkpointTimeout);
            
            // 设置两次检查点之间的最小时间间隔
            checkpointConfig.setMinPauseBetweenCheckpoints(minPauseBetweenCheckpoints);
            
            // 同一时间只允许进行一个检查点
            checkpointConfig.setMaxConcurrentCheckpoints(maxConcurrentCheckpoints);
            
            // 设置检查点的存储位置
            switch (stateBackend.toLowerCase()) {
                case "filesystem":
                    if (stateBackendPath != null && !stateBackendPath.isEmpty()) {
                        try {
                            FsStateBackend fsStateBackend = new FsStateBackend(stateBackendPath);
                            env.setStateBackend(fsStateBackend);
                        } catch (Exception e) {
                            log.error("Failed to configure filesystem state backend", e);
                        }
                    }
                    break;
                case "rocksdb":
                    log.warn("RocksDB state backend is not configured by default. Add flink-runtime-web dependency and configure it manually.");
                    break;
                case "memory":
                default:
                    env.setStateBackend(new MemoryStateBackend());
                    break;
            }

            // 作业取消时保留检查点
            checkpointConfig.setExternalizedCheckpointCleanup(CheckpointConfig.ExternalizedCheckpointCleanup.RETAIN_ON_CANCELLATION);
        }
        
        // 配置重启策略
        if (enableRestart) {
            env.setRestartStrategy(RestartStrategies.fixedDelayRestart(
                    restartAttempts,
                    restartDelayMs
            ));
        } else {
            env.setRestartStrategy(RestartStrategies.noRestart());
        }
        
        log.info("Flink执行环境配置完成，并行度: {}, 检查点间隔: {}ms", parallelism, checkpointInterval);
        return env;
    }
} 