package com.ww.app.gateway.auth;

import com.ww.app.common.common.ResCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public final class TokenResult {
    private final String tokenInfo;
    private final String userType;
    private final ResCode errorCode;
    private final HttpStatus httpStatus;

    private TokenResult(String tokenInfo, String userType, ResCode errorCode, HttpStatus httpStatus) {
        this.tokenInfo = tokenInfo;
        this.userType = userType;
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    public static TokenResult ok(String tokenInfo, String userType) {
        return new TokenResult(tokenInfo, userType, null, null);
    }

    public static TokenResult error(ResCode errorCode, HttpStatus httpStatus) {
        return new TokenResult(null, null, errorCode, httpStatus);
    }

    public boolean isError() {
        return errorCode != null;
    }

}
