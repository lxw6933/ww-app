package com.ww.app.gateway.auth;

import cn.hutool.core.date.DateUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.jwt.JWT;
import cn.hutool.jwt.JWTUtil;
import com.ww.app.common.constant.Constant;
import com.ww.app.common.enums.GlobalResCodeConstants;
import com.ww.app.common.utils.CaffeineUtil;
import com.ww.app.common.enums.UserType;
import com.ww.app.gateway.properties.AppGatewayProperties;
import lombok.extern.slf4j.Slf4j;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Expiry;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RefreshScope
public class JwtTokenService {

    @Value("${jwt.secret}")
    public String jwtSecret = Constant.SECRET_KEY;

    private final AppGatewayProperties appGatewayProperties;
    private final Cache<String, CachedToken> tokenCache;

    public JwtTokenService(AppGatewayProperties appGatewayProperties) {
        this.appGatewayProperties = appGatewayProperties;
        int cacheSize = resolveCacheSize(appGatewayProperties.getJwtCacheMaxSize());
        this.tokenCache = CaffeineUtil.createCacheWithExpiry(cacheSize, cacheSize, new Expiry<String, CachedToken>() {
            @Override
            public long expireAfterCreate(@NonNull String key, @NonNull CachedToken value, long currentTime) {
                return expireAfter(value);
            }

            @Override
            public long expireAfterUpdate(@NonNull String key, @NonNull CachedToken value, long currentTime, long currentDuration) {
                return expireAfter(value);
            }

            @Override
            public long expireAfterRead(@NonNull String key, @NonNull CachedToken value, long currentTime, long currentDuration) {
                return currentDuration;
            }
        });
    }

    public TokenResult resolve(String token) {
        CachedToken cachedToken = getCachedToken(token);
        if (cachedToken != null) {
            return TokenResult.ok(cachedToken.tokenInfo, cachedToken.userType);
        }
        // Verify signature before parsing claims.
        boolean verify = JWTUtil.verify(token, jwtSecret.getBytes());
        if (!verify) {
            return TokenResult.error(GlobalResCodeConstants.TOKEN_ERROR, HttpStatus.UNAUTHORIZED);
        }
        JWT jwt = JWTUtil.parseToken(token);
        JSONObject claimsJson = jwt.getPayload().getClaimsJson();
        Long expSeconds = claimsJson.get("exp", Long.class);
        if (expSeconds == null || DateUtil.date().after(new Date(expSeconds))) {
            return TokenResult.error(GlobalResCodeConstants.TOKEN_TIMEOUT, HttpStatus.UNAUTHORIZED);
        }
        String tokenInfo;
        String userType;
        switch (claimsJson.get(Constant.USER_TYPE, UserType.class)) {
            case ADMIN:
                userType = UserType.ADMIN.name();
                tokenInfo = claimsJson.toString();
                break;
            case CLIENT:
                userType = UserType.CLIENT.name();
                tokenInfo = claimsJson.toString();
                break;
            case OTHER:
                log.error("token 用户类型为其他，系统暂不支持");
                return TokenResult.error(GlobalResCodeConstants.UNAUTHORIZED, HttpStatus.UNAUTHORIZED);
            default:
                log.error("token 用户类型异常");
                return TokenResult.error(GlobalResCodeConstants.UNAUTHORIZED, HttpStatus.UNAUTHORIZED);
        }
        cacheToken(token, tokenInfo, userType, expSeconds);
        return TokenResult.ok(tokenInfo, userType);
    }

    private CachedToken getCachedToken(String token) {
        if (!Boolean.TRUE.equals(appGatewayProperties.getJwtCacheEnabled())) {
            return null;
        }
        CachedToken cached = tokenCache.getIfPresent(token);
        if (cached == null) {
            return null;
        }
        long now = System.currentTimeMillis();
        if (cached.expireAtMillis - getSkewMillis() <= now) {
            tokenCache.invalidate(token);
            return null;
        }
        return cached;
    }

    private void cacheToken(String token, String tokenInfo, String userType, Long expSeconds) {
        if (!Boolean.TRUE.equals(appGatewayProperties.getJwtCacheEnabled())) {
            return;
        }
        if (expSeconds == null) {
            return;
        }
        CachedToken cachedToken = new CachedToken(tokenInfo, userType, expSeconds);
        tokenCache.put(token, cachedToken);
    }

    private static class CachedToken {
        private final String tokenInfo;
        private final String userType;
        private final long expireAtMillis;

        private CachedToken(String tokenInfo, String userType, long expireAtMillis) {
            this.tokenInfo = tokenInfo;
            this.userType = userType;
            this.expireAtMillis = expireAtMillis;
        }
    }

    private long expireAfter(CachedToken cachedToken) {
        long nowMillis = System.currentTimeMillis();
        long ttlMillis = cachedToken.expireAtMillis - getSkewMillis() - nowMillis;
        if (ttlMillis <= 0) {
            return 0;
        }
        return TimeUnit.MILLISECONDS.toNanos(ttlMillis);
    }

    private long getSkewMillis() {
        Integer skewSeconds = appGatewayProperties.getJwtCacheSkewSeconds();
        int skew = skewSeconds == null ? 0 : skewSeconds;
        return skew * 1000L;
    }

    private int resolveCacheSize(Integer maxSize) {
        if (maxSize == null || maxSize <= 0) {
            return 1000;
        }
        return maxSize;
    }
}
