package com.ww.app.ssh.service;

import com.ww.app.ssh.config.LogPanelProperties;
import com.ww.app.ssh.model.LogTarget;
import com.ww.app.ssh.model.MiddlewareConsoleVO;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 中间件后台查询服务。
 * <p>
 * 负责从日志面板配置中解析测试环境下的中间件后台入口，
 * 并对跳转地址做基础白名单校验，避免将系统变成任意 URL 跳板。
 * </p>
 */
@Service
public class MiddlewareConsoleService {

    /**
     * 识别“主机/IP + 端口 + 可选路径”形式的后台地址。
     */
    private static final Pattern RAW_HOST_PORT_URL_PATTERN = Pattern.compile(
            "^(?:localhost|\\d{1,3}(?:\\.\\d{1,3}){3}|[A-Za-z0-9-]+(?:\\.[A-Za-z0-9-]+)*):(\\d+)(?:[/?#].*)?$"
    );

    /**
     * 识别“主机/IP + 可选路径”形式的后台地址。
     */
    private static final Pattern RAW_HOST_URL_PATTERN = Pattern.compile(
            "^(?:localhost|\\d{1,3}(?:\\.\\d{1,3}){3}|[A-Za-z0-9-]+(?:\\.[A-Za-z0-9-]+)+)(?:[/?#].*)?$"
    );

    /**
     * 日志面板目标解析服务。
     */
    private final LogPanelQueryService logPanelQueryService;

    /**
     * 构造方法。
     *
     * @param logPanelQueryService 日志面板目标解析服务
     */
    public MiddlewareConsoleService(LogPanelQueryService logPanelQueryService) {
        this.logPanelQueryService = logPanelQueryService;
    }

    /**
     * 查询指定实例已配置的中间件后台列表。
     *
     * @param project 项目名称
     * @param env 环境名称
     * @param service 实例服务键
     * @return 中间件后台列表
     */
    public List<MiddlewareConsoleVO> listConsoles(String project, String env, String service) {
        LogTarget target = logPanelQueryService.resolveExactTarget(project, env, service);
        List<MiddlewareConsoleVO> result = new ArrayList<>();
        Map<String, LogPanelProperties.MiddlewareConsole> middlewares =
                resolveEffectiveMiddlewares(target.getProject(), target.getEnv(), target.getServerNode());
        if (middlewares.isEmpty()) {
            return result;
        }

        for (Map.Entry<String, LogPanelProperties.MiddlewareConsole> entry : middlewares.entrySet()) {
            String code = trimToEmpty(entry.getKey());
            LogPanelProperties.MiddlewareConsole console = entry.getValue();
            if (code.isEmpty() || console == null || !console.isConsoleEnabled()) {
                continue;
            }
            MiddlewareConsoleVO vo = new MiddlewareConsoleVO();
            vo.setCode(code);
            vo.setName(resolveConsoleName(code, console));
            vo.setUrl(trimToEmpty(console.getUrl()));
            vo.setLaunchable(isLaunchableUrl(console.getUrl()));
            vo.setUsername(trimToEmpty(console.getUsername()));
            vo.setPassword(trimToEmpty(console.getPassword()));
            vo.setSort(console.getSort() == null ? 0 : console.getSort());
            result.add(vo);
        }

        result.sort(Comparator
                .comparing((MiddlewareConsoleVO item) -> item.getSort() == null ? 0 : item.getSort())
                .thenComparing(item -> trimToEmpty(item.getName()))
                .thenComparing(item -> trimToEmpty(item.getCode())));
        return result;
    }

    /**
     * 解析指定中间件后台的跳转地址。
     *
     * @param project 项目名称
     * @param env 环境名称
     * @param service 实例服务键
     * @param code 中间件编码
     * @return 可直接重定向的后台地址
     */
    public String resolveLaunchUrl(String project, String env, String service, String code) {
        LogTarget target = logPanelQueryService.resolveExactTarget(project, env, service);
        Map<String, LogPanelProperties.MiddlewareConsole> middlewares =
                resolveEffectiveMiddlewares(target.getProject(), target.getEnv(), target.getServerNode());
        if (middlewares.isEmpty()) {
            throw new IllegalArgumentException("当前实例未配置中间件后台");
        }
        String normalizedCode = trimToEmpty(code);
        if (normalizedCode.isEmpty()) {
            throw new IllegalArgumentException("中间件编码不能为空");
        }

        LogPanelProperties.MiddlewareConsole console = middlewares.get(normalizedCode);
        if (console == null || !console.isConsoleEnabled()) {
            throw new IllegalArgumentException("未找到可用的中间件后台配置: " + normalizedCode);
        }
        String launchUrl = resolveLaunchTargetUrl(console.getUrl());
        if (launchUrl.isEmpty()) {
            throw new IllegalArgumentException("中间件后台地址非法或未启用: " + normalizedCode);
        }
        return launchUrl;
    }

    /**
     * 解析生效的中间件配置。
     * <p>
     * 优先使用环境级共享配置；若环境级未配置，则回退到旧的服务级配置，
     * 兼容已有临时配置写法。
     * </p>
     *
     * @param project 项目名称
     * @param env 环境名称
     * @param serverNode 服务节点配置
     * @return 生效的中间件配置映射
     */
    private Map<String, LogPanelProperties.MiddlewareConsole> resolveEffectiveMiddlewares(String project,
                                                                                          String env,
                                                                                          LogPanelProperties.ServerNode serverNode) {
        Map<String, LogPanelProperties.MiddlewareConsole> envMiddlewares =
                logPanelQueryService.resolveEnvironmentMiddlewares(project, env);
        if (envMiddlewares != null && !envMiddlewares.isEmpty()) {
            return envMiddlewares;
        }
        if (serverNode == null || serverNode.getMiddlewares() == null || serverNode.getMiddlewares().isEmpty()) {
            return java.util.Collections.emptyMap();
        }
        return serverNode.getMiddlewares();
    }

    /**
     * 解析中间件展示名称。
     *
     * @param code 中间件编码
     * @param console 中间件配置
     * @return 展示名称
     */
    private String resolveConsoleName(String code, LogPanelProperties.MiddlewareConsole console) {
        String name = console == null ? "" : trimToEmpty(console.getName());
        return name.isEmpty() ? code : name;
    }

    /**
     * 校验中间件后台地址是否允许跳转。
     * <p>
     * 当前仅放行显式配置的 http/https 绝对地址，避免被利用为任意协议跳板。
     * </p>
     *
     * @param url 地址
     * @return true 表示允许
     */
    private boolean isLaunchableUrl(String url) {
        return !resolveLaunchTargetUrl(url).isEmpty();
    }

    /**
     * 解析浏览器可直接跳转的中间件后台地址。
     * <p>
     * 原始配置值保持不变，仅在执行“打开后台”时，对裸主机/IP 地址补全默认协议，
     * 确保浏览器能够识别并发起跳转。
     * </p>
     *
     * @param url 原始配置地址
     * @return 可跳转地址；无法识别时返回空字符串
     */
    private String resolveLaunchTargetUrl(String url) {
        String normalized = trimToEmpty(url);
        if (normalized.isEmpty() || containsWhitespace(normalized)) {
            return "";
        }
        if (isExplicitHttpUrl(normalized)) {
            return normalized;
        }
        if (!isRawHostStyleUrl(normalized)) {
            return "";
        }
        String candidate = "http://" + normalized;
        return isExplicitHttpUrl(candidate) ? candidate : "";
    }

    /**
     * 校验地址是否为显式声明协议的 HTTP/HTTPS 后台地址。
     *
     * @param url 地址
     * @return true 表示合法
     */
    private boolean isExplicitHttpUrl(String url) {
        try {
            URI uri = URI.create(trimToEmpty(url));
            String scheme = trimToEmpty(uri.getScheme()).toLowerCase();
            if (!"http".equals(scheme) && !"https".equals(scheme)) {
                return false;
            }
            return !trimToEmpty(uri.getHost()).isEmpty();
        } catch (Exception ignored) {
            return false;
        }
    }

    /**
     * 判断地址是否属于裸主机/IP 形式。
     * <p>
     * 兼容运维场景下常见的 {@code 192.168.1.10:8848/nacos}、
     * {@code nacos:8848}、{@code console.example.com} 等写法。
     * </p>
     *
     * @param url 原始地址
     * @return true 表示可按裸主机/IP 规则处理
     */
    private boolean isRawHostStyleUrl(String url) {
        String normalized = trimToEmpty(url);
        return RAW_HOST_PORT_URL_PATTERN.matcher(normalized).matches()
                || RAW_HOST_URL_PATTERN.matcher(normalized).matches();
    }

    /**
     * 判断文本中是否包含空白字符。
     *
     * @param text 文本
     * @return true 表示包含空白
     */
    private boolean containsWhitespace(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        for (int index = 0; index < text.length(); index++) {
            if (Character.isWhitespace(text.charAt(index))) {
                return true;
            }
        }
        return false;
    }

    /**
     * 字符串去空格并兜底空值。
     *
     * @param text 原始字符串
     * @return 非 null 字符串
     */
    private String trimToEmpty(String text) {
        return text == null ? "" : text.trim();
    }
}
