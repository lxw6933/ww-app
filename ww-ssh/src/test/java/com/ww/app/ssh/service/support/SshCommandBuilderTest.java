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
}
