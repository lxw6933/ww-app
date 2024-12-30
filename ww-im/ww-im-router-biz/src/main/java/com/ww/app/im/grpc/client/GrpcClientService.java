package com.ww.app.im.grpc.client;

import com.ww.app.proto.im.core.ImServerServiceGrpc;
import lombok.Getter;

/**
 * @author ww
 * @create 2024-12-28 16:59
 * @description:
 */
@Getter
//@Service
public class GrpcClientService {

//    @GrpcClient("ww-im-core")
    public ImServerServiceGrpc.ImServerServiceBlockingStub imServerServiceBlockingStub;

}
