package com.ww.app.ssh.controller;

import com.ww.app.ssh.model.LogStreamRequest;
import com.ww.app.ssh.model.LogTarget;
import com.ww.app.ssh.service.LogPanelQueryService;
import com.ww.app.ssh.service.SshLogService;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.PriorityQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 日志读取接口。
 * <p>
 * 提供一次性快照读取能力，适用于“cat 模式”快速排查。
 * </p>
 */
@RestController
@RequestMapping("/api/log")
public class LogReadController {

    /**
     * 常见 Java/应用日志时间戳提取表达式。
     * <p>
     * 兼容 yyyy-MM-dd HH:mm:ss、yyyy-MM-dd'T'HH:mm:ss 以及可选毫秒、时区偏移等形式。
     * </p>
     */
    private static final Pattern COMMON_TIMESTAMP_PATTERN = Pattern.compile(
            "(\\d{4}[-/]\\d{2}[-/]\\d{2}[ T]\\d{2}:\\d{2}:\\d{2}(?:[.,]\\d{1,9})?(?:Z|[+-]\\d{2}:?\\d{2})?)");

    /**
     * nginx access/error 日志时间戳提取表达式。
     */
    private static final Pattern NGINX_TIMESTAMP_PATTERN = Pattern.compile(
            "\\[(\\d{2}/[A-Za-z]{3}/\\d{4}:\\d{2}:\\d{2}:\\d{2} [+-]\\d{4})]");

    /**
     * 未显式带时区的日志时间戳默认按当前服务所在时区解释。
     */
    private static final ZoneId DEFAULT_LOG_ZONE = ZoneId.systemDefault();

    /**
     * 带时区偏移的时间戳解析器集合。
     */
    private static final List<DateTimeFormatter> OFFSET_TIMESTAMP_FORMATTERS = Collections.unmodifiableList(Arrays.asList(
            DateTimeFormatter.ISO_OFFSET_DATE_TIME,
            buildOffsetTimestampFormatter("yyyy-MM-dd HH:mm:ss", "+HH:MM"),
            buildOffsetTimestampFormatter("yyyy-MM-dd HH:mm:ss", "+HHMM"),
            buildOffsetTimestampFormatter("yyyy/MM/dd HH:mm:ss", "+HH:MM"),
            buildOffsetTimestampFormatter("yyyy/MM/dd HH:mm:ss", "+HHMM"),
            buildOffsetTimestampFormatter("yyyy-MM-dd'T'HH:mm:ss", "+HH:MM"),
            buildOffsetTimestampFormatter("yyyy-MM-dd'T'HH:mm:ss", "+HHMM"),
            buildOffsetTimestampFormatter("yyyy/MM/dd'T'HH:mm:ss", "+HH:MM"),
            buildOffsetTimestampFormatter("yyyy/MM/dd'T'HH:mm:ss", "+HHMM")
    ));

    /**
     * 不带时区偏移的时间戳解析器集合。
     */
    private static final List<DateTimeFormatter> LOCAL_TIMESTAMP_FORMATTERS = Collections.unmodifiableList(Arrays.asList(
            buildLocalTimestampFormatter("yyyy-MM-dd HH:mm:ss"),
            buildLocalTimestampFormatter("yyyy/MM/dd HH:mm:ss"),
            buildLocalTimestampFormatter("yyyy-MM-dd'T'HH:mm:ss"),
            buildLocalTimestampFormatter("yyyy/MM/dd'T'HH:mm:ss")
    ));

    /**
     * nginx 时间戳解析器。
     */
    private static final DateTimeFormatter NGINX_TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MMM/yyyy:HH:mm:ss Z", Locale.ENGLISH);

    /**
     * 配置查询服务。
     */
    private final LogPanelQueryService logPanelQueryService;

    /**
     * SSH 日志服务。
     */
    private final SshLogService sshLogService;

    /**
     * 构造方法。
     *
     * @param logPanelQueryService 配置查询服务
     * @param sshLogService SSH 日志服务
     */
    public LogReadController(LogPanelQueryService logPanelQueryService, SshLogService sshLogService) {
        this.logPanelQueryService = logPanelQueryService;
        this.sshLogService = sshLogService;
    }

    /**
     * 一次性读取日志快照（cat 模式）。
     *
     * @param request 日志请求参数
     * @return 日志文本行集合
     */
    @PostMapping("/cat")
    public ResponseEntity<List<String>> readByCat(@RequestBody LogStreamRequest request) {
        try {
            validateFilePathPolicy(request);
            List<LogTarget> targets = logPanelQueryService.resolveTargets(request);
            int keep = request.normalizedLines();
            boolean aggregateTailWindow = request.isAllService();
            List<String> rows = aggregateTailWindow ? readAggregateLatestRows(targets, request, keep) : new ArrayList<>();
            if (!aggregateTailWindow) {
                for (LogTarget target : targets) {
                    try {
                        rows.addAll(sshLogService.readByCat(target, request));
                    } catch (Exception ex) {
                        rows.add(buildErrorLine(target, ex));
                    }
                }
            }
            HttpHeaders headers = new HttpHeaders();
            headers.setCacheControl(CacheControl.noStore().mustRevalidate().getHeaderValue());
            headers.add(HttpHeaders.PRAGMA, "no-cache");
            headers.add(HttpHeaders.EXPIRES, "0");
            return ResponseEntity.ok().headers(headers).body(rows);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "日志快照读取失败: " + ex.getMessage(), ex);
        }
    }

    /**
     * 校验日志文件选择策略。
     * <p>
     * 单服务模式下要求前端显式传入 filePath，避免后端自动回退默认文件导致排查对象不明确。
     * </p>
     *
     * @param request 请求参数
     */
    private void validateFilePathPolicy(LogStreamRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("请求参数不能为空");
        }
        if (!request.isAllService() && request.normalizedFilePath().isEmpty()) {
            throw new IllegalArgumentException("单服务模式下必须显式选择日志文件");
        }
    }

    /**
     * 读取“全部服务”模式下真正的全局最新 N 行。
     * <p>
     * 聚合模式下不能再按目标遍历顺序简单拼接尾窗，否则越靠后遍历的实例会“天然更新”。
     * 这里改为对每一行尝试提取时间戳，并使用有界最小堆仅保留全局最新 N 行；
     * 当某行无法解析时间戳时，退回到“目标遍历顺序 + 原始行序”的稳定排序。
     * </p>
     *
     * @param targets 目标集合
     * @param request 原始请求
     * @param keep 最大保留行数
     * @return 全局最新 N 行
     */
    private List<String> readAggregateLatestRows(List<LogTarget> targets, LogStreamRequest request, int keep) {
        if (targets == null || targets.isEmpty() || keep <= 0) {
            return new ArrayList<>();
        }
        PriorityQueue<AggregatedLogRow> latestRows = new PriorityQueue<>(keep, this::compareAggregatedRows);
        long fallbackOrder = 0L;
        for (LogTarget target : targets) {
            try {
                fallbackOrder = retainLatestRows(latestRows, sshLogService.readByCat(target, request), keep, fallbackOrder);
            } catch (Exception ex) {
                fallbackOrder = retainLatestRows(latestRows,
                        Collections.singletonList(buildErrorLine(target, ex)),
                        keep,
                        fallbackOrder);
            }
        }
        List<AggregatedLogRow> orderedRows = new ArrayList<>(latestRows);
        orderedRows.sort(this::compareAggregatedRows);
        List<String> rows = new ArrayList<>(orderedRows.size());
        for (AggregatedLogRow orderedRow : orderedRows) {
            rows.add(orderedRow.getRow());
        }
        return rows;
    }

    /**
     * 将一批候选日志行压入“全局最新 N 行”窗口。
     *
     * @param latestRows 当前全局窗口
     * @param rows 候选日志行
     * @param keep 最大保留数量
     * @param fallbackOrder 当前稳定序号
     * @return 追加后的下一个稳定序号
     */
    private long retainLatestRows(PriorityQueue<AggregatedLogRow> latestRows,
                                  List<String> rows,
                                  int keep,
                                  long fallbackOrder) {
        if (latestRows == null || rows == null || rows.isEmpty() || keep <= 0) {
            return fallbackOrder;
        }
        long nextOrder = fallbackOrder;
        for (String row : rows) {
            latestRows.offer(new AggregatedLogRow(row, extractRowTimestamp(row), nextOrder++));
            if (latestRows.size() > keep) {
                latestRows.poll();
            }
        }
        return nextOrder;
    }

    /**
     * 比较两条聚合日志的“新旧”顺序。
     * <p>
     * 1. 当双方都能解析出时间戳时，优先按日志自身时间排序；<br>
     * 2. 当任一方无法解析时，退回到稳定序号，确保输出顺序可预测且不会跨实例乱跳。<br>
     * </p>
     *
     * @param left 左侧日志
     * @param right 右侧日志
     * @return 比较结果；负数表示 left 更旧
     */
    private int compareAggregatedRows(AggregatedLogRow left, AggregatedLogRow right) {
        if (left == right) {
            return 0;
        }
        if (left.hasTimestamp() && right.hasTimestamp()) {
            int timestampCompare = left.getTimestamp().compareTo(right.getTimestamp());
            if (timestampCompare != 0) {
                return timestampCompare;
            }
        }
        if (left.hasTimestamp() != right.hasTimestamp()) {
            return Boolean.compare(left.hasTimestamp(), right.hasTimestamp());
        }
        return Long.compare(left.getFallbackOrder(), right.getFallbackOrder());
    }

    /**
     * 构造统一的读取失败提示行。
     *
     * @param target 目标节点
     * @param ex 读取异常
     * @return 错误提示行
     */
    private String buildErrorLine(LogTarget target, Exception ex) {
        return "[系统提示] 读取失败 " + target.displayName() + ": " + ex.getMessage();
    }

    /**
     * 从单行日志中提取时间戳。
     *
     * @param row 原始日志行
     * @return 解析成功后的时间戳；失败时返回 null
     */
    private Instant extractRowTimestamp(String row) {
        if (row == null || row.trim().isEmpty()) {
            return null;
        }
        String commonCandidate = matchFirstTimestamp(row, COMMON_TIMESTAMP_PATTERN);
        Instant commonTimestamp = parseCommonTimestamp(commonCandidate);
        if (commonTimestamp != null) {
            return commonTimestamp;
        }
        String nginxCandidate = matchFirstTimestamp(row, NGINX_TIMESTAMP_PATTERN);
        if (nginxCandidate.isEmpty()) {
            return null;
        }
        try {
            return ZonedDateTime.parse(nginxCandidate, NGINX_TIMESTAMP_FORMATTER).toInstant();
        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * 从日志行中提取第一个命中的时间戳片段。
     *
     * @param row 原始日志行
     * @param pattern 匹配表达式
     * @return 匹配到的时间戳片段；未匹配时返回空串
     */
    private String matchFirstTimestamp(String row, Pattern pattern) {
        if (row == null || pattern == null) {
            return "";
        }
        Matcher matcher = pattern.matcher(row);
        if (!matcher.find()) {
            return "";
        }
        return matcher.group(1);
    }

    /**
     * 解析常见应用日志时间戳。
     *
     * @param candidate 时间戳候选文本
     * @return 解析成功后的时间戳；失败时返回 null
     */
    private Instant parseCommonTimestamp(String candidate) {
        if (candidate == null || candidate.trim().isEmpty()) {
            return null;
        }
        String normalized = candidate.trim();
        for (DateTimeFormatter formatter : OFFSET_TIMESTAMP_FORMATTERS) {
            try {
                return OffsetDateTime.parse(normalized, formatter).toInstant();
            } catch (Exception ignored) {
                // 继续尝试其他格式
            }
        }
        for (DateTimeFormatter formatter : LOCAL_TIMESTAMP_FORMATTERS) {
            try {
                return LocalDateTime.parse(normalized, formatter).atZone(DEFAULT_LOG_ZONE).toInstant();
            } catch (Exception ignored) {
                // 继续尝试其他格式
            }
        }
        return null;
    }

    /**
     * 构建本地时间戳解析器，兼容“.毫秒”与“,毫秒”。
     *
     * @param pattern 主体格式
     * @return 时间解析器
     */
    private static DateTimeFormatter buildLocalTimestampFormatter(String pattern) {
        return new DateTimeFormatterBuilder()
                .parseCaseInsensitive()
                .appendPattern(pattern)
                .optionalStart()
                .appendFraction(ChronoField.NANO_OF_SECOND, 1, 9, true)
                .optionalEnd()
                .optionalStart()
                .appendLiteral(',')
                .appendFraction(ChronoField.NANO_OF_SECOND, 1, 9, false)
                .optionalEnd()
                .toFormatter(Locale.ENGLISH);
    }

    /**
     * 构建带时区偏移的时间戳解析器。
     *
     * @param pattern 主体格式
     * @param offsetPattern 时区偏移格式
     * @return 时间解析器
     */
    private static DateTimeFormatter buildOffsetTimestampFormatter(String pattern, String offsetPattern) {
        return new DateTimeFormatterBuilder()
                .parseCaseInsensitive()
                .append(buildLocalTimestampFormatter(pattern))
                .appendOffset(offsetPattern, "Z")
                .toFormatter(Locale.ENGLISH);
    }

    /**
     * 聚合模式下用于比较新旧的日志记录。
     */
    private static final class AggregatedLogRow {

        /**
         * 原始日志文本。
         */
        private final String row;

        /**
         * 解析出的日志时间戳；解析失败时为 null。
         */
        private final Instant timestamp;

        /**
         * 兜底稳定顺序号。
         */
        private final long fallbackOrder;

        /**
         * 构造方法。
         *
         * @param row 原始日志
         * @param timestamp 解析出的时间戳
         * @param fallbackOrder 稳定顺序号
         */
        private AggregatedLogRow(String row, Instant timestamp, long fallbackOrder) {
            this.row = row;
            this.timestamp = timestamp;
            this.fallbackOrder = fallbackOrder;
        }

        /**
         * 获取原始日志文本。
         *
         * @return 原始日志文本
         */
        private String getRow() {
            return row;
        }

        /**
         * 获取解析出的时间戳。
         *
         * @return 时间戳
         */
        private Instant getTimestamp() {
            return timestamp;
        }

        /**
         * 判断是否包含可用时间戳。
         *
         * @return true 表示可用
         */
        private boolean hasTimestamp() {
            return timestamp != null;
        }

        /**
         * 获取兜底稳定顺序号。
         *
         * @return 稳定顺序号
         */
        private long getFallbackOrder() {
            return fallbackOrder;
        }
    }
}
