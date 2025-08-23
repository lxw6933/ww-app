package com.ww.app.gateway.utils;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.server.reactive.ServerHttpRequest;

public class BearerTokenExtractor {

    private final static Logger logger = LoggerFactory.getLogger(BearerTokenExtractor.class);

    public static final String BEARER_TYPE = "Bearer";

    /**
     * The access token issued by the authorization server. This value is REQUIRED.
     */
    public static final String ACCESS_TOKEN = "access_token";

    /**
     * extract token from HttpServletRequest
     *
     * @param request httpServletRequest
     * @return return token if exists, otherwise null
     */
    public String extractToken(ServerHttpRequest request) {
        // first check the header...
        String token = extractHeaderToken(request);

        // bearer type allows a request parameter as well
        if (token == null) {
            logger.debug("Token not found in headers. Trying request parameters.");
            token = request.getQueryParams().getFirst(ACCESS_TOKEN);
            if (token == null) {
                logger.debug("Token not found in request parameters.  Not an OAuth2 request.");
            }
        }

        return token;
    }

    /**
     * Extract the OAuth bearer token from a header.
     *
     * @param request The request.
     * @return The token, or null if no OAuth authorization header was supplied.
     */
    protected String extractHeaderToken(ServerHttpRequest request) {
        String authorization = request.getHeaders().getFirst("Authorization");
        if (StringUtils.isNotBlank(authorization) && authorization.toLowerCase().startsWith(BEARER_TYPE.toLowerCase())) {
            String authHeaderValue = authorization.substring(BEARER_TYPE.length()).trim();
            int commaIndex = authHeaderValue.indexOf(',');
            if (commaIndex > 0) {
                authHeaderValue = authHeaderValue.substring(0, commaIndex);
            }
            return authHeaderValue;
        }
        return null;
    }

}
