package com.ww.app.proto.im.core;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.63.0)",
    comments = "Source: im/imServer.proto")
@io.grpc.stub.annotations.GrpcGenerated
public final class ImServerServiceGrpc {

  private ImServerServiceGrpc() {}

  public static final java.lang.String SERVICE_NAME = "ImServerService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<com.ww.app.proto.im.ImMsgBodyRequest,
      com.ww.app.proto.im.ImCommonResponse> getSendMsgMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "sendMsg",
      requestType = com.ww.app.proto.im.ImMsgBodyRequest.class,
      responseType = com.ww.app.proto.im.ImCommonResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.ww.app.proto.im.ImMsgBodyRequest,
      com.ww.app.proto.im.ImCommonResponse> getSendMsgMethod() {
    io.grpc.MethodDescriptor<com.ww.app.proto.im.ImMsgBodyRequest, com.ww.app.proto.im.ImCommonResponse> getSendMsgMethod;
    if ((getSendMsgMethod = ImServerServiceGrpc.getSendMsgMethod) == null) {
      synchronized (ImServerServiceGrpc.class) {
        if ((getSendMsgMethod = ImServerServiceGrpc.getSendMsgMethod) == null) {
          ImServerServiceGrpc.getSendMsgMethod = getSendMsgMethod =
              io.grpc.MethodDescriptor.<com.ww.app.proto.im.ImMsgBodyRequest, com.ww.app.proto.im.ImCommonResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "sendMsg"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.ww.app.proto.im.ImMsgBodyRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.ww.app.proto.im.ImCommonResponse.getDefaultInstance()))
              .setSchemaDescriptor(new ImServerServiceMethodDescriptorSupplier("sendMsg"))
              .build();
        }
      }
    }
    return getSendMsgMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.ww.app.proto.im.ImMsgBodyListRequest,
      com.ww.app.proto.im.ImCommonResponse> getBatchSendMsgMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "batchSendMsg",
      requestType = com.ww.app.proto.im.ImMsgBodyListRequest.class,
      responseType = com.ww.app.proto.im.ImCommonResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.ww.app.proto.im.ImMsgBodyListRequest,
      com.ww.app.proto.im.ImCommonResponse> getBatchSendMsgMethod() {
    io.grpc.MethodDescriptor<com.ww.app.proto.im.ImMsgBodyListRequest, com.ww.app.proto.im.ImCommonResponse> getBatchSendMsgMethod;
    if ((getBatchSendMsgMethod = ImServerServiceGrpc.getBatchSendMsgMethod) == null) {
      synchronized (ImServerServiceGrpc.class) {
        if ((getBatchSendMsgMethod = ImServerServiceGrpc.getBatchSendMsgMethod) == null) {
          ImServerServiceGrpc.getBatchSendMsgMethod = getBatchSendMsgMethod =
              io.grpc.MethodDescriptor.<com.ww.app.proto.im.ImMsgBodyListRequest, com.ww.app.proto.im.ImCommonResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "batchSendMsg"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.ww.app.proto.im.ImMsgBodyListRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.ww.app.proto.im.ImCommonResponse.getDefaultInstance()))
              .setSchemaDescriptor(new ImServerServiceMethodDescriptorSupplier("batchSendMsg"))
              .build();
        }
      }
    }
    return getBatchSendMsgMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static ImServerServiceStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<ImServerServiceStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<ImServerServiceStub>() {
        @java.lang.Override
        public ImServerServiceStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new ImServerServiceStub(channel, callOptions);
        }
      };
    return ImServerServiceStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static ImServerServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<ImServerServiceBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<ImServerServiceBlockingStub>() {
        @java.lang.Override
        public ImServerServiceBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new ImServerServiceBlockingStub(channel, callOptions);
        }
      };
    return ImServerServiceBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static ImServerServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<ImServerServiceFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<ImServerServiceFutureStub>() {
        @java.lang.Override
        public ImServerServiceFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new ImServerServiceFutureStub(channel, callOptions);
        }
      };
    return ImServerServiceFutureStub.newStub(factory, channel);
  }

  /**
   */
  public interface AsyncService {

    /**
     * <pre>
     * 发送消息给客户端
     * </pre>
     */
    default void sendMsg(com.ww.app.proto.im.ImMsgBodyRequest request,
        io.grpc.stub.StreamObserver<com.ww.app.proto.im.ImCommonResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getSendMsgMethod(), responseObserver);
    }

    /**
     * <pre>
     * 批量发送消息给客户端
     * </pre>
     */
    default void batchSendMsg(com.ww.app.proto.im.ImMsgBodyListRequest request,
        io.grpc.stub.StreamObserver<com.ww.app.proto.im.ImCommonResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getBatchSendMsgMethod(), responseObserver);
    }
  }

  /**
   * Base class for the server implementation of the service ImServerService.
   */
  public static abstract class ImServerServiceImplBase
      implements io.grpc.BindableService, AsyncService {

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return ImServerServiceGrpc.bindService(this);
    }
  }

  /**
   * A stub to allow clients to do asynchronous rpc calls to service ImServerService.
   */
  public static final class ImServerServiceStub
      extends io.grpc.stub.AbstractAsyncStub<ImServerServiceStub> {
    private ImServerServiceStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected ImServerServiceStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new ImServerServiceStub(channel, callOptions);
    }

    /**
     * <pre>
     * 发送消息给客户端
     * </pre>
     */
    public void sendMsg(com.ww.app.proto.im.ImMsgBodyRequest request,
        io.grpc.stub.StreamObserver<com.ww.app.proto.im.ImCommonResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getSendMsgMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * 批量发送消息给客户端
     * </pre>
     */
    public void batchSendMsg(com.ww.app.proto.im.ImMsgBodyListRequest request,
        io.grpc.stub.StreamObserver<com.ww.app.proto.im.ImCommonResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getBatchSendMsgMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * A stub to allow clients to do synchronous rpc calls to service ImServerService.
   */
  public static final class ImServerServiceBlockingStub
      extends io.grpc.stub.AbstractBlockingStub<ImServerServiceBlockingStub> {
    private ImServerServiceBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected ImServerServiceBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new ImServerServiceBlockingStub(channel, callOptions);
    }

    /**
     * <pre>
     * 发送消息给客户端
     * </pre>
     */
    public com.ww.app.proto.im.ImCommonResponse sendMsg(com.ww.app.proto.im.ImMsgBodyRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getSendMsgMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * 批量发送消息给客户端
     * </pre>
     */
    public com.ww.app.proto.im.ImCommonResponse batchSendMsg(com.ww.app.proto.im.ImMsgBodyListRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getBatchSendMsgMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do ListenableFuture-style rpc calls to service ImServerService.
   */
  public static final class ImServerServiceFutureStub
      extends io.grpc.stub.AbstractFutureStub<ImServerServiceFutureStub> {
    private ImServerServiceFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected ImServerServiceFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new ImServerServiceFutureStub(channel, callOptions);
    }

    /**
     * <pre>
     * 发送消息给客户端
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.ww.app.proto.im.ImCommonResponse> sendMsg(
        com.ww.app.proto.im.ImMsgBodyRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getSendMsgMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * 批量发送消息给客户端
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.ww.app.proto.im.ImCommonResponse> batchSendMsg(
        com.ww.app.proto.im.ImMsgBodyListRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getBatchSendMsgMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_SEND_MSG = 0;
  private static final int METHODID_BATCH_SEND_MSG = 1;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final AsyncService serviceImpl;
    private final int methodId;

    MethodHandlers(AsyncService serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_SEND_MSG:
          serviceImpl.sendMsg((com.ww.app.proto.im.ImMsgBodyRequest) request,
              (io.grpc.stub.StreamObserver<com.ww.app.proto.im.ImCommonResponse>) responseObserver);
          break;
        case METHODID_BATCH_SEND_MSG:
          serviceImpl.batchSendMsg((com.ww.app.proto.im.ImMsgBodyListRequest) request,
              (io.grpc.stub.StreamObserver<com.ww.app.proto.im.ImCommonResponse>) responseObserver);
          break;
        default:
          throw new AssertionError();
      }
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public io.grpc.stub.StreamObserver<Req> invoke(
        io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        default:
          throw new AssertionError();
      }
    }
  }

  public static final io.grpc.ServerServiceDefinition bindService(AsyncService service) {
    return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
        .addMethod(
          getSendMsgMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.ww.app.proto.im.ImMsgBodyRequest,
              com.ww.app.proto.im.ImCommonResponse>(
                service, METHODID_SEND_MSG)))
        .addMethod(
          getBatchSendMsgMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.ww.app.proto.im.ImMsgBodyListRequest,
              com.ww.app.proto.im.ImCommonResponse>(
                service, METHODID_BATCH_SEND_MSG)))
        .build();
  }

  private static abstract class ImServerServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    ImServerServiceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return com.ww.app.proto.im.core.ImMsgBizProto.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("ImServerService");
    }
  }

  private static final class ImServerServiceFileDescriptorSupplier
      extends ImServerServiceBaseDescriptorSupplier {
    ImServerServiceFileDescriptorSupplier() {}
  }

  private static final class ImServerServiceMethodDescriptorSupplier
      extends ImServerServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final java.lang.String methodName;

    ImServerServiceMethodDescriptorSupplier(java.lang.String methodName) {
      this.methodName = methodName;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.MethodDescriptor getMethodDescriptor() {
      return getServiceDescriptor().findMethodByName(methodName);
    }
  }

  private static volatile io.grpc.ServiceDescriptor serviceDescriptor;

  public static io.grpc.ServiceDescriptor getServiceDescriptor() {
    io.grpc.ServiceDescriptor result = serviceDescriptor;
    if (result == null) {
      synchronized (ImServerServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new ImServerServiceFileDescriptorSupplier())
              .addMethod(getSendMsgMethod())
              .addMethod(getBatchSendMsgMethod())
              .build();
        }
      }
    }
    return result;
  }
}
