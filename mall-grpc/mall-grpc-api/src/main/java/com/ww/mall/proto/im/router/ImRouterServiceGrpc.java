package com.ww.mall.proto.im.router;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.63.0)",
    comments = "Source: im/imRouter.proto")
@io.grpc.stub.annotations.GrpcGenerated
public final class ImRouterServiceGrpc {

  private ImRouterServiceGrpc() {}

  public static final java.lang.String SERVICE_NAME = "ImRouterService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<com.ww.mall.proto.im.ImMsgBodyRequest,
      com.google.protobuf.Empty> getRouteMsgMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "routeMsg",
      requestType = com.ww.mall.proto.im.ImMsgBodyRequest.class,
      responseType = com.google.protobuf.Empty.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.ww.mall.proto.im.ImMsgBodyRequest,
      com.google.protobuf.Empty> getRouteMsgMethod() {
    io.grpc.MethodDescriptor<com.ww.mall.proto.im.ImMsgBodyRequest, com.google.protobuf.Empty> getRouteMsgMethod;
    if ((getRouteMsgMethod = ImRouterServiceGrpc.getRouteMsgMethod) == null) {
      synchronized (ImRouterServiceGrpc.class) {
        if ((getRouteMsgMethod = ImRouterServiceGrpc.getRouteMsgMethod) == null) {
          ImRouterServiceGrpc.getRouteMsgMethod = getRouteMsgMethod =
              io.grpc.MethodDescriptor.<com.ww.mall.proto.im.ImMsgBodyRequest, com.google.protobuf.Empty>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "routeMsg"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.ww.mall.proto.im.ImMsgBodyRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.Empty.getDefaultInstance()))
              .setSchemaDescriptor(new ImRouterServiceMethodDescriptorSupplier("routeMsg"))
              .build();
        }
      }
    }
    return getRouteMsgMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.ww.mall.proto.im.ImMsgBodyListRequest,
      com.google.protobuf.Empty> getBatchRouteMsgMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "batchRouteMsg",
      requestType = com.ww.mall.proto.im.ImMsgBodyListRequest.class,
      responseType = com.google.protobuf.Empty.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.ww.mall.proto.im.ImMsgBodyListRequest,
      com.google.protobuf.Empty> getBatchRouteMsgMethod() {
    io.grpc.MethodDescriptor<com.ww.mall.proto.im.ImMsgBodyListRequest, com.google.protobuf.Empty> getBatchRouteMsgMethod;
    if ((getBatchRouteMsgMethod = ImRouterServiceGrpc.getBatchRouteMsgMethod) == null) {
      synchronized (ImRouterServiceGrpc.class) {
        if ((getBatchRouteMsgMethod = ImRouterServiceGrpc.getBatchRouteMsgMethod) == null) {
          ImRouterServiceGrpc.getBatchRouteMsgMethod = getBatchRouteMsgMethod =
              io.grpc.MethodDescriptor.<com.ww.mall.proto.im.ImMsgBodyListRequest, com.google.protobuf.Empty>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "batchRouteMsg"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.ww.mall.proto.im.ImMsgBodyListRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.Empty.getDefaultInstance()))
              .setSchemaDescriptor(new ImRouterServiceMethodDescriptorSupplier("batchRouteMsg"))
              .build();
        }
      }
    }
    return getBatchRouteMsgMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static ImRouterServiceStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<ImRouterServiceStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<ImRouterServiceStub>() {
        @java.lang.Override
        public ImRouterServiceStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new ImRouterServiceStub(channel, callOptions);
        }
      };
    return ImRouterServiceStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static ImRouterServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<ImRouterServiceBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<ImRouterServiceBlockingStub>() {
        @java.lang.Override
        public ImRouterServiceBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new ImRouterServiceBlockingStub(channel, callOptions);
        }
      };
    return ImRouterServiceBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static ImRouterServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<ImRouterServiceFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<ImRouterServiceFutureStub>() {
        @java.lang.Override
        public ImRouterServiceFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new ImRouterServiceFutureStub(channel, callOptions);
        }
      };
    return ImRouterServiceFutureStub.newStub(factory, channel);
  }

  /**
   */
  public interface AsyncService {

    /**
     * <pre>
     * 路由消息给IM server
     * </pre>
     */
    default void routeMsg(com.ww.mall.proto.im.ImMsgBodyRequest request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getRouteMsgMethod(), responseObserver);
    }

    /**
     * <pre>
     * 批量路由消息给IM server
     * </pre>
     */
    default void batchRouteMsg(com.ww.mall.proto.im.ImMsgBodyListRequest request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getBatchRouteMsgMethod(), responseObserver);
    }
  }

  /**
   * Base class for the server implementation of the service ImRouterService.
   */
  public static abstract class ImRouterServiceImplBase
      implements io.grpc.BindableService, AsyncService {

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return ImRouterServiceGrpc.bindService(this);
    }
  }

  /**
   * A stub to allow clients to do asynchronous rpc calls to service ImRouterService.
   */
  public static final class ImRouterServiceStub
      extends io.grpc.stub.AbstractAsyncStub<ImRouterServiceStub> {
    private ImRouterServiceStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected ImRouterServiceStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new ImRouterServiceStub(channel, callOptions);
    }

    /**
     * <pre>
     * 路由消息给IM server
     * </pre>
     */
    public void routeMsg(com.ww.mall.proto.im.ImMsgBodyRequest request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getRouteMsgMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * 批量路由消息给IM server
     * </pre>
     */
    public void batchRouteMsg(com.ww.mall.proto.im.ImMsgBodyListRequest request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getBatchRouteMsgMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * A stub to allow clients to do synchronous rpc calls to service ImRouterService.
   */
  public static final class ImRouterServiceBlockingStub
      extends io.grpc.stub.AbstractBlockingStub<ImRouterServiceBlockingStub> {
    private ImRouterServiceBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected ImRouterServiceBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new ImRouterServiceBlockingStub(channel, callOptions);
    }

    /**
     * <pre>
     * 路由消息给IM server
     * </pre>
     */
    public com.google.protobuf.Empty routeMsg(com.ww.mall.proto.im.ImMsgBodyRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getRouteMsgMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * 批量路由消息给IM server
     * </pre>
     */
    public com.google.protobuf.Empty batchRouteMsg(com.ww.mall.proto.im.ImMsgBodyListRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getBatchRouteMsgMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do ListenableFuture-style rpc calls to service ImRouterService.
   */
  public static final class ImRouterServiceFutureStub
      extends io.grpc.stub.AbstractFutureStub<ImRouterServiceFutureStub> {
    private ImRouterServiceFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected ImRouterServiceFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new ImRouterServiceFutureStub(channel, callOptions);
    }

    /**
     * <pre>
     * 路由消息给IM server
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.google.protobuf.Empty> routeMsg(
        com.ww.mall.proto.im.ImMsgBodyRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getRouteMsgMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * 批量路由消息给IM server
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.google.protobuf.Empty> batchRouteMsg(
        com.ww.mall.proto.im.ImMsgBodyListRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getBatchRouteMsgMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_ROUTE_MSG = 0;
  private static final int METHODID_BATCH_ROUTE_MSG = 1;

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
        case METHODID_ROUTE_MSG:
          serviceImpl.routeMsg((com.ww.mall.proto.im.ImMsgBodyRequest) request,
              (io.grpc.stub.StreamObserver<com.google.protobuf.Empty>) responseObserver);
          break;
        case METHODID_BATCH_ROUTE_MSG:
          serviceImpl.batchRouteMsg((com.ww.mall.proto.im.ImMsgBodyListRequest) request,
              (io.grpc.stub.StreamObserver<com.google.protobuf.Empty>) responseObserver);
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
          getRouteMsgMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.ww.mall.proto.im.ImMsgBodyRequest,
              com.google.protobuf.Empty>(
                service, METHODID_ROUTE_MSG)))
        .addMethod(
          getBatchRouteMsgMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.ww.mall.proto.im.ImMsgBodyListRequest,
              com.google.protobuf.Empty>(
                service, METHODID_BATCH_ROUTE_MSG)))
        .build();
  }

  private static abstract class ImRouterServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    ImRouterServiceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return com.ww.mall.proto.im.router.ImRouterProto.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("ImRouterService");
    }
  }

  private static final class ImRouterServiceFileDescriptorSupplier
      extends ImRouterServiceBaseDescriptorSupplier {
    ImRouterServiceFileDescriptorSupplier() {}
  }

  private static final class ImRouterServiceMethodDescriptorSupplier
      extends ImRouterServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final java.lang.String methodName;

    ImRouterServiceMethodDescriptorSupplier(java.lang.String methodName) {
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
      synchronized (ImRouterServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new ImRouterServiceFileDescriptorSupplier())
              .addMethod(getRouteMsgMethod())
              .addMethod(getBatchRouteMsgMethod())
              .build();
        }
      }
    }
    return result;
  }
}
