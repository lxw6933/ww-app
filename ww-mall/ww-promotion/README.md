# 拼团活动业务系统

## 概述

`ww-promotion` 当前采用 Redis + Lua 维护拼团主状态，Mongo 作为查询模型。
主链路在 Redis 状态流转成功后，只额外发送一条内部 `group.state.changed` 消息，
消费者再把 Redis 快照同步落到 Mongo。

## 当前消息链路

- `group.order.paid`：支付成功后驱动开团或参团。
- `group.after.sale.success`：售后成功后驱动释放名额或关团。
- `group.state.changed`：拼团状态变更后的内部落库消息，只用于同步 Mongo 查询模型。

## 当前核心流程

### 创建拼团 / 参团

1. 校验活动、订单与幂等条件，并计算团业务失效时间。
2. 执行 Redis Lua，原子更新拼团状态与成员快照。
3. 主链路 `try/catch` 发送一条 `group.state.changed` 内部消息。
4. 消费者收到消息后，把最新 Redis 快照同步到 Mongo。

### 售后成功

1. 根据订单定位拼团与成员。
2. 执行 Redis Lua，原子释放名额或将拼团置为失败。
3. 主链路 `try/catch` 发送一条 `group.state.changed` 内部消息。
4. 消费者收到消息后，把最新 Redis 快照同步到 Mongo。

### 过期关团

1. 定时任务扫描过期索引。
2. 执行 Redis Lua，将过期拼团标记为失败。
3. 主链路 `try/catch` 发送一条 `group.state.changed` 内部消息。
4. 消费者收到消息后，把最新 Redis 快照同步到 Mongo。

## 数据存储

### Redis

- `group:instance:meta:{groupId}`：拼团主状态，内部包含业务失效时间 `expireTime`。
- `group:instance:member-store:{groupId}`：成员快照。
- `group:instance:user-index:{groupId}`：团内活跃用户索引。
- `group:order:index`：订单到拼团的幂等索引。
- `group:activity:stats:{activityId}`：活动累计统计。
- `group:expiry`：过期索引。

### Mongo

- `GroupActivity`：活动模板。
- `GroupInstance`：拼团实例查询模型。
- `GroupMember`：拼团成员查询模型。

## 说明

- 当前实现不再自动发送拼团成功、失败、退款等业务通知。
- 限购校验与计数维护不在拼团域执行，统一由下单域负责。
- `expireTime` 表示团业务失效时间，取“活动结束时间”和“开团时间 + 团有效期”中的较小值。
- OPEN 状态 Redis TTL 在开团时一次性设置为“距 `expireTime` 的剩余时长 + 2天保留期”。
- 团成功、售后关闭、过期失败后，Redis TTL 会重置为固定 2 天，不继续沿用 OPEN 状态下的长 TTL。
- 如果内部 `group.state.changed` 发送失败，只记录错误日志。
- B 端回显时应校验 Mongo/Redis 状态，必要时提供手动补发内部消息的入口。
