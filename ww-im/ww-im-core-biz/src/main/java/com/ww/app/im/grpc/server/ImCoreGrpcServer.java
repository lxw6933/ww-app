package com.ww.app.im.grpc.server;

import com.ww.app.im.core.api.common.ImMsgBody;
import com.ww.app.im.service.MsgRouterService;
import com.ww.app.proto.im.ImCommonResponse;
import com.ww.app.proto.im.ImMsgBodyListRequest;
import com.ww.app.proto.im.ImMsgBodyRequest;
import com.ww.app.proto.im.core.ImServerServiceGrpc;
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
public class ImCoreGrpcServer extends ImServerServiceGrpc.ImServerServiceImplBase {

    @Resource
    private MsgRouterService msgRouterService;

    @Override
    public void sendMsg(ImMsgBodyRequest request, StreamObserver<ImCommonResponse> responseObserver) {
        ImMsgBody imMsgBody = ImMsgBody.build(request);
        log.info("im server grpc 参数：{}", imMsgBody);
        boolean result = msgRouterService.sendMsgToClient(imMsgBody);
        ImCommonResponse.Builder response = ImCommonResponse.newBuilder();
        response.setSuccess(result);
        // 将响应消息通过网络回传给client
        responseObserver.onNext(response.build());
        // 通知client服务端处理完了
        responseObserver.onCompleted();
    }

    @Override
    public void batchSendMsg(ImMsgBodyListRequest request, StreamObserver<ImCommonResponse> responseObserver) {
        request.getImMsgBodyListList().forEach(imMsgBodyRequest -> {
            ImMsgBody imMsgBody = ImMsgBody.build(imMsgBodyRequest);
            boolean result = msgRouterService.sendMsgToClient(imMsgBody);
            log.info("im server grpc 消息：[{}] 发送结果: {}", imMsgBody, result);
        });
        ImCommonResponse.Builder response = ImCommonResponse.newBuilder();
        response.setSuccess(true);
        // 将响应消息通过网络回传给client
        responseObserver.onNext(response.build());
        // 通知client服务端处理完了
        responseObserver.onCompleted();
    }
}
