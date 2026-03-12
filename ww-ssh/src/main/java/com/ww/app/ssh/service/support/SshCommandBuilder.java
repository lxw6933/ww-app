package com.ww.app.ssh.service.support;

import com.ww.app.ssh.model.InstanceOperationRequest;
import com.ww.app.ssh.model.LogStreamRequest;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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
                + " -maxdepth 1 -type f \\( "
                + "-name '*.log' -o -name '*.log.*' "
                + "-o -name '*.out' -o -name '*.out.*' "
                + "-o -name '*.txt' -o -name '*.txt.*' "
                + "\\) | sort; fi";
    }

    /**
     * 构建最新日志文件发现命令。
     * <p>
     * 选择策略：
     * 1. 若目录下存在文件名包含当天日期（yyyy-MM-dd）的日志，优先返回该类文件中最近修改的一个；<br>
     * 2. 若当天文件尚未生成，则回退到目录内最近修改的日志文件。<br>
     * 通过该策略可兼容按天滚动日志在跨日或延迟生成场景下的默认读取行为。
     * </p>
     *
     * @param logPath 日志目录或日志文件
     * @return Shell 命令
     */
    public String buildLatestFileCommand(String logPath) {
        String quoted = shellQuote(logPath);
        return "if [ -f " + quoted + " ]; then echo " + quoted
                + "; elif [ -d " + quoted + " ]; then "
                + "TODAY=$(date +%F 2>/dev/null); "
                + "LATEST_LIST=$(ls -1t "
                + quoted + "/*.log " + quoted + "/*.log.* "
                + quoted + "/*.out " + quoted + "/*.out.* "
                + quoted + "/*.txt " + quoted + "/*.txt.* "
                + "2>/dev/null); "
                + "if [ -n \"$TODAY\" ]; then "
                + "TODAY_FILE=$(printf \"%s\\n\" \"$LATEST_LIST\" | awk -v d=\"$TODAY\" 'index($0,d)>0 {print; exit}'); "
                + "if [ -n \"$TODAY_FILE\" ]; then "
                + "echo \"$TODAY_FILE\"; "
                + "else "
                + "printf \"%s\\n\" \"$LATEST_LIST\" | head -n 1; "
                + "fi; "
                + "else "
                + "printf \"%s\\n\" \"$LATEST_LIST\" | head -n 1; "
                + "fi; "
                + "fi";
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
     * 构建 cat 快照读取命令（一次性输出并结束）。
     * <p>
     * 为了避免读取超大文件造成阻塞，实际采用 {@code tail -n} 截取最新 N 行，
     * 语义上对应“cat 快照模式”的一次性读取。
     * </p>
     *
     * @param filePath 日志文件路径
     * @param lines    回看行数
     * @return Shell 命令
     */
    public String buildCatCommand(String filePath, int lines) {
        String quotedPath = shellQuote(filePath);
        return "tail -n " + lines + " " + quotedPath + " 2>&1";
    }

    /**
     * 构建 cat 模式 grep 预筛命令（全文件扫描）。
     * <p>
     * 该命令只应用“包含规则”的必要条件，避免遗漏真实命中；
     * 复杂表达式的最终精确判定由服务层 matcher 完成。
     * </p>
     *
     * @param filePath    日志文件路径
     * @param filterRules 过滤规则
     * @return Shell 命令
     */
    public String buildCatGrepPrefilterCommand(String filePath, List<LogStreamRequest.FilterRule> filterRules) {
        String quotedPath = shellQuote(filePath);
        StringBuilder command = new StringBuilder("cat " + quotedPath);
        List<List<String>> includeRuleTerms = resolveIncludeRuleTerms(filterRules);
        for (List<String> terms : includeRuleTerms) {
            if (terms == null || terms.isEmpty()) {
                continue;
            }
            command.append(" | grep -a -F");
            for (String term : terms) {
                command.append(" -e ").append(shellQuote(term));
            }
        }
        String fallback = "cat " + quotedPath + " 2>&1";
        return "if [ -f " + quotedPath + " ]; then " + command + " 2>&1; else " + fallback + "; fi";
    }

    /**
     * 构建 tail 模式 grep 预筛追踪命令（仅追踪新增内容）。
     * <p>
     * 该命令从“当前时刻”开始跟踪日志，并基于包含规则做远端预筛，
     * 以避免重复回放历史窗口及降低网络回传量。
     * </p>
     *
     * @param filePath    日志文件路径
     * @param filterRules 过滤规则
     * @return Shell 命令
     */
    public String buildTailFollowGrepPrefilterCommand(String filePath,
                                                       List<LogStreamRequest.FilterRule> filterRules) {
        String quotedPath = shellQuote(filePath);
        String commandWithF = "tail -n 0 -F " + quotedPath + " 2>&1";
        String commandWithf = "tail -n 0 -f " + quotedPath + " 2>&1";
        StringBuilder command = new StringBuilder("(" + commandWithF + " || " + commandWithf + ")");
        List<List<String>> includeRuleTerms = resolveIncludeRuleTerms(filterRules);
        for (List<String> terms : includeRuleTerms) {
            if (terms == null || terms.isEmpty()) {
                continue;
            }
            command.append(" | grep -a -F");
            for (String term : terms) {
                command.append(" -e ").append(shellQuote(term));
            }
        }
        return command.toString();
    }

    /**
     * 构建实例启停管理命令。
     * <p>
     * 命令策略：
     * 1. 若脚本可执行，直接执行：{@code 脚本 动作}；<br>
     * 2. 若脚本不可执行，回退为：{@code sh 脚本 动作}；<br>
     * 3. 标准错误合并到标准输出，便于统一回传。
     * </p>
     *
     * @param commandFile 脚本路径
     * @param action      动作（start/restart/stop）
     * @return Shell 命令
     */
    public String buildInstanceOperationCommand(String commandFile, String action) {
        String normalizedAction = action == null ? "" : action.trim().toLowerCase();
        if (!InstanceOperationRequest.ACTION_START.equals(normalizedAction)
                && !InstanceOperationRequest.ACTION_RESTART.equals(normalizedAction)
                && !InstanceOperationRequest.ACTION_STOP.equals(normalizedAction)) {
            throw new IllegalArgumentException("不支持的实例动作: " + action);
        }
        return buildManagedCommand(commandFile, normalizedAction);
    }

    /**
     * 构建实例状态探测命令。
     * <p>
     * 兼容两类场景：
     * 1. 命令前缀模式（如 {@code sh server.sh}）：尝试执行 {@code status}；<br>
     * 2. 脚本路径模式（如 {@code /data/app/bin/server.sh}）：
     * 基于 {@code pid/*.pid} 与进程名探测运行态，不依赖脚本必须实现 status。
     * </p>
     *
     * @param commandFile 脚本路径
     * @return Shell 命令
     */
    public String buildInstanceStatusCommand(String commandFile) {
        String normalizedCommand = commandFile == null ? "" : commandFile.trim();
        if (normalizedCommand.isEmpty()) {
            throw new IllegalArgumentException("manageCommandFile 不能为空");
        }
        if (looksLikeCommandPrefix(normalizedCommand)) {
            String scriptPath = extractScriptPathFromPrefix(normalizedCommand);
            if (!scriptPath.isEmpty()) {
                return buildInstanceStatusCommand(scriptPath);
            }
            return buildManagedCommand(commandFile, "status");
        }
        String quotedFile = shellQuote(normalizedCommand);
        return buildProfileBootstrapCommand()
                + "CMD_FILE=" + quotedFile + "; "
                + "CMD_DIR=$(dirname \"$CMD_FILE\"); "
                + "cd \"$CMD_DIR\" 2>/dev/null || { "
                + "echo \"状态探测失败: 脚本目录不存在 $CMD_DIR\"; "
                + "echo __WW_INSTANCE_EXIT__:98; "
                + "exit; }; "
                + "APP_HOME=$(dirname \"$CMD_DIR\"); "
                + "SERVICE_GUESS=$(basename \"$APP_HOME\"); "
                + "CMD_BASE=$(basename \"$CMD_FILE\"); "
                + "if [ -f \"./$CMD_BASE\" ]; then "
                + "SCRIPT_SERVICE=$(awk -F'=' '/^[[:space:]]*SERVICE_NAME=/{gsub(/[\"'\"'\"'[:space:]]/,\"\",$2); "
                + "if($2!=\"\"){print $2; exit}}' \"./$CMD_BASE\" 2>/dev/null); "
                + "if [ -n \"$SCRIPT_SERVICE\" ]; then SERVICE_GUESS=\"$SCRIPT_SERVICE\"; fi; "
                + "fi; "
                + "HAS_PID_FILE=0; "
                + "LAST_PID_FILE=''; "
                + "LAST_PID=''; "
                + "if [ -d \"./pid\" ]; then "
                + "for PID_FILE in ./pid/*.pid; do "
                + "[ -f \"$PID_FILE\" ] || continue; "
                + "HAS_PID_FILE=1; "
                + "PID=$(tr -cd '0-9' < \"$PID_FILE\"); "
                + "[ -n \"$PID\" ] || continue; "
                + "if kill -0 \"$PID\" 2>/dev/null; then "
                + "echo \"running: pid文件命中 $PID_FILE (pid=$PID)\"; "
                + "echo __WW_INSTANCE_EXIT__:0; "
                + "exit; "
                + "fi; "
                + "if command -v ps >/dev/null 2>&1 && ps -p \"$PID\" >/dev/null 2>&1; then "
                + "echo \"running: ps命中进程号 $PID\"; "
                + "echo __WW_INSTANCE_EXIT__:0; "
                + "exit; "
                + "fi; "
                + "if [ -d \"/proc/$PID\" ]; then "
                + "echo \"running: /proc命中进程号 $PID\"; "
                + "echo __WW_INSTANCE_EXIT__:0; "
                + "exit; "
                + "fi; "
                + "LAST_PID_FILE=\"$PID_FILE\"; "
                + "LAST_PID=\"$PID\"; "
                + "done; "
                + "fi; "
                + "if command -v ps >/dev/null 2>&1 && [ -n \"$SERVICE_GUESS\" ]; then "
                + "JAVA_PID=$(ps -eo pid=,args= 2>/dev/null "
                + "| awk -v svc=\"$SERVICE_GUESS\" -v self=\"$$\" "
                + "'$1!=self && $0 ~ /[j]ava/ && index($0, svc) > 0 {print $1; exit}'); "
                + "if [ -n \"$JAVA_PID\" ]; then "
                + "echo \"running: java进程命中 $SERVICE_GUESS (pid=$JAVA_PID)\"; "
                + "echo __WW_INSTANCE_EXIT__:0; "
                + "exit; "
                + "fi; "
                + "fi; "
                + "if [ \"$HAS_PID_FILE\" -eq 1 ]; then "
                + "echo \"stopped: 存在陈旧pid文件(进程不存在) $LAST_PID_FILE $LAST_PID\"; "
                + "echo __WW_INSTANCE_EXIT__:3; "
                + "exit; "
                + "fi; "
                + "echo \"stopped: 未发现有效pid或进程\"; "
                + "echo __WW_INSTANCE_EXIT__:3;";
    }

    /**
     * 构建实例管理命令（统一用于 start/restart/stop/status）。
     * <p>
     * 兼容两类配置：
     * 1. 脚本路径：如 {@code /data/app/server.sh}；<br>
     * 2. 命令前缀：如 {@code sh server.sh} 或 {@code bash /data/app/server.sh}。<br>
     * 第二类会将动作参数直接追加到配置尾部执行。
     * </p>
     *
     * @param commandFile 运维脚本路径或命令前缀
     * @param action      动作参数
     * @return Shell 命令
     */
    private String buildManagedCommand(String commandFile, String action) {
        String normalizedCommand = commandFile == null ? "" : commandFile.trim();
        if (normalizedCommand.isEmpty()) {
            throw new IllegalArgumentException("manageCommandFile 不能为空");
        }
        String quotedAction = shellQuote(action);
        if (looksLikeCommandPrefix(normalizedCommand)) {
            String quotedPrefix = shellQuote(normalizedCommand);
            return buildProfileBootstrapCommand()
                    + "CMD_PREFIX=" + quotedPrefix + "; "
                    + "CMD_ACTION=" + quotedAction + "; "
                    + "eval \"$CMD_PREFIX $CMD_ACTION\" 2>&1; "
                    + "echo __WW_INSTANCE_EXIT__:$?";
        }
        String quotedFile = shellQuote(normalizedCommand);
        return buildProfileBootstrapCommand()
                + "CMD_ACTION=" + quotedAction + "; "
                + "CMD_FILE=" + quotedFile + "; "
                + "CMD_DIR=$(dirname \"$CMD_FILE\"); "
                + "CMD_BASE=$(basename \"$CMD_FILE\"); "
                + "cd \"$CMD_DIR\" 2>/dev/null || { "
                + "echo \"脚本目录不存在: $CMD_DIR\"; "
                + "echo __WW_INSTANCE_EXIT__:98; "
                + "exit; }; "
                + "APP_HOME=$(dirname \"$CMD_DIR\"); "
                + "SERVICE_GUESS=$(basename \"$APP_HOME\"); "
                + "if [ -f \"./$CMD_BASE\" ]; then "
                + "SCRIPT_SERVICE=$(awk -F'=' '/^[[:space:]]*SERVICE_NAME=/{gsub(/[\"'\"'\"'[:space:]]/,\"\",$2); "
                + "if($2!=\"\"){print $2; exit}}' \"./$CMD_BASE\" 2>/dev/null); "
                + "if [ -n \"$SCRIPT_SERVICE\" ]; then SERVICE_GUESS=\"$SCRIPT_SERVICE\"; fi; "
                + "fi; "
                + "detect_java_pids_by_service() { "
                + "if ! command -v ps >/dev/null 2>&1 || [ -z \"$SERVICE_GUESS\" ]; then "
                + "return; "
                + "fi; "
                + "ps -eo pid=,args= 2>/dev/null "
                + "| awk -v svc=\"$SERVICE_GUESS\" -v self=\"$$\" "
                + "'$1!=self && $0 ~ /[j]ava/ && index($0, svc) > 0 {print $1}'; "
                + "}; "
                + "RC=1; "
                + "if command -v sh >/dev/null 2>&1; then "
                + "sh \"./$CMD_BASE\" " + quotedAction + " 2>&1; RC=$?; "
                + "fi; "
                + "if [ \"$RC\" -ne 0 ] && [ -x \"./$CMD_BASE\" ]; then "
                + "./\"$CMD_BASE\" " + quotedAction + " 2>&1; RC=$?; "
                + "fi; "
                + "if [ \"$RC\" -ne 0 ] && command -v bash >/dev/null 2>&1; then "
                + "bash \"./$CMD_BASE\" " + quotedAction + " 2>&1; RC=$?; "
                + "fi; "
                + "if [ \"$CMD_ACTION\" = \"stop\" ] && [ \"$RC\" -eq 0 ]; then "
                + "RUNNING=0; "
                + "if [ -d \"./pid\" ]; then "
                + "for PID_FILE in ./pid/*.pid; do "
                + "[ -f \"$PID_FILE\" ] || continue; "
                + "PID=$(tr -cd '0-9' < \"$PID_FILE\"); "
                + "[ -n \"$PID\" ] || continue; "
                + "if kill -0 \"$PID\" 2>/dev/null; then RUNNING=1; break; fi; "
                + "if command -v ps >/dev/null 2>&1 && ps -p \"$PID\" >/dev/null 2>&1; then RUNNING=1; break; fi; "
                + "if [ -d \"/proc/$PID\" ]; then RUNNING=1; break; fi; "
                + "done; "
                + "fi; "
                + "if [ \"$RUNNING\" -eq 0 ] && command -v ps >/dev/null 2>&1 "
                + "&& [ -n \"$SERVICE_GUESS\" ] "
                + "; then "
                + "JAVA_PIDS=$(detect_java_pids_by_service); "
                + "if [ -n \"$JAVA_PIDS\" ]; then RUNNING=1; fi; "
                + "fi; "
                + "if [ \"$RUNNING\" -eq 1 ] && command -v ps >/dev/null 2>&1 && [ -n \"$SERVICE_GUESS\" ]; then "
                + "PIDS=$(detect_java_pids_by_service); "
                + "if [ -n \"$PIDS\" ]; then "
                + "for PID in $PIDS; do kill \"$PID\" 2>/dev/null; done; "
                + "sleep 1; "
                + "for PID in $PIDS; do kill -0 \"$PID\" 2>/dev/null && kill -9 \"$PID\" 2>/dev/null; done; "
                + "echo \"fallback-stop: 已按服务名尝试停止 $SERVICE_GUESS\"; "
                + "fi; "
                + "fi; "
                + "RUNNING=0; "
                + "if [ -d \"./pid\" ]; then "
                + "for PID_FILE in ./pid/*.pid; do "
                + "[ -f \"$PID_FILE\" ] || continue; "
                + "PID=$(tr -cd '0-9' < \"$PID_FILE\"); "
                + "[ -n \"$PID\" ] || continue; "
                + "if kill -0 \"$PID\" 2>/dev/null; then RUNNING=1; break; fi; "
                + "if command -v ps >/dev/null 2>&1 && ps -p \"$PID\" >/dev/null 2>&1; then RUNNING=1; break; fi; "
                + "if [ -d \"/proc/$PID\" ]; then RUNNING=1; break; fi; "
                + "done; "
                + "fi; "
                + "if [ \"$RUNNING\" -eq 0 ] && command -v ps >/dev/null 2>&1 "
                + "&& [ -n \"$SERVICE_GUESS\" ] "
                + "; then "
                + "JAVA_PIDS=$(detect_java_pids_by_service); "
                + "if [ -n \"$JAVA_PIDS\" ]; then RUNNING=1; fi; "
                + "fi; "
                + "if [ \"$RUNNING\" -eq 1 ]; then "
                + "RC=5; "
                + "echo \"停止失败: 兜底停止后仍检测到进程运行 $SERVICE_GUESS\"; "
                + "fi; "
                + "fi; "
                + "if [ \"$RC\" -eq 0 ] "
                + "&& { [ \"$CMD_ACTION\" = \"start\" ] || [ \"$CMD_ACTION\" = \"restart\" ]; }; then "
                + "RUNNING=0; "
                + "CHECK_COUNT=0; "
                + "while [ \"$CHECK_COUNT\" -lt 15 ]; do "
                + "RUNNING=0; "
                + "if [ -d \"./pid\" ]; then "
                + "for PID_FILE in ./pid/*.pid; do "
                + "[ -f \"$PID_FILE\" ] || continue; "
                + "PID=$(tr -cd '0-9' < \"$PID_FILE\"); "
                + "[ -n \"$PID\" ] || continue; "
                + "if kill -0 \"$PID\" 2>/dev/null; then RUNNING=1; break; fi; "
                + "if command -v ps >/dev/null 2>&1 && ps -p \"$PID\" >/dev/null 2>&1; then RUNNING=1; break; fi; "
                + "if [ -d \"/proc/$PID\" ]; then RUNNING=1; break; fi; "
                + "done; "
                + "fi; "
                + "if [ \"$RUNNING\" -eq 0 ] && command -v ps >/dev/null 2>&1 "
                + "&& [ -n \"$SERVICE_GUESS\" ]; then "
                + "JAVA_PIDS=$(detect_java_pids_by_service); "
                + "if [ -n \"$JAVA_PIDS\" ]; then RUNNING=1; fi; "
                + "fi; "
                + "if [ \"$RUNNING\" -eq 1 ]; then break; fi; "
                + "CHECK_COUNT=$((CHECK_COUNT+1)); "
                + "sleep 1; "
                + "done; "
                + "if [ \"$RUNNING\" -eq 0 ]; then "
                + "RC=6; "
                + "echo \"启动校验失败: 未检测到java进程 $SERVICE_GUESS\"; "
                + "fi; "
                + "fi; "
                + "echo __WW_INSTANCE_EXIT__:$RC";
    }

    /**
     * 判断配置是否更像“命令前缀”而非“脚本路径”。
     *
     * @param commandFile 配置值
     * @return true 表示命令前缀模式
     */
    private boolean looksLikeCommandPrefix(String commandFile) {
        String normalized = commandFile == null ? "" : commandFile.trim().toLowerCase();
        if (normalized.isEmpty()) {
            return false;
        }
        return normalized.startsWith("sh ")
                || normalized.startsWith("bash ")
                || normalized.startsWith("/bin/sh ")
                || normalized.startsWith("/bin/bash ")
                || normalized.contains("&&")
                || normalized.contains("||")
                || normalized.contains(";")
                || normalized.contains("|");
    }

    /**
     * 从命令前缀中提取脚本路径。
     * <p>
     * 例如：{@code sh /data/app/bin/server.sh}、{@code bash ./server.sh}。
     * </p>
     *
     * @param commandPrefix 命令前缀
     * @return 脚本路径，未识别返回空字符串
     */
    private String extractScriptPathFromPrefix(String commandPrefix) {
        String normalized = commandPrefix == null ? "" : commandPrefix.trim();
        if (normalized.isEmpty()) {
            return "";
        }
        String[] tokens = normalized.split("\\s+");
        for (String token : tokens) {
            String candidate = token == null ? "" : token.trim();
            if (candidate.isEmpty()) {
                continue;
            }
            if ((candidate.startsWith("\"") && candidate.endsWith("\""))
                    || (candidate.startsWith("'") && candidate.endsWith("'"))) {
                candidate = candidate.substring(1, candidate.length() - 1).trim();
            }
            if (candidate.isEmpty()) {
                continue;
            }
            String lower = candidate.toLowerCase();
            if ("sh".equals(lower) || "bash".equals(lower)
                    || "/bin/sh".equals(lower) || "/bin/bash".equals(lower)) {
                continue;
            }
            if (lower.endsWith(".sh")) {
                return candidate;
            }
        }
        return "";
    }

    /**
     * 构建环境变量预加载命令。
     * <p>
     * 启动/重启通常依赖 JAVA_HOME、PATH 等变量，
     * 该段命令会尽量加载常见 profile 文件。
     * </p>
     *
     * @return Shell 片段
     */
    private String buildProfileBootstrapCommand() {
        return "[ -f /etc/profile ] && . /etc/profile >/dev/null 2>&1; "
                + "[ -f ~/.bash_profile ] && . ~/.bash_profile >/dev/null 2>&1; "
                + "[ -f ~/.profile ] && . ~/.profile >/dev/null 2>&1; "
                + "[ -f ~/.bashrc ] && . ~/.bashrc >/dev/null 2>&1; ";
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
     * 构建交换内存指标采集命令。
     * <p>
     * 输出格式：使用率(%) 已使用(MB) 总量(MB)。
     * 若机器未启用 swap（SwapTotal=0），固定输出 {@code 0 0 0}。
     * </p>
     *
     * @return Shell 命令
     */
    public String buildSwapUsageCommand() {
        return "awk '/SwapTotal:/ {t=$2} /SwapFree:/ {f=$2} "
                + "END {if(t>0){u=t-f; printf \"%.2f %d %d\", (u*100/t), int(u/1024), int(t/1024)} "
                + "else {printf \"0 0 0\"}}' /proc/meminfo";
    }

    /**
     * 构建磁盘容量指标采集命令。
     * <p>
     * 输出格式：使用率(%) 已使用(MB) 总量(MB)。
     * 默认采集根分区 {@code /}，用于反映服务所在主机的整体磁盘压力。
     * </p>
     *
     * @return Shell 命令
     */
    public String buildDiskUsageCommand() {
        return "df -Pm / 2>/dev/null | awk 'NR==2 {gsub(/%/,\"\",$5); printf \"%s %s %s\", $5, $3, $2}'";
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
     * 构建合并主机指标采集命令（CPU + 内存 + 交换 + 磁盘 + 负载，一次 SSH 完成）。
     * <p>
     * 将原本需要5次 SSH 连接的指标采集合并为单次命令，输出格式每行一个指标：
     * <pre>
     * WW_CPU:85.23
     * WW_MEM:65.12 8192 12288
     * WW_SWAP:0 0 0
     * WW_DISK:45.00 50000 110000
     * WW_LOAD:0.80 0.90 1.00
     * </pre>
     * 上层通过行前缀解析各字段，保持与原单条命令输出格式完全一致。
     * </p>
     *
     * @return Shell 命令
     */
    public String buildCombinedHostMetricsCommand() {
        // CPU：优先 top 解析空闲率，失败则回退 /proc/stat
        String cpuCapture = "_CPU=$(top -bn1 2>/dev/null"
                + " | awk -F',' '/Cpu|CPU:/ {for(i=1;i<=NF;i++){if($i~/id/)"
                + "{gsub(/[^0-9.]/,\"\",$i);if($i!=\"\"){printf \"%.2f\",100-$i;exit}}}}');"
                + " [ -n \"$_CPU\" ] || _CPU=$(awk '/^cpu /{idle=$5;total=0;"
                + "for(i=2;i<=NF;i++){total+=$i};"
                + "if(total>0){printf \"%.2f\",(total-idle)*100/total}else{print \"0\"}}"
                + "' /proc/stat 2>/dev/null);";
        // 内存：格式 使用率% 已用MB 总MB
        String memCapture = "_MEM=$(awk '/MemTotal:/{t=$2}/MemAvailable:/{a=$2}"
                + "END{if(t>0){u=t-a;printf \"%.2f %d %d\",(u*100/t),int(u/1024),int(t/1024)}}'"
                + " /proc/meminfo 2>/dev/null);";
        // 交换内存：格式 使用率% 已用MB 总MB
        String swapCapture = "_SWAP=$(awk '/SwapTotal:/{t=$2}/SwapFree:/{f=$2}"
                + "END{if(t>0){u=t-f;printf \"%.2f %d %d\",(u*100/t),int(u/1024),int(t/1024)}"
                + "else{printf \"0 0 0\"}}' /proc/meminfo 2>/dev/null);";
        // 磁盘（根分区）：格式 使用率% 已用MB 总MB
        String diskCapture = "_DISK=$(df -Pm / 2>/dev/null"
                + " | awk 'NR==2{gsub(/%/,\"\",$5);printf \"%s %s %s\",$5,$3,$2}');";
        // 系统负载：格式 1m 5m 15m
        String loadCapture = "_LOAD=$(awk '{print $1\" \"$2\" \"$3}' /proc/loadavg 2>/dev/null);";
        // 输出带前缀的多行结果
        String printAll = "printf 'WW_CPU:%s\\nWW_MEM:%s\\nWW_SWAP:%s\\nWW_DISK:%s\\nWW_LOAD:%s\\n'"
                + " \"$_CPU\" \"$_MEM\" \"$_SWAP\" \"$_DISK\" \"$_LOAD\"";
        return cpuCapture + " " + memCapture + " " + swapCapture + " "
                + diskCapture + " " + loadCapture + " " + printAll;
    }

    /**
     * 构建 JVM GC 指标采集命令。
     * <p>
     * 采集策略：
     * 1. 先根据 manageCommandFile 与 serviceKey 推断服务识别词；<br>
     * 2. 优先从脚本目录与应用目录的 pid 文件识别 Java 进程 PID；<br>
     * 3. pid 文件未命中时，回退到 ps/jps 进行进程匹配；<br>
     * 4. 使用 jstat -gc 读取单次快照（含堆详细容量/占用）；<br>
     * 5. 统一输出带标记结果，便于上层稳定解析。
     * </p>
     *
     * @param manageCommandFile 实例运维脚本配置（可为空）
     * @param serviceKey        实例服务键（如 mall-basic@node1）
     * @return Shell 命令
     */
    public String buildJvmGcStatsCommand(String manageCommandFile, String serviceKey) {
        String normalizedServiceKey = serviceKey == null ? "" : serviceKey.trim();
        String serviceGroup = normalizeServiceGroupName(normalizedServiceKey);
        String normalizedManageCommand = manageCommandFile == null ? "" : manageCommandFile.trim();
        String scriptPath = "";
        if (!normalizedManageCommand.isEmpty()) {
            if (looksLikeCommandPrefix(normalizedManageCommand)) {
                scriptPath = extractScriptPathFromPrefix(normalizedManageCommand);
            } else {
                scriptPath = normalizedManageCommand;
            }
        }
        return buildProfileBootstrapCommand()
                + "SERVICE_KEY=" + shellQuote(normalizedServiceKey) + "; "
                + "SERVICE_GROUP=" + shellQuote(serviceGroup) + "; "
                + "SCRIPT_PATH=" + shellQuote(scriptPath) + "; "
                + "SCRIPT_DIR=''; "
                + "APP_HOME=''; "
                + "SERVICE_GUESS=\"$SERVICE_GROUP\"; "
                + "if [ -n \"$SCRIPT_PATH\" ]; then "
                + "SCRIPT_DIR=$(dirname \"$SCRIPT_PATH\"); "
                + "if [ -d \"$SCRIPT_DIR\" ]; then "
                + "APP_HOME=$(dirname \"$SCRIPT_DIR\"); "
                + "SERVICE_FROM_DIR=$(basename \"$APP_HOME\"); "
                + "if [ -n \"$SERVICE_FROM_DIR\" ]; then SERVICE_GUESS=\"$SERVICE_FROM_DIR\"; fi; "
                + "SCRIPT_BASE=$(basename \"$SCRIPT_PATH\"); "
                + "if [ -f \"$SCRIPT_DIR/$SCRIPT_BASE\" ]; then "
                + "SCRIPT_SERVICE=$(awk -F'=' '/^[[:space:]]*SERVICE_NAME=/{gsub(/[\"'\"'\"'[:space:]]/,\"\",$2); "
                + "if($2!=\"\"){print $2; exit}}' \"$SCRIPT_DIR/$SCRIPT_BASE\" 2>/dev/null); "
                + "if [ -n \"$SCRIPT_SERVICE\" ]; then SERVICE_GUESS=\"$SCRIPT_SERVICE\"; fi; "
                + "fi; "
                + "fi; "
                + "fi; "
                + "if [ -z \"$SERVICE_GUESS\" ]; then SERVICE_GUESS=\"$SERVICE_KEY\"; fi; "
                + "if ! command -v ps >/dev/null 2>&1; then echo __WW_JVM_GC__:NO_PS; exit; fi; "
                + "JAVA_PID=''; "
                + "collect_pid_from_file() { "
                + "PID_FILE=\"$1\"; "
                + "[ -f \"$PID_FILE\" ] || return; "
                + "PID_VAL=$(tr -cd '0-9' < \"$PID_FILE\"); "
                + "[ -n \"$PID_VAL\" ] || return; "
                + "if kill -0 \"$PID_VAL\" 2>/dev/null || ps -p \"$PID_VAL\" >/dev/null 2>&1 || [ -d \"/proc/$PID_VAL\" ]; then "
                + "JAVA_PID=\"$PID_VAL\"; "
                + "fi; "
                + "}; "
                + "if [ -n \"$SCRIPT_DIR\" ]; then "
                + "for PID_FILE in \"$SCRIPT_DIR\"/pid/*.pid; do "
                + "[ -f \"$PID_FILE\" ] || continue; "
                + "collect_pid_from_file \"$PID_FILE\"; "
                + "[ -n \"$JAVA_PID\" ] && break; "
                + "done; "
                + "fi; "
                + "if [ -z \"$JAVA_PID\" ] && [ -n \"$APP_HOME\" ]; then "
                + "for PID_FILE in \"$APP_HOME\"/pid/*.pid; do "
                + "[ -f \"$PID_FILE\" ] || continue; "
                + "collect_pid_from_file \"$PID_FILE\"; "
                + "[ -n \"$JAVA_PID\" ] && break; "
                + "done; "
                + "fi; "
                + "if [ -z \"$JAVA_PID\" ]; then "
                + "JAVA_PID=$(ps -eo pid=,args= 2>/dev/null | "
                + "awk -v svc=\"$SERVICE_GUESS\" -v key=\"$SERVICE_KEY\" -v home=\"$APP_HOME\" -v self=\"$$\" "
                + "'BEGIN{svc=tolower(svc); key=tolower(key); home=tolower(home)} "
                + "$1!=self && $0 ~ /[j]ava/ {line=tolower($0); "
                + "if ((svc!=\"\" && index(line, svc)>0) || (key!=\"\" && index(line,key)>0) || (home!=\"\" && index(line,home)>0)) {print $1; exit}}'); "
                + "fi; "
                + "if [ -z \"$JAVA_PID\" ] && command -v jps >/dev/null 2>&1; then "
                + "JAVA_PID=$(jps -l 2>/dev/null | "
                + "awk -v svc=\"$SERVICE_GUESS\" -v key=\"$SERVICE_KEY\" "
                + "'BEGIN{svc=tolower(svc); key=tolower(key)} "
                + "{line=tolower($0); if((svc!=\"\" && index(line,svc)>0) || (key!=\"\" && index(line,key)>0)){print $1; exit}}'); "
                + "fi; "
                + "if [ -z \"$JAVA_PID\" ]; then echo __WW_JVM_GC__:NO_PID:$SERVICE_GUESS; exit; fi; "
                + "if ! command -v jstat >/dev/null 2>&1; then echo __WW_JVM_GC__:NO_JSTAT:$JAVA_PID; exit; fi; "
                + "GC_OUTPUT=$(jstat -gc \"$JAVA_PID\" 1 1 2>/dev/null); "
                + "GC_HEADER=$(printf \"%s\\n\" \"$GC_OUTPUT\" | awk 'NR==1{print $0}'); "
                + "GC_LINE=$(printf \"%s\\n\" \"$GC_OUTPUT\" | awk 'NR==2{print $0}'); "
                + "if [ -z \"$GC_HEADER\" ] || [ -z \"$GC_LINE\" ]; then echo __WW_JVM_GC__:NO_DATA:$JAVA_PID; exit; fi; "
                + "echo \"__WW_JVM_GC__:OK:$JAVA_PID|$GC_HEADER|$GC_LINE\"";
    }

    /**
     * 将实例服务键归一化为服务组名。
     * <p>
     * 例如：{@code mall-basic@node1} 会归一化为 {@code mall-basic}。
     * </p>
     *
     * @param serviceKey 实例服务键
     * @return 服务组名
     */
    private String normalizeServiceGroupName(String serviceKey) {
        String normalized = serviceKey == null ? "" : serviceKey.trim();
        int index = normalized.indexOf("@");
        if (index <= 0) {
            return normalized;
        }
        return normalized.substring(0, index);
    }

    /**
     * 提取所有“包含规则”的关键词集合（每条规则一组）。
     *
     * @param filterRules 过滤规则
     * @return 关键词组
     */
    private List<List<String>> resolveIncludeRuleTerms(List<LogStreamRequest.FilterRule> filterRules) {
        List<List<String>> groups = new ArrayList<>();
        if (filterRules == null || filterRules.isEmpty()) {
            return groups;
        }
        for (LogStreamRequest.FilterRule rule : filterRules) {
            if (rule == null) {
                continue;
            }
            if (!LogStreamRequest.FILTER_TYPE_INCLUDE.equalsIgnoreCase(trimToEmpty(rule.getType()))) {
                continue;
            }
            List<String> terms = extractExpressionTerms(trimToEmpty(rule.getData()));
            if (!terms.isEmpty()) {
                groups.add(terms);
            }
        }
        return groups;
    }

    /**
     * 从过滤表达式中提取原子关键词并去重。
     *
     * @param expression 表达式
     * @return 关键词列表
     */
    private List<String> extractExpressionTerms(String expression) {
        Set<String> terms = new LinkedHashSet<>();
        String normalized = trimToEmpty(expression);
        if (normalized.isEmpty()) {
            return new ArrayList<>();
        }
        String[] orGroups = normalized.split("\\|\\|");
        for (String orGroup : orGroups) {
            String[] andTerms = trimToEmpty(orGroup).split("&&");
            for (String term : andTerms) {
                String value = trimToEmpty(term);
                if (!value.isEmpty()) {
                    terms.add(value);
                }
            }
        }
        return new ArrayList<>(terms);
    }

    /**
     * 对字符串进行 null 安全的去空处理。
     *
     * @param source 原字符串
     * @return 非 null 字符串
     */
    private String trimToEmpty(String source) {
        return source == null ? "" : source.trim();
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
