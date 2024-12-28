package com.ww.mall.im.grpc.client;

import com.ww.mall.proto.im.core.ImServerServiceGrpc;
import lombok.Getter;

/**
 * @author ww
 * @create 2024-12-28 16:59
 * @description:
 */
@Getter
//@Service
public class GrpcClientService {

//    @GrpcClient("mall-im-core")
    public ImServerServiceGrpc.ImServerServiceBlockingStub imServerServiceBlockingStub;

}
