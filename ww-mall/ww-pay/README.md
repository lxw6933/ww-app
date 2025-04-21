# WW商城支付模块架构设计

## 1. 架构概述

支付模块采用了策略模式设计，通过统一的接口定义，实现了多种支付渠道的统一管理，包括支付宝、微信支付等。系统支持完整的支付流程，包括支付下单、支付查询、支付回调、退款申请、退款查询、退款回调等功能。

![支付模块架构图](./doc/images/pay-architecture.png)

## 2. 核心接口和类

### 2.1 策略接口

- `PaymentStrategy`: 所有支付渠道实现的统一接口，定义了支付相关的所有操作

### 2.2 枚举类

- `PayChannelEnum`: 支付渠道枚举，如支付宝、微信支付等
- `PayTypeEnum`: 支付方式枚举，如APP支付、H5支付、扫码支付等
- `PayStatusEnum`: 支付状态枚举，如待支付、支付中、支付成功等
- `RefundStatusEnum`: 退款状态枚举，如退款中、退款成功等

### 2.3 DTO/VO

- 请求DTO: `PaymentRequestDTO`, `PaymentQueryDTO`, `RefundRequestDTO`, `RefundQueryDTO`
- 响应VO: `PaymentResponseVO`, `PaymentQueryResponseVO`, `RefundResponseVO`, `RefundQueryResponseVO`
- 回调DTO: `PaymentCallbackDTO`, `RefundCallbackDTO`

### 2.4 服务接口

- `PaymentService`: 统一支付服务接口，定义了支付相关的所有业务操作

### 2.5 工厂类

- `PaymentStrategyFactory`: 支付策略工厂，根据支付渠道选择对应的支付策略实现

## 3. 支付流程

### 3.1 创建支付订单

```
1. 客户端调用支付接口
2. PaymentController接收请求并调用PaymentService
3. PaymentService通过PaymentStrategyFactory获取对应支付渠道的策略实现
4. 具体策略实现(如AliPayStrategy)处理支付逻辑
5. 返回支付参数给客户端
```

### 3.2 处理支付回调

```
1. 支付平台调用商户回调接口
2. PaymentController接收回调请求并调用PaymentService
3. PaymentService通过PaymentStrategyFactory获取对应支付渠道的策略实现
4. 具体策略实现处理回调，验证签名，解析支付结果
5. 返回处理结果给支付平台
```

### 3.3 申请退款

```
1. 客户端调用退款接口
2. PaymentController接收请求并调用PaymentService
3. PaymentService通过PaymentStrategyFactory获取对应支付渠道的策略实现
4. 具体策略实现处理退款逻辑
5. 返回退款结果给客户端
```

## 4. 扩展支付渠道

要增加新的支付渠道，只需以下步骤：

1. 在`PayChannelEnum`中添加新的渠道枚举
2. 实现`PaymentStrategy`接口，创建新的支付策略类
3. 添加新渠道的配置属性类
4. 注册新的支付策略实现到Spring容器

## 5. 配置示例

```yaml
pay:
  # 支付宝支付配置
  alipay:
    app-id: 2021000123456789
    private-key: MIIEvQIBADANBgk.....
    public-key: MIIBIjANBgk.....
    server-url: https://openapi.alipay.com/gateway.do
    notify-url: https://api.yourdomain.com/api/payment/callback/alipay
    return-url: https://www.yourdomain.com/payment/result

  # 微信支付配置
  wxpay:
    app-id: wx123456789
    mch-id: 1234567890
    api-key: your_api_key
    notify-url: https://api.yourdomain.com/api/payment/callback/wxpay
```

## 6. API接口

| 接口 | 方法 | 路径 | 说明 |
| --- | --- | --- | --- |
| 创建支付订单 | POST | /api/payment/pay | 创建支付订单并获取支付参数 |
| 支付页面跳转 | GET | /api/payment/pay/redirect | 页面跳转支付 |
| 支付回调 | POST | /api/payment/callback/{channel} | 处理支付平台回调 |
| 查询支付结果 | POST | /api/payment/query | 查询支付结果 |
| 申请退款 | POST | /api/payment/refund | 申请退款 |
| 退款回调 | POST | /api/payment/refund/callback/{channel} | 处理退款回调 |
| 查询退款结果 | POST | /api/payment/refund/query | 查询退款结果 |

## 7. 高性能设计

- 使用异步处理支付结果，通过消息队列解耦支付和业务逻辑
- 采用缓存优化支付结果查询
- 使用乐观锁保证支付并发安全

## 8. 高可用设计

- 支付系统服务部署多实例，提供负载均衡
- 使用熔断机制处理第三方支付平台异常
- 支付信息持久化，确保系统重启不丢失支付状态

## 9. 安全设计

- 全链路签名验证，确保支付数据完整性
- 敏感信息加密存储
- 支付接口添加防重放攻击措施
- 支付回调接口添加IP白名单限制 