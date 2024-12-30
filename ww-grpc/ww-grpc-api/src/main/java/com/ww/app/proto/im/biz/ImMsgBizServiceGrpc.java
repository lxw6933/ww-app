package com.ww.app.proto.im.biz;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.63.0)",
    comments = "Source: im/imMsgBiz.proto")
@io.grpc.stub.annotations.GrpcGenerated
public final class ImMsgBizServiceGrpc {

  private ImMsgBizServiceGrpc() {}

  public static final java.lang.String SERVICE_NAME = "ImMsgBizService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<com.ww.app.proto.im.ImMsgBodyRequest,
      com.google.protobuf.Empty> getHandleImMsgMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "handleImMsg",
      requestType = com.ww.app.proto.im.ImMsgBodyRequest.class,
      responseType = com.google.protobuf.Empty.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.ww.app.proto.im.ImMsgBodyRequest,
      com.google.protobuf.Empty> getHandleImMsgMethod() {
    io.grpc.MethodDescriptor<com.ww.app.proto.im.ImMsgBodyRequest, com.google.protobuf.Empty> getHandleImMsgMethod;
    if ((getHandleImMsgMethod = ImMsgBizServiceGrpc.getHandleImMsgMethod) == null) {
      synchronized (ImMsgBizServiceGrpc.class) {
        if ((getHandleImMsgMethod = ImMsgBizServiceGrpc.getHandleImMsgMethod) == null) {
          ImMsgBizServiceGrpc.getHandleImMsgMethod = getHandleImMsgMethod =
              io.grpc.MethodDescriptor.<com.ww.app.proto.im.ImMsgBodyRequest, com.google.protobuf.Empty>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "handleImMsg"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.ww.app.proto.im.ImMsgBodyRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.Empty.getDefaultInstance()))
              .setSchemaDescriptor(new ImMsgBizServiceMethodDescriptorSupplier("handleImMsg"))
              .build();
        }
      }
    }
    return getHandleImMsgMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static ImMsgBizServiceStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<ImMsgBizServiceStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<ImMsgBizServiceStub>() {
        @java.lang.Override
        public ImMsgBizServiceStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new ImMsgBizServiceStub(channel, callOptions);
        }
      };
    return ImMsgBizServiceStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static ImMsgBizServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<ImMsgBizServiceBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<ImMsgBizServiceBlockingStub>() {
        @java.lang.Override
        public ImMsgBizServiceBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new ImMsgBizServiceBlockingStub(channel, callOptions);
        }
      };
    return ImMsgBizServiceBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static ImMsgBizServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<ImMsgBizServiceFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<ImMsgBizServiceFutureStub>() {
        @java.lang.Override
        public ImMsgBizServiceFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new ImMsgBizServiceFutureStub(channel, callOptions);
        }
      };
    return ImMsgBizServiceFutureStub.newStub(factory, channel);
  }

  /**
   */
  public interface AsyncService {

    /**
     * <pre>
     * 简单rpc 请求消息：ImMsgBodyRequest 响应消息：void
     * 处理业务消息
     * </pre>
     */
    default void handleImMsg(com.ww.app.proto.im.ImMsgBodyRequest request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getHandleImMsgMethod(), responseObserver);
    }
  }

  /**
   * Base class for the server implementation of the service ImMsgBizService.
   */
  public static abstract class ImMsgBizServiceImplBase
      implements io.grpc.BindableService, AsyncService {

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return ImMsgBizServiceGrpc.bindService(this);
    }
  }

  /**
   * A stub to allow clients to do asynchronous rpc calls to service ImMsgBizService.
   */
  public static final class ImMsgBizServiceStub
      extends io.grpc.stub.AbstractAsyncStub<ImMsgBizServiceStub> {
    private ImMsgBizServiceStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected ImMsgBizServiceStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new ImMsgBizServiceStub(channel, callOptions);
    }

    /**
     * <pre>
     * 简单rpc 请求消息：ImMsgBodyRequest 响应消息：void
     * 处理业务消息
     * </pre>
     */
    public void handleImMsg(com.ww.app.proto.im.ImMsgBodyRequest request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getHandleImMsgMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * A stub to allow clients to do synchronous rpc calls to service ImMsgBizService.
   */
  public static final class ImMsgBizServiceBlockingStub
      extends io.grpc.stub.AbstractBlockingStub<ImMsgBizServiceBlockingStub> {
    private ImMsgBizServiceBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected ImMsgBizServiceBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new ImMsgBizServiceBlockingStub(channel, callOptions);
    }

    /**
     * <pre>
     * 简单rpc 请求消息：ImMsgBodyRequest 响应消息：void
     * 处理业务消息
     * </pre>
     */
    public com.google.protobuf.Empty handleImMsg(com.ww.app.proto.im.ImMsgBodyRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getHandleImMsgMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do ListenableFuture-style rpc calls to service ImMsgBizService.
   */
  public static final class ImMsgBizServiceFutureStub
      extends io.grpc.stub.AbstractFutureStub<ImMsgBizServiceFutureStub> {
    private ImMsgBizServiceFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected ImMsgBizServiceFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new ImMsgBizServiceFutureStub(channel, callOptions);
    }

    /**
     * <pre>
     * 简单rpc 请求消息：ImMsgBodyRequest 响应消息：void
     * 处理业务消息
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.google.protobuf.Empty> handleImMsg(
        com.ww.app.proto.im.ImMsgBodyRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getHandleImMsgMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_HANDLE_IM_MSG = 0;

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
        case METHODID_HANDLE_IM_MSG:
          serviceImpl.handleImMsg((com.ww.app.proto.im.ImMsgBodyRequest) request,
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
          getHandleImMsgMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.ww.app.proto.im.ImMsgBodyRequest,
              com.google.protobuf.Empty>(
                service, METHODID_HANDLE_IM_MSG)))
        .build();
  }

  private static abstract class ImMsgBizServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    ImMsgBizServiceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return com.ww.app.proto.im.biz.ImMsgBizProto.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("ImMsgBizService");
    }
  }

  private static final class ImMsgBizServiceFileDescriptorSupplier
      extends ImMsgBizServiceBaseDescriptorSupplier {
    ImMsgBizServiceFileDescriptorSupplier() {}
  }

  private static final class ImMsgBizServiceMethodDescriptorSupplier
      extends ImMsgBizServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final java.lang.String methodName;

    ImMsgBizServiceMethodDescriptorSupplier(java.lang.String methodName) {
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
      synchronized (ImMsgBizServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new ImMsgBizServiceFileDescriptorSupplier())
              .addMethod(getHandleImMsgMethod())
              .build();
        }
      }
    }
    return result;
  }
}
