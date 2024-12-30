package com.ww.app.im.grpc.server;

import com.google.protobuf.Empty;
import com.ww.app.im.common.ImMsgBody;
import com.ww.app.im.service.ImRouterService;
import com.ww.app.proto.im.ImMsgBodyListRequest;
import com.ww.app.proto.im.ImMsgBodyRequest;
import com.ww.app.proto.im.router.ImRouterServiceGrpc;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * @author ww
 * @create 2024-12-28 16:34
 * @description: im 业务消息处理 grpc
 */
@Slf4j
//@GrpcService
public class ImRouterGrpcServer extends ImRouterServiceGrpc.ImRouterServiceImplBase {

    @Resource
    private ImRouterService imRouterService;

    @Override
    public void routeMsg(ImMsgBodyRequest request, StreamObserver<Empty> responseObserver) {
        ImMsgBody imMsgBody = ImMsgBody.build(request);
        log.info("im msg biz grpc 参数：{}", imMsgBody);
        imRouterService.sendMsg(imMsgBody);
        // 通知client服务端处理完了
        responseObserver.onCompleted();
    }

    @Override
    public void batchRouteMsg(ImMsgBodyListRequest request, StreamObserver<Empty> responseObserver) {
        List<ImMsgBody> imMsgBodyList = new ArrayList<>();
        request.getImMsgBodyListList().forEach(imMsgBodyRequest -> {
            ImMsgBody imMsgBody = ImMsgBody.build(imMsgBodyRequest);
            imMsgBodyList.add(imMsgBody);
            log.info("im router grpc 消息：[{}]", imMsgBody);
        });
        imRouterService.batchSendMsg(imMsgBodyList);
        // 通知client服务端处理完了
        responseObserver.onCompleted();
    }
}
