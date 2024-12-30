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
    comments = "Source: Hello.proto")
@io.grpc.stub.annotations.GrpcGenerated
public final class HelloServiceGrpc {

  private HelloServiceGrpc() {}

  public static final java.lang.String SERVICE_NAME = "HelloService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<com.ww.app.proto.hello.HelloRequest,
      com.ww.app.proto.hello.HelloResponse> getHelloMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "hello",
      requestType = com.ww.app.proto.hello.HelloRequest.class,
      responseType = com.ww.app.proto.hello.HelloResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.ww.app.proto.hello.HelloRequest,
      com.ww.app.proto.hello.HelloResponse> getHelloMethod() {
    io.grpc.MethodDescriptor<com.ww.app.proto.hello.HelloRequest, com.ww.app.proto.hello.HelloResponse> getHelloMethod;
    if ((getHelloMethod = HelloServiceGrpc.getHelloMethod) == null) {
      synchronized (HelloServiceGrpc.class) {
        if ((getHelloMethod = HelloServiceGrpc.getHelloMethod) == null) {
          HelloServiceGrpc.getHelloMethod = getHelloMethod =
              io.grpc.MethodDescriptor.<com.ww.app.proto.hello.HelloRequest, com.ww.app.proto.hello.HelloResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "hello"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.ww.app.proto.hello.HelloRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.ww.app.proto.hello.HelloResponse.getDefaultInstance()))
              .setSchemaDescriptor(new HelloServiceMethodDescriptorSupplier("hello"))
              .build();
        }
      }
    }
    return getHelloMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.ww.app.proto.hello.HelloRequest,
      com.ww.app.proto.hello.HelloResponse> getServerStreamHelloMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "serverStreamHello",
      requestType = com.ww.app.proto.hello.HelloRequest.class,
      responseType = com.ww.app.proto.hello.HelloResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
  public static io.grpc.MethodDescriptor<com.ww.app.proto.hello.HelloRequest,
      com.ww.app.proto.hello.HelloResponse> getServerStreamHelloMethod() {
    io.grpc.MethodDescriptor<com.ww.app.proto.hello.HelloRequest, com.ww.app.proto.hello.HelloResponse> getServerStreamHelloMethod;
    if ((getServerStreamHelloMethod = HelloServiceGrpc.getServerStreamHelloMethod) == null) {
      synchronized (HelloServiceGrpc.class) {
        if ((getServerStreamHelloMethod = HelloServiceGrpc.getServerStreamHelloMethod) == null) {
          HelloServiceGrpc.getServerStreamHelloMethod = getServerStreamHelloMethod =
              io.grpc.MethodDescriptor.<com.ww.app.proto.hello.HelloRequest, com.ww.app.proto.hello.HelloResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "serverStreamHello"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.ww.app.proto.hello.HelloRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.ww.app.proto.hello.HelloResponse.getDefaultInstance()))
              .setSchemaDescriptor(new HelloServiceMethodDescriptorSupplier("serverStreamHello"))
              .build();
        }
      }
    }
    return getServerStreamHelloMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.ww.app.proto.hello.HelloRequest,
      com.ww.app.proto.hello.HelloResponse> getClientStreamHelloMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "clientStreamHello",
      requestType = com.ww.app.proto.hello.HelloRequest.class,
      responseType = com.ww.app.proto.hello.HelloResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.CLIENT_STREAMING)
  public static io.grpc.MethodDescriptor<com.ww.app.proto.hello.HelloRequest,
      com.ww.app.proto.hello.HelloResponse> getClientStreamHelloMethod() {
    io.grpc.MethodDescriptor<com.ww.app.proto.hello.HelloRequest, com.ww.app.proto.hello.HelloResponse> getClientStreamHelloMethod;
    if ((getClientStreamHelloMethod = HelloServiceGrpc.getClientStreamHelloMethod) == null) {
      synchronized (HelloServiceGrpc.class) {
        if ((getClientStreamHelloMethod = HelloServiceGrpc.getClientStreamHelloMethod) == null) {
          HelloServiceGrpc.getClientStreamHelloMethod = getClientStreamHelloMethod =
              io.grpc.MethodDescriptor.<com.ww.app.proto.hello.HelloRequest, com.ww.app.proto.hello.HelloResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.CLIENT_STREAMING)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "clientStreamHello"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.ww.app.proto.hello.HelloRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.ww.app.proto.hello.HelloResponse.getDefaultInstance()))
              .setSchemaDescriptor(new HelloServiceMethodDescriptorSupplier("clientStreamHello"))
              .build();
        }
      }
    }
    return getClientStreamHelloMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.ww.app.proto.hello.HelloRequest,
      com.ww.app.proto.hello.HelloResponse> getDuplexStreamHelloMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "duplexStreamHello",
      requestType = com.ww.app.proto.hello.HelloRequest.class,
      responseType = com.ww.app.proto.hello.HelloResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.BIDI_STREAMING)
  public static io.grpc.MethodDescriptor<com.ww.app.proto.hello.HelloRequest,
      com.ww.app.proto.hello.HelloResponse> getDuplexStreamHelloMethod() {
    io.grpc.MethodDescriptor<com.ww.app.proto.hello.HelloRequest, com.ww.app.proto.hello.HelloResponse> getDuplexStreamHelloMethod;
    if ((getDuplexStreamHelloMethod = HelloServiceGrpc.getDuplexStreamHelloMethod) == null) {
      synchronized (HelloServiceGrpc.class) {
        if ((getDuplexStreamHelloMethod = HelloServiceGrpc.getDuplexStreamHelloMethod) == null) {
          HelloServiceGrpc.getDuplexStreamHelloMethod = getDuplexStreamHelloMethod =
              io.grpc.MethodDescriptor.<com.ww.app.proto.hello.HelloRequest, com.ww.app.proto.hello.HelloResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.BIDI_STREAMING)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "duplexStreamHello"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.ww.app.proto.hello.HelloRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.ww.app.proto.hello.HelloResponse.getDefaultInstance()))
              .setSchemaDescriptor(new HelloServiceMethodDescriptorSupplier("duplexStreamHello"))
              .build();
        }
      }
    }
    return getDuplexStreamHelloMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static HelloServiceStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<HelloServiceStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<HelloServiceStub>() {
        @java.lang.Override
        public HelloServiceStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new HelloServiceStub(channel, callOptions);
        }
      };
    return HelloServiceStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static HelloServiceBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<HelloServiceBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<HelloServiceBlockingStub>() {
        @java.lang.Override
        public HelloServiceBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new HelloServiceBlockingStub(channel, callOptions);
        }
      };
    return HelloServiceBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static HelloServiceFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<HelloServiceFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<HelloServiceFutureStub>() {
        @java.lang.Override
        public HelloServiceFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new HelloServiceFutureStub(channel, callOptions);
        }
      };
    return HelloServiceFutureStub.newStub(factory, channel);
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
     * 简单rpc 请求消息：HelloRequest 响应消息：HelloResponse
     * </pre>
     */
    default void hello(com.ww.app.proto.hello.HelloRequest request,
        io.grpc.stub.StreamObserver<com.ww.app.proto.hello.HelloResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getHelloMethod(), responseObserver);
    }

    /**
     * <pre>
     * 服务端流式rpc
     * </pre>
     */
    default void serverStreamHello(com.ww.app.proto.hello.HelloRequest request,
        io.grpc.stub.StreamObserver<com.ww.app.proto.hello.HelloResponse> responseObserver) {
      io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall(getServerStreamHelloMethod(), responseObserver);
    }

    /**
     * <pre>
     * 客户端流式rpc
     * </pre>
     */
    default io.grpc.stub.StreamObserver<com.ww.app.proto.hello.HelloRequest> clientStreamHello(
        io.grpc.stub.StreamObserver<com.ww.app.proto.hello.HelloResponse> responseObserver) {
      return io.grpc.stub.ServerCalls.asyncUnimplementedStreamingCall(getClientStreamHelloMethod(), responseObserver);
    }

    /**
     * <pre>
     * 双向流式rpc
     * </pre>
     */
    default io.grpc.stub.StreamObserver<com.ww.app.proto.hello.HelloRequest> duplexStreamHello(
        io.grpc.stub.StreamObserver<com.ww.app.proto.hello.HelloResponse> responseObserver) {
      return io.grpc.stub.ServerCalls.asyncUnimplementedStreamingCall(getDuplexStreamHelloMethod(), responseObserver);
    }
  }

  /**
   * Base class for the server implementation of the service HelloService.
   * <pre>
   *定义服务
   *1.简单rpc 【客户端一次请求，服务端相应一次，客户端阻塞等待服务端返回】
   *2.服务端流式rpc 【客户端一次请求，服务端多次响应】
   *3.客户端流式rpc 【客户端不断发送请求给到服务端】应用场景：iot
   *4.双向流rpc
   * </pre>
   */
  public static abstract class HelloServiceImplBase
      implements io.grpc.BindableService, AsyncService {

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return HelloServiceGrpc.bindService(this);
    }
  }

  /**
   * A stub to allow clients to do asynchronous rpc calls to service HelloService.
   * <pre>
   *定义服务
   *1.简单rpc 【客户端一次请求，服务端相应一次，客户端阻塞等待服务端返回】
   *2.服务端流式rpc 【客户端一次请求，服务端多次响应】
   *3.客户端流式rpc 【客户端不断发送请求给到服务端】应用场景：iot
   *4.双向流rpc
   * </pre>
   */
  public static final class HelloServiceStub
      extends io.grpc.stub.AbstractAsyncStub<HelloServiceStub> {
    private HelloServiceStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected HelloServiceStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new HelloServiceStub(channel, callOptions);
    }

    /**
     * <pre>
     * 简单rpc 请求消息：HelloRequest 响应消息：HelloResponse
     * </pre>
     */
    public void hello(com.ww.app.proto.hello.HelloRequest request,
        io.grpc.stub.StreamObserver<com.ww.app.proto.hello.HelloResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncUnaryCall(
          getChannel().newCall(getHelloMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * 服务端流式rpc
     * </pre>
     */
    public void serverStreamHello(com.ww.app.proto.hello.HelloRequest request,
        io.grpc.stub.StreamObserver<com.ww.app.proto.hello.HelloResponse> responseObserver) {
      io.grpc.stub.ClientCalls.asyncServerStreamingCall(
          getChannel().newCall(getServerStreamHelloMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * 客户端流式rpc
     * </pre>
     */
    public io.grpc.stub.StreamObserver<com.ww.app.proto.hello.HelloRequest> clientStreamHello(
        io.grpc.stub.StreamObserver<com.ww.app.proto.hello.HelloResponse> responseObserver) {
      return io.grpc.stub.ClientCalls.asyncClientStreamingCall(
          getChannel().newCall(getClientStreamHelloMethod(), getCallOptions()), responseObserver);
    }

    /**
     * <pre>
     * 双向流式rpc
     * </pre>
     */
    public io.grpc.stub.StreamObserver<com.ww.app.proto.hello.HelloRequest> duplexStreamHello(
        io.grpc.stub.StreamObserver<com.ww.app.proto.hello.HelloResponse> responseObserver) {
      return io.grpc.stub.ClientCalls.asyncBidiStreamingCall(
          getChannel().newCall(getDuplexStreamHelloMethod(), getCallOptions()), responseObserver);
    }
  }

  /**
   * A stub to allow clients to do synchronous rpc calls to service HelloService.
   * <pre>
   *定义服务
   *1.简单rpc 【客户端一次请求，服务端相应一次，客户端阻塞等待服务端返回】
   *2.服务端流式rpc 【客户端一次请求，服务端多次响应】
   *3.客户端流式rpc 【客户端不断发送请求给到服务端】应用场景：iot
   *4.双向流rpc
   * </pre>
   */
  public static final class HelloServiceBlockingStub
      extends io.grpc.stub.AbstractBlockingStub<HelloServiceBlockingStub> {
    private HelloServiceBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected HelloServiceBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new HelloServiceBlockingStub(channel, callOptions);
    }

    /**
     * <pre>
     * 简单rpc 请求消息：HelloRequest 响应消息：HelloResponse
     * </pre>
     */
    public com.ww.app.proto.hello.HelloResponse hello(com.ww.app.proto.hello.HelloRequest request) {
      return io.grpc.stub.ClientCalls.blockingUnaryCall(
          getChannel(), getHelloMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * 服务端流式rpc
     * </pre>
     */
    public java.util.Iterator<com.ww.app.proto.hello.HelloResponse> serverStreamHello(
        com.ww.app.proto.hello.HelloRequest request) {
      return io.grpc.stub.ClientCalls.blockingServerStreamingCall(
          getChannel(), getServerStreamHelloMethod(), getCallOptions(), request);
    }
  }

  /**
   * A stub to allow clients to do ListenableFuture-style rpc calls to service HelloService.
   * <pre>
   *定义服务
   *1.简单rpc 【客户端一次请求，服务端相应一次，客户端阻塞等待服务端返回】
   *2.服务端流式rpc 【客户端一次请求，服务端多次响应】
   *3.客户端流式rpc 【客户端不断发送请求给到服务端】应用场景：iot
   *4.双向流rpc
   * </pre>
   */
  public static final class HelloServiceFutureStub
      extends io.grpc.stub.AbstractFutureStub<HelloServiceFutureStub> {
    private HelloServiceFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected HelloServiceFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new HelloServiceFutureStub(channel, callOptions);
    }

    /**
     * <pre>
     * 简单rpc 请求消息：HelloRequest 响应消息：HelloResponse
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.ww.app.proto.hello.HelloResponse> hello(
        com.ww.app.proto.hello.HelloRequest request) {
      return io.grpc.stub.ClientCalls.futureUnaryCall(
          getChannel().newCall(getHelloMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_HELLO = 0;
  private static final int METHODID_SERVER_STREAM_HELLO = 1;
  private static final int METHODID_CLIENT_STREAM_HELLO = 2;
  private static final int METHODID_DUPLEX_STREAM_HELLO = 3;

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
        case METHODID_HELLO:
          serviceImpl.hello((com.ww.app.proto.hello.HelloRequest) request,
              (io.grpc.stub.StreamObserver<com.ww.app.proto.hello.HelloResponse>) responseObserver);
          break;
        case METHODID_SERVER_STREAM_HELLO:
          serviceImpl.serverStreamHello((com.ww.app.proto.hello.HelloRequest) request,
              (io.grpc.stub.StreamObserver<com.ww.app.proto.hello.HelloResponse>) responseObserver);
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
        case METHODID_CLIENT_STREAM_HELLO:
          return (io.grpc.stub.StreamObserver<Req>) serviceImpl.clientStreamHello(
              (io.grpc.stub.StreamObserver<com.ww.app.proto.hello.HelloResponse>) responseObserver);
        case METHODID_DUPLEX_STREAM_HELLO:
          return (io.grpc.stub.StreamObserver<Req>) serviceImpl.duplexStreamHello(
              (io.grpc.stub.StreamObserver<com.ww.app.proto.hello.HelloResponse>) responseObserver);
        default:
          throw new AssertionError();
      }
    }
  }

  public static final io.grpc.ServerServiceDefinition bindService(AsyncService service) {
    return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
        .addMethod(
          getHelloMethod(),
          io.grpc.stub.ServerCalls.asyncUnaryCall(
            new MethodHandlers<
              com.ww.app.proto.hello.HelloRequest,
              com.ww.app.proto.hello.HelloResponse>(
                service, METHODID_HELLO)))
        .addMethod(
          getServerStreamHelloMethod(),
          io.grpc.stub.ServerCalls.asyncServerStreamingCall(
            new MethodHandlers<
              com.ww.app.proto.hello.HelloRequest,
              com.ww.app.proto.hello.HelloResponse>(
                service, METHODID_SERVER_STREAM_HELLO)))
        .addMethod(
          getClientStreamHelloMethod(),
          io.grpc.stub.ServerCalls.asyncClientStreamingCall(
            new MethodHandlers<
              com.ww.app.proto.hello.HelloRequest,
              com.ww.app.proto.hello.HelloResponse>(
                service, METHODID_CLIENT_STREAM_HELLO)))
        .addMethod(
          getDuplexStreamHelloMethod(),
          io.grpc.stub.ServerCalls.asyncBidiStreamingCall(
            new MethodHandlers<
              com.ww.app.proto.hello.HelloRequest,
              com.ww.app.proto.hello.HelloResponse>(
                service, METHODID_DUPLEX_STREAM_HELLO)))
        .build();
  }

  private static abstract class HelloServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    HelloServiceBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return com.ww.app.proto.hello.HelloProto.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("HelloService");
    }
  }

  private static final class HelloServiceFileDescriptorSupplier
      extends HelloServiceBaseDescriptorSupplier {
    HelloServiceFileDescriptorSupplier() {}
  }

  private static final class HelloServiceMethodDescriptorSupplier
      extends HelloServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final java.lang.String methodName;

    HelloServiceMethodDescriptorSupplier(java.lang.String methodName) {
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
      synchronized (HelloServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new HelloServiceFileDescriptorSupplier())
              .addMethod(getHelloMethod())
              .addMethod(getServerStreamHelloMethod())
              .addMethod(getClientStreamHelloMethod())
              .addMethod(getDuplexStreamHelloMethod())
              .build();
        }
      }
    }
    return result;
  }
}
