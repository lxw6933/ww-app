package com.ww.app.ssh.controller;

import com.ww.app.ssh.model.LogStreamRequest;
import com.ww.app.ssh.model.LogTarget;
import com.ww.app.ssh.service.LogPanelQueryService;
import com.ww.app.ssh.service.SshLogService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * {@link LogReadController} 单元测试。
 */
class LogReadControllerTest {

    /**
     * 校验“全部服务”cat 模式会按日志自身时间戳返回真正的全局最新 N 行。
     */
    @Test
    void shouldReturnGlobalLatestRowsByParsedTimestampWhenAllService() {
        LogPanelQueryService queryService = Mockito.mock(LogPanelQueryService.class);
        SshLogService sshLogService = Mockito.mock(SshLogService.class);
        LogReadController controller = new LogReadController(queryService, sshLogService);
        LogStreamRequest request = buildAllServiceRequest(10);
        LogTarget targetA = new LogTarget("mall", "prod", "mall-basic@node1", null);
        LogTarget targetB = new LogTarget("mall", "prod", "mall-basic@node2", null);

        Mockito.when(queryService.resolveTargets(request)).thenReturn(Arrays.asList(targetA, targetB));
        Mockito.when(sshLogService.readByCat(targetA, request)).thenReturn(Arrays.asList(
                "2026-04-03 10:00:01.000 [A] first",
                "2026-04-03 10:00:03.000 [A] third",
                "2026-04-03 10:00:05.000 [A] fifth",
                "2026-04-03 10:00:07.000 [A] seventh",
                "2026-04-03 10:00:09.000 [A] ninth",
                "2026-04-03 10:00:11.000 [A] eleventh"
        ));
        Mockito.when(sshLogService.readByCat(targetB, request)).thenReturn(Arrays.asList(
                "2026-04-03 10:00:02.000 [B] second",
                "2026-04-03 10:00:04.000 [B] fourth",
                "2026-04-03 10:00:06.000 [B] sixth",
                "2026-04-03 10:00:08.000 [B] eighth",
                "2026-04-03 10:00:10.000 [B] tenth"
        ));

        ResponseEntity<List<String>> response = controller.readByCat(request);

        Assertions.assertEquals(Arrays.asList(
                "2026-04-03 10:00:02.000 [B] second",
                "2026-04-03 10:00:03.000 [A] third",
                "2026-04-03 10:00:04.000 [B] fourth",
                "2026-04-03 10:00:05.000 [A] fifth",
                "2026-04-03 10:00:06.000 [B] sixth",
                "2026-04-03 10:00:07.000 [A] seventh",
                "2026-04-03 10:00:08.000 [B] eighth",
                "2026-04-03 10:00:09.000 [A] ninth",
                "2026-04-03 10:00:10.000 [B] tenth",
                "2026-04-03 10:00:11.000 [A] eleventh"
        ), response.getBody());
    }

    /**
     * 校验当日志行无法解析时间戳时，会退回到“目标遍历顺序 + 原始行序”的稳定顺序。
     */
    @Test
    void shouldFallbackToStableSourceOrderWhenTimestampMissing() {
        LogPanelQueryService queryService = Mockito.mock(LogPanelQueryService.class);
        SshLogService sshLogService = Mockito.mock(SshLogService.class);
        LogReadController controller = new LogReadController(queryService, sshLogService);
        LogStreamRequest request = buildAllServiceRequest(10);
        LogTarget targetA = new LogTarget("mall", "prod", "mall-basic@node1", null);
        LogTarget targetB = new LogTarget("mall", "prod", "mall-basic@node2", null);

        Mockito.when(queryService.resolveTargets(request)).thenReturn(Arrays.asList(targetA, targetB));
        Mockito.when(sshLogService.readByCat(targetA, request)).thenReturn(Arrays.asList(
                "plain-row-1",
                "plain-row-2",
                "plain-row-3",
                "plain-row-4",
                "plain-row-5"
        ));
        Mockito.when(sshLogService.readByCat(targetB, request)).thenReturn(Arrays.asList(
                "plain-row-6",
                "plain-row-7",
                "plain-row-8",
                "plain-row-9",
                "plain-row-10",
                "plain-row-11"
        ));

        ResponseEntity<List<String>> response = controller.readByCat(request);

        Assertions.assertEquals(Arrays.asList(
                "plain-row-2",
                "plain-row-3",
                "plain-row-4",
                "plain-row-5",
                "plain-row-6",
                "plain-row-7",
                "plain-row-8",
                "plain-row-9",
                "plain-row-10",
                "plain-row-11"
        ), response.getBody());
    }

    /**
     * 校验当可解析时间戳日志与纯文本日志混排时，聚合窗口仍会优先保留真正最新的时间戳日志行。
     */
    @Test
    void shouldPreferTimestampedRowsOverPlainRowsWhenAggregatingLatestWindow() {
        LogPanelQueryService queryService = Mockito.mock(LogPanelQueryService.class);
        SshLogService sshLogService = Mockito.mock(SshLogService.class);
        LogReadController controller = new LogReadController(queryService, sshLogService);
        LogStreamRequest request = buildAllServiceRequest(10);
        LogTarget targetA = new LogTarget("mall", "prod", "mall-basic@node1", null);
        LogTarget targetB = new LogTarget("mall", "prod", "mall-basic@node2", null);

        Mockito.when(queryService.resolveTargets(request)).thenReturn(Arrays.asList(targetA, targetB));
        Mockito.when(sshLogService.readByCat(targetA, request)).thenReturn(Arrays.asList(
                "2026-04-03 10:00:01.000 [A] first",
                "2026-04-03 10:00:02.000 [A] second",
                "2026-04-03 10:00:03.000 [A] third",
                "2026-04-03 10:00:04.000 [A] fourth",
                "2026-04-03 10:00:05.000 [A] fifth",
                "2026-04-03 10:00:06.000 [A] sixth",
                "2026-04-03 10:00:07.000 [A] seventh",
                "2026-04-03 10:00:08.000 [A] eighth",
                "2026-04-03 10:00:09.000 [A] ninth",
                "2026-04-03 10:00:10.000 [A] tenth"
        ));
        Mockito.when(sshLogService.readByCat(targetB, request)).thenReturn(Collections.singletonList("plain-row-late"));

        ResponseEntity<List<String>> response = controller.readByCat(request);

        Assertions.assertEquals(Arrays.asList(
                "2026-04-03 10:00:01.000 [A] first",
                "2026-04-03 10:00:02.000 [A] second",
                "2026-04-03 10:00:03.000 [A] third",
                "2026-04-03 10:00:04.000 [A] fourth",
                "2026-04-03 10:00:05.000 [A] fifth",
                "2026-04-03 10:00:06.000 [A] sixth",
                "2026-04-03 10:00:07.000 [A] seventh",
                "2026-04-03 10:00:08.000 [A] eighth",
                "2026-04-03 10:00:09.000 [A] ninth",
                "2026-04-03 10:00:10.000 [A] tenth"
        ), response.getBody());
    }

    /**
     * 构造“全部服务”cat 模式请求。
     *
     * @param lines 期望保留的行数
     * @return 请求对象
     */
    private LogStreamRequest buildAllServiceRequest(int lines) {
        LogStreamRequest request = new LogStreamRequest();
        request.setProject("mall");
        request.setEnv("prod");
        request.setService(LogStreamRequest.ALL);
        request.setReadMode(LogStreamRequest.READ_MODE_CAT);
        request.setLines(lines);
        return request;
    }
}
