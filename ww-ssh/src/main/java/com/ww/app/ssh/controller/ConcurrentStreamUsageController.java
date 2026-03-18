package com.ww.app.ssh.controller;

import com.ww.app.common.utils.IpUtil;
import com.ww.app.ssh.model.ConcurrentStreamAccessResponse;
import com.ww.app.ssh.model.ConcurrentStreamUsageSnapshot;
import com.ww.app.ssh.service.SshLogService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletRequest;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 实时日志流占用概览接口。
 * <p>
 * 仅允许通过当前运行节点本机 IP 访问页面时查看，
 * 用于在页面右上角弹框中展示实时流占用情况。
 * </p>
 */
@RestController
@RequestMapping("/api/stream-usage")
public class ConcurrentStreamUsageController {

    /**
     * 流占用服务。
     */
    private final SshLogService sshLogService;

    /**
     * 构造方法。
     *
     * @param sshLogService 流占用服务
     */
    public ConcurrentStreamUsageController(SshLogService sshLogService) {
        this.sshLogService = sshLogService;
    }

    /**
     * 查询当前页面访问地址是否具备流占用概览查看权限。
     *
     * @param request HTTP 请求
     * @return 权限响应
     */
    @GetMapping("/access")
    public ConcurrentStreamAccessResponse access(HttpServletRequest request) {
        ConcurrentStreamAccessResponse response = new ConcurrentStreamAccessResponse();
        response.setEnabled(isLocalAccessHost(request));
        return response;
    }

    /**
     * 查询当前流占用概览。
     *
     * @param request HTTP 请求
     * @return 流占用概览
     */
    @GetMapping
    public ConcurrentStreamUsageSnapshot usage(HttpServletRequest request) {
        ensureLocalAccessHost(request);
        return sshLogService.getConcurrentStreamUsageSnapshot();
    }

    /**
     * 校验当前页面访问地址是否属于运行节点本机 IP。
     *
     * @param request HTTP 请求
     */
    private void ensureLocalAccessHost(HttpServletRequest request) {
        if (!isLocalAccessHost(request)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "仅通过运行当前项目的本机 IP 访问页面时可查看流占用概览");
        }
    }

    /**
     * 判断当前页面访问地址是否属于运行节点本机 IP。
     * <p>
     * 这里判断的是浏览器访问本页面时使用的 Host/ServerName，
     * 而不是请求来源客户端 IP，避免把“使用部署机 IP 打开页面”和“访问者来源 IP 属于部署机”
     * 两个概念混淆。
     * </p>
     *
     * @param request HTTP 请求
     * @return true 表示属于本机 IP
     */
    private boolean isLocalAccessHost(HttpServletRequest request) {
        String accessHost = normalizeAccessHost(request == null ? null : request.getServerName());
        if (!IpUtil.isValidIp(accessHost)) {
            return false;
        }
        Set<String> localIps = new LinkedHashSet<>(IpUtil.getAllLocalIps());
        localIps.add(IpUtil.getLocalIp());
        localIps.add("127.0.0.1");
        localIps.add("0:0:0:0:0:0:0:1");
        localIps.add("::1");
        return localIps.stream()
                .map(this::normalizeAccessHost)
                .anyMatch(accessHost::equals);
    }

    /**
     * 规整访问地址中的主机名/IP 文本。
     * <p>
     * 该方法主要处理两类情况：
     * 1. {@code localhost} 统一映射到回环 IPv4，便于与本机地址集合比较；
     * 2. IPv6 Host 头可能带有中括号，需要去壳后再进行 IP 合法性校验。
     * </p>
     *
     * @param host 原始 Host/ServerName
     * @return 规整后的主机名/IP 文本
     */
    private String normalizeAccessHost(String host) {
        String normalizedHost = host == null ? "" : host.trim();
        if (normalizedHost.isEmpty()) {
            return "";
        }
        if (normalizedHost.startsWith("[") && normalizedHost.endsWith("]") && normalizedHost.length() > 2) {
            normalizedHost = normalizedHost.substring(1, normalizedHost.length() - 1);
        }
        if ("localhost".equalsIgnoreCase(normalizedHost)) {
            return "127.0.0.1";
        }
        return normalizedHost;
    }
}
