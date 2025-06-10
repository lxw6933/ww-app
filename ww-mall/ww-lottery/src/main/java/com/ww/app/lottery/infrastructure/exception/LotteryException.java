package com.ww.app.lottery.infrastructure.exception;

import com.ww.app.common.exception.ApiException;
import com.ww.app.lottery.domain.result.LotteryResult;

/**
 * @author ww
 * @create 2025-06-10- 11:39
 * @description:
 */
public class LotteryException extends ApiException {

    public LotteryException(String message) {
        super(message);
    }

    public LotteryException(LotteryResult.ResultCode resultCode) {
        super(resultCode.getCode(), resultCode.getMessage());
    }

}
