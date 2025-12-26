package com.ww.app.open.interceptor;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.ww.app.common.enums.GlobalResCodeConstants;
import com.ww.app.common.exception.ApiException;
import com.ww.app.common.utils.DigitalSignatureUtil;
import com.ww.app.open.common.BaseOpenRequest;
import com.ww.app.open.common.OpenApiContext;
import com.ww.app.open.entity.BusinessClientInfo;
import com.ww.app.open.entity.OpenApplication;
import com.ww.app.open.entity.OpenApiInfo;
import com.ww.app.open.respository.BusinessClientInfoRepository;
import com.ww.app.open.service.OpenApiInfoService;
import com.ww.app.open.service.OpenApiPermissionService;
import com.ww.app.open.service.OpenApplicationService;
import com.github.benmanes.caffeine.cache.Cache;
import com.ww.app.common.utils.CaffeineUtil;
import com.ww.app.open.constant.OpenPlatformConstants;
import com.ww.app.open.utils.CacheKeyBuilder;
import com.ww.app.open.utils.SignatureUtil;
import com.ww.app.open.utils.StatusValidator;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 开放平台API安全拦截器
 * 
 * @author ww
 * @create 2024-05-27
 * @description: 实现签名验证、限流、防重放等安全功能
 */
@Slf4j
@Component
public class OpenApiInterceptor implements HandlerInterceptor {

    @Autowired
    private OpenApplicationService openApplicationService;

    @Autowired
    private OpenApiInfoService openApiInfoService;

    @Autowired
    private OpenApiPermissionService openApiPermissionService;

    @Autowired
    private BusinessClientInfoRepository businessClientInfoRepository;


    /**
     * 防重放攻击缓存
     * 容量: 10000个请求
     * 过期时间: 5分钟
     */
    private Cache<String, Boolean> replayAttackCache;

    /**
     * 限流缓存
     * 容量: 5000个应用+API组合
     * 过期时间: 60秒（限流窗口）
     * 使用 Cache 而不是 LoadingCache，因为需要手动管理计数器
     */
    private Cache<String, AtomicLong> rateLimitCache;

    @PostConstruct
    public void init() {
        // 初始化防重放攻击缓存
        this.replayAttackCache = CaffeineUtil.createCache(
                OpenPlatformConstants.REPLAY_CACHE_INITIAL_CAPACITY,
                OpenPlatformConstants.REPLAY_CACHE_MAXIMUM_SIZE,
                (int) OpenPlatformConstants.REPLAY_ATTACK_CACHE_TIME_SECONDS,
                TimeUnit.SECONDS
        );

        // 初始化限流缓存（使用 Cache 而不是 LoadingCache）
        this.rateLimitCache = CaffeineUtil.createCache(
                OpenPlatformConstants.RATE_LIMIT_CACHE_INITIAL_CAPACITY,
                OpenPlatformConstants.RATE_LIMIT_CACHE_MAXIMUM_SIZE,
                (int) OpenPlatformConstants.RATE_LIMIT_WINDOW_SECONDS,
                TimeUnit.SECONDS
        );

        log.info("OpenApiInterceptor 缓存初始化完成 - 防重放缓存容量: {}, 限流缓存容量: {}",
                OpenPlatformConstants.REPLAY_CACHE_MAXIMUM_SIZE,
                OpenPlatformConstants.RATE_LIMIT_CACHE_MAXIMUM_SIZE);
    }

    @Override
    public boolean preHandle(HttpServletRequest request, @NotNull HttpServletResponse response, @NotNull Object handler) throws Exception {
        String requestPath = request.getRequestURI();
        String httpMethod = request.getMethod();

        // 创建上下文
        OpenApiContext context = new OpenApiContext();
        context.setRequestStartTime(System.currentTimeMillis());
        context.setRequestIp(getClientIp(request));
        OpenApiContext.set(context);

        try {
            // 1. 读取请求体
            String requestBody = readRequestBody(request);
            if (StrUtil.isBlank(requestBody)) {
                throw new ApiException(GlobalResCodeConstants.BAD_REQUEST.getCode(), "请求体不能为空");
            }

            // 2. 解析请求参数
            BaseOpenRequest<?> openRequest = JSON.parseObject(requestBody, BaseOpenRequest.class);
            if (openRequest == null) {
                throw new ApiException(GlobalResCodeConstants.BAD_REQUEST.getCode(), "请求参数格式错误");
            }

            // 3. 验证基础参数
            validateBasicParams(openRequest);

            // 4. 设置上下文信息
            context.setTransId(openRequest.getTransId());
            context.setSysCode(openRequest.getSysCode());
            context.setAppCode(openRequest.getAppCode());
            context.setApiCode(openRequest.getMethodCode());

            // 5. 验证应用信息
            OpenApplication application = validateApplication(openRequest.getAppCode(), openRequest.getSysCode());

            // 6. 验证API信息
            OpenApiInfo apiInfo = validateApiInfo(requestPath, httpMethod, openRequest.getMethodCode());

            // 7. 验证权限
            validatePermission(openRequest.getAppCode(), apiInfo.getApiCode());

            // 8. 验证IP白名单
            validateIpWhitelist(application, context.getRequestIp());

            // 9. 验证请求时间（防重放攻击）
            validateRequestTime(openRequest.getReqTime());

            // 10. 验证签名（从商户信息获取公钥）
            BusinessClientInfo businessClient = businessClientInfoRepository.getBySysCode(openRequest.getSysCode());
            if (businessClient == null || businessClient.getPublicKey() == null) {
                throw new ApiException(GlobalResCodeConstants.BAD_REQUEST.getCode(), "商户信息不存在或公钥未配置");
            }
            validateSignature(openRequest, businessClient.getPublicKey());

            // 11. 防重放攻击检查
            checkReplayAttack(openRequest.getTransId());

            // 12. 限流检查
            checkRateLimit(openRequest.getAppCode(), apiInfo.getApiCode(), 
                          openApiPermissionService.getQpsLimit(openRequest.getAppCode(), apiInfo.getApiCode()));

            // 13. 更新上下文
            context.setApiCode(apiInfo.getApiCode());
            OpenApiContext.set(context);

            // 将请求信息存储到request attribute中，供后续使用
            request.setAttribute("openRequest", openRequest);
            request.setAttribute("openApplication", application);
            request.setAttribute("openApiInfo", apiInfo);

            return true;
        } catch (Exception e) {
            log.error("开放平台API拦截器验证失败: path={}, method={}, error={}", 
                     requestPath, httpMethod, e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public void afterCompletion(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response, @NotNull Object handler, Exception ex) {
        // 清理上下文
        OpenApiContext.clear();
    }

    /**
     * 验证基础参数
     */
    private void validateBasicParams(BaseOpenRequest<?> request) {
        Assert.notBlank(request.getTransId(), () -> new ApiException(GlobalResCodeConstants.BAD_REQUEST.getCode(), "流水号不能为空"));
        Assert.notBlank(request.getSysCode(), () -> new ApiException(GlobalResCodeConstants.BAD_REQUEST.getCode(), "商户编码不能为空"));
        Assert.notBlank(request.getAppCode(), () -> new ApiException(GlobalResCodeConstants.BAD_REQUEST.getCode(), "应用编码不能为空"));
        Assert.notBlank(request.getMethodCode(), () -> new ApiException(GlobalResCodeConstants.BAD_REQUEST.getCode(), "方法编码不能为空"));
        Assert.notBlank(request.getReqTime(), () -> new ApiException(GlobalResCodeConstants.BAD_REQUEST.getCode(), "请求时间不能为空"));
        Assert.notBlank(request.getSign(), () -> new ApiException(GlobalResCodeConstants.BAD_REQUEST.getCode(), "签名不能为空"));
    }

    /**
     * 验证应用信息
     */
    private OpenApplication validateApplication(String appCode, String sysCode) {
        OpenApplication application = openApplicationService.getByAppCode(appCode);
        if (application == null) {
            throw new ApiException(GlobalResCodeConstants.BAD_REQUEST.getCode(), "应用不存在");
        }
        if (!application.getSysCode().equals(sysCode)) {
            throw new ApiException(GlobalResCodeConstants.BAD_REQUEST.getCode(), "商户编码与应用不匹配");
        }
        StatusValidator.validateApplicationEnabled(application);
        return application;
    }

    /**
     * 验证API信息
     */
    private OpenApiInfo validateApiInfo(String requestPath, String httpMethod, String methodCode) {
        OpenApiInfo apiInfo = openApiInfoService.getByPathAndMethod(requestPath, httpMethod);
        if (apiInfo == null) {
            // 如果通过路径找不到，尝试通过methodCode查找
            apiInfo = openApiInfoService.getByApiCode(methodCode);
        }
        StatusValidator.validateApiPublished(apiInfo);
        return apiInfo;
    }

    /**
     * 验证权限
     */
    private void validatePermission(String appCode, String apiCode) {
        if (!openApiPermissionService.hasPermission(appCode, apiCode)) {
            throw new ApiException(GlobalResCodeConstants.FORBIDDEN.getCode(), "应用无权限调用此API");
        }
    }

    /**
     * 验证IP白名单
     */
    private void validateIpWhitelist(OpenApplication application, String requestIp) {
        if (StrUtil.isNotBlank(application.getIpWhitelist())) {
            String[] whitelist = application.getIpWhitelist().split(OpenPlatformConstants.IP_WHITELIST_SEPARATOR);
            boolean allowed = false;
            for (String ip : whitelist) {
                if (ip.trim().equals(requestIp)) {
                    allowed = true;
                    break;
                }
            }
            if (!allowed) {
                throw new ApiException(GlobalResCodeConstants.FORBIDDEN.getCode(), "IP不在白名单中");
            }
        }
    }

    /**
     * 验证请求时间（防重放攻击）
     */
    private void validateRequestTime(String reqTime) {
        try {
            long requestTimestamp = DateUtil.parse(reqTime, OpenPlatformConstants.REQUEST_TIME_PATTERN).getTime();
            long currentTimestamp = System.currentTimeMillis();
            long deviation = Math.abs(currentTimestamp - requestTimestamp) / 1000;
            
            if (deviation > OpenPlatformConstants.MAX_TIME_DEVIATION_SECONDS) {
                throw new ApiException(GlobalResCodeConstants.BAD_REQUEST.getCode(), 
                    "请求时间偏差过大，请检查系统时间");
            }
        } catch (Exception e) {
            throw new ApiException(GlobalResCodeConstants.BAD_REQUEST.getCode(), "请求时间格式错误");
        }
    }

    /**
     * 验证签名
     */
    private void validateSignature(BaseOpenRequest<?> request, String publicKey) {
        try {
            String reqData = SignatureUtil.buildSignData(request);
            
            boolean success = DigitalSignatureUtil.verifySignature(
                reqData, request.getSign(), Base64.decodeBase64(publicKey));
            
            if (!success) {
                throw new ApiException(GlobalResCodeConstants.SIGN_ERROR);
            }
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("签名验证失败: {}", e.getMessage(), e);
            throw new ApiException(GlobalResCodeConstants.SIGN_ERROR);
        }
    }

    /**
     * 防重放攻击检查
     */
    private void checkReplayAttack(String transId) {
        String key = CacheKeyBuilder.buildReplayKey(transId);
        Boolean exists = replayAttackCache.getIfPresent(key);
        if (Boolean.TRUE.equals(exists)) {
            throw new ApiException(GlobalResCodeConstants.BAD_REQUEST.getCode(), "重复请求");
        }
        // 设置缓存，防止重放攻击
        replayAttackCache.put(key, Boolean.TRUE);
    }

    /**
     * 限流检查
     * 注意：基于 Caffeine 的本地缓存限流，仅适用于单机限流场景
     * 在分布式环境下，每个节点的限流是独立的
     */
    private void checkRateLimit(String appCode, String apiCode, Long qpsLimit) {
        if (qpsLimit == null || qpsLimit <= 0) {
            return; // 不限流
        }
        
        String key = CacheKeyBuilder.buildRateLimitKey(appCode, apiCode);
        
        // 使用 computeIfAbsent 确保线程安全地获取或创建计数器
        AtomicLong count = rateLimitCache.asMap().computeIfAbsent(key, k -> new AtomicLong(0));
        
        long currentCount = count.incrementAndGet();
        if (currentCount > qpsLimit) {
            throw new ApiException(GlobalResCodeConstants.LIMIT_REQUEST);
        }
    }

    /**
     * 读取请求体
     */
    private String readRequestBody(HttpServletRequest request) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
        return sb.toString();
    }

    /**
     * 获取客户端IP
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (StrUtil.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (StrUtil.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 处理多级代理的情况
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}

