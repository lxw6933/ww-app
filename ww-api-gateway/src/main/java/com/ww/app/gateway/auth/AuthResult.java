package com.ww.app.gateway.auth;

import com.ww.app.common.common.ResCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public final class AuthResult {
    private final boolean allowed;
    private final String tokenInfo;
    private final String userType;
    private final ResCode errorCode;
    private final HttpStatus httpStatus;

    private AuthResult(boolean allowed, String tokenInfo, String userType, ResCode errorCode, HttpStatus httpStatus) {
        this.allowed = allowed;
        this.tokenInfo = tokenInfo;
        this.userType = userType;
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    public static AuthResult ok(String tokenInfo, String userType) {
        return new AuthResult(true, tokenInfo, userType, null, null);
    }

    public static AuthResult deny(ResCode errorCode, HttpStatus httpStatus) {
        return new AuthResult(false, null, null, errorCode, httpStatus);
    }

}
