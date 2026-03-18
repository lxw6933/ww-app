package com.ww.app.ssh.service.support;

import com.ww.app.ssh.model.LogStreamRequest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

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
     * 校验 tail 实时跟随命令会从当前时刻开始订阅，避免重复回放历史窗口。
     */
    @Test
    void shouldBuildTailFollowCommandFromCurrentTime() {
        SshCommandBuilder builder = new SshCommandBuilder();
        String command = builder.buildTailFollowCommand("/data/logs/app.log");
        Assertions.assertTrue(command.contains("tail -n 0 -F"));
        Assertions.assertTrue(command.contains("||"));
        Assertions.assertTrue(command.contains("tail -n 0 -f"));
    }

    /**
     * 校验 cat grep 预筛命令会基于包含规则拼接 grep 条件。
     */
    @Test
    void shouldBuildCatGrepPrefilterCommand() {
        SshCommandBuilder builder = new SshCommandBuilder();

        LogStreamRequest.FilterRule include = new LogStreamRequest.FilterRule();
        include.setType(LogStreamRequest.FILTER_TYPE_INCLUDE);
        include.setData("ERROR&&orderId||timeout");

        LogStreamRequest.FilterRule exclude = new LogStreamRequest.FilterRule();
        exclude.setType(LogStreamRequest.FILTER_TYPE_EXCLUDE);
        exclude.setData("DEBUG");

        String command = builder.buildCatGrepPrefilterCommand("/data/logs/app.log", Arrays.asList(include, exclude));
        Assertions.assertTrue(command.contains("if [ -f"));
        Assertions.assertTrue(command.contains("cat '/data/logs/app.log'"));
        Assertions.assertTrue(command.contains("grep -a -F"));
        Assertions.assertTrue(command.contains("-e 'ERROR'"));
        Assertions.assertTrue(command.contains("-e 'orderId'"));
        Assertions.assertTrue(command.contains("-e 'timeout'"));
        Assertions.assertFalse(command.contains("-e 'DEBUG'"));
    }

    /**
     * 校验 tail grep 预筛命令从当前时刻追踪并拼接包含关键词。
     */
    @Test
    void shouldBuildTailFollowGrepPrefilterCommand() {
        SshCommandBuilder builder = new SshCommandBuilder();

        LogStreamRequest.FilterRule include = new LogStreamRequest.FilterRule();
        include.setType(LogStreamRequest.FILTER_TYPE_INCLUDE);
        include.setData("ERROR&&orderId||timeout");

        LogStreamRequest.FilterRule exclude = new LogStreamRequest.FilterRule();
        exclude.setType(LogStreamRequest.FILTER_TYPE_EXCLUDE);
        exclude.setData("DEBUG");

        String command = builder.buildTailFollowGrepPrefilterCommand(
                "/data/logs/app.log", Arrays.asList(include, exclude));
        Assertions.assertTrue(command.contains("tail -n 0 -F"));
        Assertions.assertTrue(command.contains("tail -n 0 -f"));
        Assertions.assertTrue(command.contains("grep -a -F"));
        Assertions.assertTrue(command.contains("-e 'ERROR'"));
        Assertions.assertTrue(command.contains("-e 'orderId'"));
        Assertions.assertTrue(command.contains("-e 'timeout'"));
        Assertions.assertFalse(command.contains("-e 'DEBUG'"));
    }

    /**
     * 校验磁盘指标采集命令包含根分区采集与百分比处理逻辑。
     */
    /**
     * 校验 cat 上下文窗口命令会基于完整过滤规则生成 awk 命中条件，
     * 先识别 include && !exclude 的命中行，再围绕命中行输出原始上下文。
     */
    @Test
    void shouldBuildCatContextWindowCommandWithFullRuleSemantics() {
        SshCommandBuilder builder = new SshCommandBuilder();

        LogStreamRequest.FilterRule include = new LogStreamRequest.FilterRule();
        include.setType(LogStreamRequest.FILTER_TYPE_INCLUDE);
        include.setData("ERROR&&orderId");

        LogStreamRequest.FilterRule exclude = new LogStreamRequest.FilterRule();
        exclude.setType(LogStreamRequest.FILTER_TYPE_EXCLUDE);
        exclude.setData("DEBUG");

        String command = builder.buildCatContextWindowCommand(
                "/data/logs/app.log", Arrays.asList(include, exclude), 10, 10, 200);
        Assertions.assertTrue(command.contains("awk -v B=10 -v A=10 -v CAP=200"));
        Assertions.assertTrue(command.contains("index($0,\"ERROR\")>0"));
        Assertions.assertTrue(command.contains("index($0,\"orderId\")>0"));
        Assertions.assertTrue(command.contains("index($0,\"DEBUG\")>0"));
        Assertions.assertTrue(command.contains("&& !("));
        Assertions.assertTrue(command.contains("cat '/data/logs/app.log' 2>&1"));
    }

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
