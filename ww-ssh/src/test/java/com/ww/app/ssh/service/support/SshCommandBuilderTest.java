package com.ww.app.ssh.service.support;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * {@link SshCommandBuilder} 命令拼装测试。
 */
class SshCommandBuilderTest {

    /**
     * 校验日志列表命令包含目录扫描语义。
     */
    @Test
    void shouldBuildListFilesCommand() {
        SshCommandBuilder builder = new SshCommandBuilder();
        String command = builder.buildListFilesCommand("/data/logs");
        Assertions.assertTrue(command.contains("find"));
        Assertions.assertTrue(command.contains("*.log"));
        Assertions.assertTrue(command.contains("*.log.*"));
    }

    /**
     * 校验最新日志命令包含按时间倒序选择逻辑。
     */
    @Test
    void shouldBuildLatestFileCommand() {
        SshCommandBuilder builder = new SshCommandBuilder();
        String command = builder.buildLatestFileCommand("/data/logs");
        Assertions.assertTrue(command.contains("ls -1t"));
        Assertions.assertTrue(command.contains("head -n 1"));
        Assertions.assertTrue(command.contains("date +%F"));
        Assertions.assertTrue(command.contains("TODAY_FILE"));
        Assertions.assertTrue(command.contains("*.log.*"));
    }

    /**
     * 校验 tail 命令具备 -F 到 -f 的兼容回退。
     */
    @Test
    void shouldBuildTailCommandWithFallback() {
        SshCommandBuilder builder = new SshCommandBuilder();
        String command = builder.buildTailCommand("/data/logs/app.log", 200);
        Assertions.assertTrue(command.contains("tail -n 200 -F"));
        Assertions.assertTrue(command.contains("||"));
        Assertions.assertTrue(command.contains("tail -n 200 -f"));
    }

    /**
     * 校验磁盘指标采集命令包含根分区采集与百分比处理逻辑。
     */
    @Test
    void shouldBuildDiskUsageCommand() {
        SshCommandBuilder builder = new SshCommandBuilder();
        String command = builder.buildDiskUsageCommand();
        Assertions.assertTrue(command.contains("df -Pm /"));
        Assertions.assertTrue(command.contains("gsub(/%/"));
        Assertions.assertTrue(command.contains("$5"));
    }

    /**
     * 校验 JVM GC 命令包含 PID 识别、jstat 采集与统一输出标记。
     */
    @Test
    void shouldBuildJvmGcStatsCommand() {
        SshCommandBuilder builder = new SshCommandBuilder();
        String command = builder.buildJvmGcStatsCommand("/data/app/mall-basic/bin/server.sh", "mall-basic@node1");
        Assertions.assertTrue(command.contains("__WW_JVM_GC__"));
        Assertions.assertTrue(command.contains("jstat -gcutil"));
        Assertions.assertTrue(command.contains("NO_PID"));
        Assertions.assertTrue(command.contains("SERVICE_GROUP"));
        Assertions.assertTrue(command.contains("collect_pid_from_file"));
        Assertions.assertTrue(command.contains("jps -l"));
    }
}
