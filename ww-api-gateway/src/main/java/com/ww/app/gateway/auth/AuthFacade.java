package com.ww.app.gateway.auth;

import com.ww.app.common.enums.GlobalResCodeConstants;
import com.ww.app.gateway.utils.BearerTokenExtractor;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;

@Component
public class AuthFacade {
    private final JwtTokenService jwtTokenService;
    private final WhiteListMatcher whiteListMatcher;
    private final BearerTokenExtractor tokenExtractor = new BearerTokenExtractor();

    public AuthFacade(JwtTokenService jwtTokenService, WhiteListMatcher whiteListMatcher) {
        this.jwtTokenService = jwtTokenService;
        this.whiteListMatcher = whiteListMatcher;
    }

    public AuthResult authorize(ServerHttpRequest request) {
        String token = tokenExtractor.extractToken(request);
        if (token == null) {
            // No token: allow only if URL is on the whitelist.
            String urlPath = request.getURI().getPath();
            if (!whiteListMatcher.isWhiteListed(urlPath)) {
                return AuthResult.deny(GlobalResCodeConstants.FORBIDDEN, HttpStatus.FORBIDDEN);
            }
            return AuthResult.ok(null, null);
        }
        // Token present: validate and map to user context.
        TokenResult tokenResult = jwtTokenService.resolve(token);
        if (tokenResult.isError()) {
            return AuthResult.deny(tokenResult.getErrorCode(), tokenResult.getHttpStatus());
        }
        return AuthResult.ok(tokenResult.getTokenInfo(), tokenResult.getUserType());
    }
}
