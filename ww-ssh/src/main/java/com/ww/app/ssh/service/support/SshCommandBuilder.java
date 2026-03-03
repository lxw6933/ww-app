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
     * 构建 CPU 使用率采集命令。
     * <p>
     * 优先尝试通过 top 输出解析空闲率并换算为使用率；
     * 若 top 不可用或解析失败，回退到 /proc/stat 的“累计使用率”近似值。
     * </p>
     *
     * @return Shell 命令
     */
    public String buildCpuUsageCommand() {
        return "CPU=$(top -bn1 2>/dev/null | awk -F',' '/Cpu|CPU:/ {"
                + "for(i=1;i<=NF;i++){if($i ~ /id/){gsub(/[^0-9.]/,\"\",$i);"
                + "if($i!=\"\"){printf \"%.2f\",100-$i; exit}}}}');"
                + "if [ -n \"$CPU\" ]; then echo $CPU; "
                + "else awk '/^cpu / {idle=$5; total=0; for(i=2;i<=NF;i++){total+=$i}; "
                + "if(total>0){printf \"%.2f\", (total-idle)*100/total} else {print \"0\"}}' /proc/stat; fi";
    }

    /**
     * 构建内存指标采集命令。
     * <p>
     * 输出格式：使用率(%) 已使用(MB) 总量(MB)。
     * </p>
     *
     * @return Shell 命令
     */
    public String buildMemoryUsageCommand() {
        return "awk '/MemTotal:/ {t=$2} /MemAvailable:/ {a=$2} "
                + "END {if(t>0){u=t-a; printf \"%.2f %d %d\", (u*100/t), int(u/1024), int(t/1024)}}' /proc/meminfo";
    }

    /**
     * 构建负载采集命令。
     * <p>
     * 输出格式：1m 5m 15m。
     * </p>
     *
     * @return Shell 命令
     */
    public String buildLoadAverageCommand() {
        return "cat /proc/loadavg 2>/dev/null | awk '{print $1\" \"$2\" \"$3}'";
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
