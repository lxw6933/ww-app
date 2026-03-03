package com.ww.app.ssh.service.support;

import org.springframework.stereotype.Component;

/**
 * SSH 命令构建器。
 * <p>
 * 统一管理日志面板依赖的远程 Shell 命令拼装逻辑，避免命令字符串散落在业务服务中。
 * </p>
 */
@Component
public class SshCommandBuilder {

    /**
     * 构建日志文件列表命令。
     *
     * @param logPath 日志目录或日志文件
     * @return Shell 命令
     */
    public String buildListFilesCommand(String logPath) {
        String quoted = shellQuote(logPath);
        return "if [ -f " + quoted + " ]; then echo " + quoted
                + "; elif [ -d " + quoted + " ]; then find " + quoted
                + " -maxdepth 1 -type f \\( -name '*.log' -o -name '*.out' -o -name '*.txt' \\) | sort; fi";
    }

    /**
     * 构建最新日志文件发现命令。
     *
     * @param logPath 日志目录或日志文件
     * @return Shell 命令
     */
    public String buildLatestFileCommand(String logPath) {
        String quoted = shellQuote(logPath);
        return "if [ -f " + quoted + " ]; then echo " + quoted
                + "; elif [ -d " + quoted + " ]; then ls -1t " + quoted
                + "/*.log " + quoted + "/*.out " + quoted + "/*.txt 2>/dev/null | head -n 1; fi";
    }

    /**
     * 构建 tail 实时读取命令。
     * <p>
     * 使用 "-F" 优先支持日志滚动追踪，若目标环境不支持 "-F"（如部分 BusyBox），
     * 自动回退到 "-f" 以保证至少具备实时监听能力。
     * </p>
     *
     * @param filePath 日志文件路径
     * @param lines    回看行数
     * @return Shell 命令
     */
    public String buildTailCommand(String filePath, int lines) {
        String quotedPath = shellQuote(filePath);
        String commandWithF = "tail -n " + lines + " -F " + quotedPath + " 2>&1";
        String commandWithf = "tail -n " + lines + " -f " + quotedPath + " 2>&1";
        return "(" + commandWithF + " || " + commandWithf + ")";
    }

    /**
     * Shell 单引号安全转义。
     *
     * @param raw 原始字符串
     * @return 可安全拼接到 Shell 命令中的字符串
     */
    private String shellQuote(String raw) {
        return "'" + raw.replace("'", "'\"'\"'") + "'";
    }
}
