package com.ww.app.proto.hello;

import static io.grpc.MethodDescriptor.generateFullMethodName;

/**
 * <pre>
 *定义服务
 *1.简单rpc 【客户端一次请求，服务端相应一次，客户端阻塞等待服务端返回】
 *2.服务端流式rpc 【客户端一次请求，服务端多次响应】
 *3.客户端流式rpc 【客户端不断发送请求给到服务端】应用场景：iot
 *4.双向流rpc
 * </pre>
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.63.0)",
    comments = "Source: Future.proto")
@io.grpc.stub.annotations.GrpcGenerated
public final class FutureServiceGrpc {

  private FutureServiceGrpc() {}

  public static final java.lang.String SERVICE_NAME = "FutureService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<com.ww.app.proto.hello.FutureRequest,
      com.ww.app.proto.hello.FutureResponse> getTestMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "test",
      requestType = com.ww.app.proto.hello.FutureRequest.class,
      responseType = com.ww.app.proto.hello.FutureResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.ww.app.proto.hello.FutureRequest,
      com.ww.app.proto.hello.FutureResponse> getTestMethod() {
    io.grpc.MethodDescriptor<com.ww.app.proto.hello.FutureRequest, com.ww.app.proto.hello.FutureResponse> getTestMethod;
    if ((getTestMethod = FutureServiceGrpc.getTestMethod) == null) {
      synchronized (FutureServiceGrpc.class) {
        if ((getTestMethod = FutureServiceGrpc.getTestMethod) == null) {
          FutureServiceGrpc.getTestMethod = getTestMethod =
              io.grpc.MethodDescriptor.<com.ww.app.proto.hello.FutureRequest, com.ww.app.proto.hello.FutureResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "test"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.ww.app.proto.hello.FutureRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.ww.app.proto.hello.FutureResponse.getDefaultInstance()))
              .setSchemaDescriptor(new FutureServiceMethodDescriptorSupplier("test"))
              .build();
        }
      }
    }
    return getTestMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static FutureServiceStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<FutureServiceStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<FutureServiceStub>() {
        @java.lang.Override
        public FutureServiceStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new FutureServiceStub(channel, callOptions);
        }
      };
    return FutureServiceStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static FutureServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<FutureServiceBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<FutureServiceBlockingStub>() {
        @java.lang.Override
        public FutureServiceBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new FutureServiceBlockingStub(channel, callOptions);
        }
      };
    return FutureServiceBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static FutureServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<FutureServiceFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<FutureServiceFutureStub>() {
        @java.lang.Override
        public FutureServiceFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new FutureServiceFutureStub(channel, callOptions);
        }
      };
    return FutureServiceFutureStub.newStub(factory, channel);
  }

  /**
   * <pre>
   *定义服务
   *1.简单rpc 【客户端一次请求，服务端相应一次，客户端阻塞等待服务端返回】
   *2.服务端流式rpc 【客户端一次请求，服务端多次响应】
   *3.客户端流式rpc 【客户端不断发送请求给到服务端】应用场景：iot
   *4.双向流rpc
   * </pre>
   */
  public interface AsyncService {

    /**
     * <pre>
     * rpc 请求消息 HelloRequest 响应消息 HelloResponse
     * </pre>
     */
    default void test(com.ww.app.proto.hello.FutureRequest request,
        io.grpc.stub.StreamObserver<com.ww.app.proto.hello.FutureResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getTestMethod(), responseObserver);
    }
  }

  /**
   * Base class for the server implementation of the service FutureService.
   * <pre>
   *定义服务
   *1.简单rpc 【客户端一次请求，服务端相应一次，客户端阻塞等待服务端返回】
   *2.服务端流式rpc 【客户端一次请求，服务端多次响应】
   *3.客户端流式rpc 【客户端不断发送请求给到服务端】应用场景：iot
   *4.双向流rpc
   * </pre>
   */
  public static abstract class FutureServiceImplBase
      implements io.grpc.BindableService, AsyncService {

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return FutureServiceGrpc.bindService(this);
    }
  }

  /**
   * A stub to allow clients to do asynchronous rpc calls to service FutureService.
   * <pre>
   *定义服务
   *1.简单rpc 【客户端一次请求，服务端相应一次，客户端阻塞等待服务端返回】
   *2.服务端流式rpc 【客户端一次请求，服务端多次响应】
   *3.客户端流式rpc 【客户端不断发送请求给到服务端】应用场景：iot
   *4.双向流rpc
   * </pre>
   */
  public static final class FutureServiceStub
      extends io.grpc.stub.AbstractAsyncStub<FutureServiceStub> {
    private FutureServiceStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected FutureServiceStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new FutureServiceStub(channel, callOptions);
    }

    /**
     * <pre>
     * rpc 请求消息 HelloRequest 响应消息 HelloResponse
     * </pre>
     */
    public void test(com.ww.app.proto.hello.FutureRequest request,
        io.grpc.stub.StreamObserver<com.ww.app.proto.hello.FutureResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getTestMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * A stub to allow clients to do synchronous rpc calls to service FutureService.
   * <pre>
   *定义服务
   *1.简单rpc 【客户端一次请求，服务端相应一次，客户端阻塞等待服务端返回】
   *2.服务端流式rpc 【客户端一次请求，服务端多次响应】
   *3.客户端流式rpc 【客户端不断发送请求给到服务端】应用场景：iot
   *4.双向流rpc
   * </pre>
   */
  public static final class FutureServiceBlockingStub
      extends io.grpc.stub.AbstractBlockingStub<FutureServiceBlockingStub> {
    private FutureServiceBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected FutureServiceBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new FutureServiceBlockingStub(channel, callOptions);
    }

    /**
     * <pre>
     * rpc 请求消息 HelloRequest 响应消息 HelloResponse
     * </pre>
     */
    public com.ww.app.proto.hello.FutureResponse test(com.ww.app.proto.hello.FutureRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getTestMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do ListenableFuture-style rpc calls to service FutureService.
   * <pre>
   *定义服务
   *1.简单rpc 【客户端一次请求，服务端相应一次，客户端阻塞等待服务端返回】
   *2.服务端流式rpc 【客户端一次请求，服务端多次响应】
   *3.客户端流式rpc 【客户端不断发送请求给到服务端】应用场景：iot
   *4.双向流rpc
   * </pre>
   */
  public static final class FutureServiceFutureStub
      extends io.grpc.stub.AbstractFutureStub<FutureServiceFutureStub> {
    private FutureServiceFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected FutureServiceFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new FutureServiceFutureStub(channel, callOptions);
    }

    /**
     * <pre>
     * rpc 请求消息 HelloRequest 响应消息 HelloResponse
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.ww.app.proto.hello.FutureResponse> test(
        com.ww.app.proto.hello.FutureRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getTestMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_TEST = 0;

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
        case METHODID_TEST:
          serviceImpl.test((com.ww.app.proto.hello.FutureRequest) request,
              (io.grpc.stub.StreamObserver<com.ww.app.proto.hello.FutureResponse>) responseObserver);
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
          getTestMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.ww.app.proto.hello.FutureRequest,
              com.ww.app.proto.hello.FutureResponse>(
                service, METHODID_TEST)))
        .build();
  }

  private static abstract class FutureServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    FutureServiceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return com.ww.app.proto.hello.FutureProto.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("FutureService");
    }
  }

  private static final class FutureServiceFileDescriptorSupplier
      extends FutureServiceBaseDescriptorSupplier {
    FutureServiceFileDescriptorSupplier() {}
  }

  private static final class FutureServiceMethodDescriptorSupplier
      extends FutureServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final java.lang.String methodName;

    FutureServiceMethodDescriptorSupplier(java.lang.String methodName) {
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
      synchronized (FutureServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new FutureServiceFileDescriptorSupplier())
              .addMethod(getTestMethod())
              .build();
        }
      }
    }
    return result;
  }
}
