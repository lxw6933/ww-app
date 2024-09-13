package com.ww.mall.web.interceptor.grpc.client;

import com.ww.mall.common.constant.Constant;
import io.grpc.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/**
 * @author ww
 * @create 2024-05-22 23:18
 * @description:
 */
@Slf4j
@Component
public class GrpcAuthInterceptor implements ServerInterceptor {

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> serverCall,
                                                                 Metadata metadata,
                                                                 ServerCallHandler<ReqT, RespT> serverCallHandler) {
        String token = metadata.get(Metadata.Key.of(Constant.USER_TOKEN_KEY, Metadata.ASCII_STRING_MARSHALLER));

        if (StringUtils.isBlank(token)) {
            serverCall.close(Status.UNAUTHENTICATED, null);
        }

        return null;
    }
}

