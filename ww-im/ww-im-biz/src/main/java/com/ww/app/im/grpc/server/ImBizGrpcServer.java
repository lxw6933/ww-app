package com.ww.app.im.grpc.server;

import com.google.protobuf.Empty;
import com.ww.app.im.core.api.common.ImMsgBody;
import com.ww.app.im.service.MsgService;
import com.ww.app.proto.im.ImMsgBodyRequest;
import com.ww.app.proto.im.biz.ImMsgBizServiceGrpc;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;

/**
 * @author ww
 * @create 2024-12-28 16:34
 * @description: im 业务消息处理 grpc
 */
@Slf4j
//@GrpcService
public class ImBizGrpcServer extends ImMsgBizServiceGrpc.ImMsgBizServiceImplBase {

    @Resource
    private MsgService msgService;

    @Override
    public void handleImMsg(ImMsgBodyRequest request, StreamObserver<Empty> responseObserver) {
        ImMsgBody imMsgBody = ImMsgBody.build(request);
        log.info("im msg biz grpc 参数：{}", imMsgBody);
        msgService.handleImMsg(imMsgBody);
        // 通知client服务端处理完了
        responseObserver.onCompleted();
    }
}
