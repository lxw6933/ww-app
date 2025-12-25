# 拼团活动业务系统

## 系统架构

本系统是一个高性能、高扩展性的电商拼团活动业务系统，基于Spring Boot 2.7，使用MongoDB、Redis和RabbitMQ构建。

### 技术栈

- **Spring Boot 2.7**: 基础框架
- **MongoDB**: 持久化存储（活动、拼团实例、成员信息）
- **Redis**: 缓存和实时数据存储（拼团状态、库存、过期索引）
- **RabbitMQ**: 异步消息处理（拼团成功/失败通知）
- **Lua脚本**: Redis原子操作保证数据一致性
- **XXL-Job**: 分布式定时任务（处理过期拼团）

## 核心设计

### 1. 数据存储策略

#### MongoDB（持久化）
- `GroupActivity`: 拼团活动模板
- `GroupInstance`: 拼团实例
- `GroupMember`: 拼团成员记录

#### Redis（实时数据）
- **活动缓存**: `group:activity:cache:{activityId}` - 活动信息缓存
- **拼团元数据**: `group:instance:meta:{groupId}` - 拼团基本信息（Hash）
- **剩余名额**: `group:instance:slots:{groupId}` - 剩余名额（String）
- **成员列表**: `group:instance:members:{groupId}` - 成员ID列表（Sorted Set）
- **订单信息**: `group:instance:orders:{groupId}` - 订单信息（Hash）
- **过期索引**: `group:expiry` - 过期时间索引（Sorted Set，score为过期时间）
- **活动库存**: `group:activity:stock:{activityId}` - 活动库存（String）
- **用户拼团**: `group:user:group:{userId}` - 用户参与的拼团（Set）

### 2. 核心业务流程

#### 创建拼团流程
1. 校验活动状态和库存
2. 扣减Redis库存
3. 执行Lua脚本原子性创建拼团（防止并发）
4. 添加到过期索引
5. 异步保存到MongoDB

#### 加入拼团流程
1. 从Redis获取拼团信息
2. 执行Lua脚本原子性加入拼团
3. 检查是否拼团成功
4. 异步保存成员信息到MongoDB
5. 如果拼团成功，发送消息到RabbitMQ

#### 拼团过期处理
1. 定时任务扫描过期索引
2. 使用Lua脚本原子性标记失败
3. 发送失败消息到RabbitMQ
4. 处理退款等后续逻辑

### 3. 高性能优化

#### Redis Lua脚本
- **原子性保证**: 所有关键操作使用Lua脚本，保证原子性
- **减少网络往返**: 一次脚本执行完成多个操作
- **防止并发问题**: 幂等性检查、状态校验都在脚本中完成

#### 缓存策略
- **活动信息缓存**: 减少数据库查询
- **拼团状态实时查询**: 直接从Redis获取，响应速度快
- **过期索引**: 使用Sorted Set快速查询过期拼团

#### 异步处理
- **MongoDB持久化**: 异步保存，不阻塞主流程
- **消息队列**: 拼团成功/失败通过RabbitMQ异步处理
- **定时任务**: 过期拼团处理使用定时任务，避免实时轮询

### 4. 高扩展性设计

#### 水平扩展
- **无状态服务**: 服务实例可以水平扩展
- **Redis集群**: 支持Redis集群模式
- **MongoDB分片**: 支持MongoDB分片集群

#### 消息队列解耦
- **拼团成功**: 通知订单系统、库存系统等
- **拼团失败**: 触发退款、库存回退等
- **可扩展**: 新增消费者不影响现有流程

#### 定时任务
- **分布式任务**: 使用XXL-Job支持分布式执行
- **可配置**: 任务执行频率可配置
- **容错**: 任务失败可重试

## API接口

### 活动管理
- `POST /api/promotion/group/activity/create` - 创建活动
- `PUT /api/promotion/group/activity/update/{activityId}` - 更新活动
- `GET /api/promotion/group/activity/detail/{activityId}` - 查询活动详情
- `GET /api/promotion/group/activity/list/active` - 查询进行中的活动
- `GET /api/promotion/group/activity/list/spu/{spuId}` - 根据SPU查询活动
- `PUT /api/promotion/group/activity/enable/{activityId}` - 启用/禁用活动

### 拼团操作
- `POST /api/promotion/group/instance/create` - 创建拼团
- `POST /api/promotion/group/instance/join` - 加入拼团
- `GET /api/promotion/group/instance/detail/{groupId}` - 查询拼团详情
- `GET /api/promotion/group/instance/user/{userId}` - 查询用户参与的拼团
- `GET /api/promotion/group/instance/activity/{activityId}` - 查询活动下的拼团

## 定时任务

### groupExpireJobHandler
- **功能**: 处理过期拼团
- **执行频率**: 每分钟执行一次
- **逻辑**: 扫描过期索引，标记失败的拼团

### groupSyncToMongoJobHandler
- **功能**: 同步Redis数据到MongoDB
- **执行频率**: 每小时执行一次
- **逻辑**: 将Redis中的拼团状态同步到MongoDB

## 消息队列

### 队列配置
- **交换机**: `group.exchange` (TopicExchange)
- **成功队列**: `group.success.queue`
- **失败队列**: `group.failed.queue`
- **过期队列**: `group.expired.queue`

### 消息处理
- **拼团成功**: 通知订单系统发货、更新库存等
- **拼团失败**: 触发退款、库存回退等

## 部署建议

### Redis配置
- 建议使用Redis集群模式
- 设置合适的过期时间
- 配置持久化策略

### MongoDB配置
- 建议使用副本集
- 配置合适的索引
- 定期备份数据

### RabbitMQ配置
- 配置死信队列
- 设置消息持久化
- 配置消费者并发数

### 性能调优
- 根据并发量调整线程池大小
- 优化Lua脚本执行效率
- 合理设置缓存过期时间
- 监控系统性能指标

## 注意事项

1. **幂等性**: 所有操作都支持幂等性，防止重复提交
2. **库存管理**: 库存扣减在Redis中完成，需要定期与MongoDB同步
3. **过期处理**: 定时任务需要保证高可用，避免过期拼团未及时处理
4. **消息可靠性**: RabbitMQ消息需要保证可靠性，配置重试和死信队列
5. **数据一致性**: Redis和MongoDB数据需要定期同步，保证一致性

## 扩展建议

1. **限流**: 添加接口限流，防止恶意请求
2. **监控**: 添加系统监控和告警
3. **日志**: 完善日志记录，便于问题排查
4. **测试**: 添加单元测试和集成测试
5. **文档**: 完善API文档和使用文档



