# 通用排行榜组件

基于 Redis Sorted Set 的排行榜组件，支持高性能读写与分片扩展，适用于游戏排行、活动榜单、销量榜等场景。

## 核心能力

- 支持按分数排名，分数相同按时间先后排序
- 支持分页/区间排行查询
- 支持成员排名与分数查询
- 支持批量写入、批量删除
- 支持排行榜过期时间
- 数据量达到阈值后自动分片，降低 bigkey 风险
- 迁移期间双写，读取仍走主 key，避免历史数据未完全迁移导致读缺失

## 使用示例

### 1. 写入与增量

```java
@Resource
private RankingRedisComponent rankingRedisComponent;

// 写入或更新分数
rankingRedisComponent.addOrUpdateScore("game", "level1", "user123", 100);

// 增量更新分数
rankingRedisComponent.incrementScore("game", "level1", "user123", 10);
```

### 2. 批量写入

```java
Map<String, Double> memberScores = new HashMap<>();
memberScores.put("user1", 100.0);
memberScores.put("user2", 200.0);
memberScores.put("user3", 150.0);
rankingRedisComponent.batchAddOrUpdateScore("game", "level1", memberScores);
```

### 3. 排行榜查询

```java
// 默认 TOP 50（降序）
List<RankingItem> top50 = rankingRedisComponent.getRanking("game", "level1");

// 分页查询
List<RankingItem> page = rankingRedisComponent.getRanking("game", "level1", 1, 20, true);

// 按排名区间查询
List<RankingItem> range = rankingRedisComponent.getRankingByRankRange("game", "level1", 1, 10, true);
```

### 4. 成员信息

```java
long rank = rankingRedisComponent.getMemberRank("game", "level1", "user123");
Double score = rankingRedisComponent.getMemberScore("game", "level1", "user123");
RankingItem info = rankingRedisComponent.getMemberRankingInfo("game", "level1", "user123", true);
```

### 5. 统计与删除

```java
long total = rankingRedisComponent.getRankingCount("game", "level1");
long count = rankingRedisComponent.countByScoreRange("game", "level1", 100, 200);

rankingRedisComponent.removeMember("game", "level1", "user123");
rankingRedisComponent.batchRemoveMembers("game", "level1", Arrays.asList("user1", "user2"));
rankingRedisComponent.deleteRanking("game", "level1");
```

## 分片与排序说明

- 排序规则：分数越高排名越前；分数相同按时间先后排序（先达到的排前）
- 分片阈值：当成员数达到阈值后自动启用分片
- 迁移阶段：双写主 key 与分片 key，读仍走主 key，避免历史数据缺失

## 典型业务场景

- 游戏榜：关卡排行、赛季排行、实时积分榜
- 活动榜：活动积分排行、拉新排行榜、贡献度排行榜
- 销量榜：商品销量、门店销量、类目销量排行
- 用户榜：邀请榜、消费榜、成长值榜

## 分片策略细节

- 启用条件：成员数达到阈值后自动触发分片
- 迁移方式：先标记迁移中（双写），再把主 key 数据搬到分片
- 读策略：迁移中读取主 key，避免历史数据未完全迁移导致读缺失
- 写策略：迁移中主 key 与分片双写；启用后只写分片
- 分片分布：对 memberId 的 hash 做取模，保证稳定分布

## 参数说明

- `bizType`：业务类型，必填，用于区分不同榜单
- `bizId`：业务 ID，可选，用于区分同类型下的不同榜单
- `memberId`：成员 ID，必填
- `score`：分数，当前实现按整数分数设计（如分/积分）

## 参数约束

- `bizType` / `memberId` 不能为空
- `score` 建议为整数分值（如分/积分），小数会被截断
- 分页参数 `page >= 1`，`size >= 1`，且建议 `size <= 100`

## 性能建议

- 批量操作优先使用 `batchAddOrUpdateScore` / `batchRemoveMembers`
- 分页查询建议 `size <= 100`，避免一次返回过多数据
- 读取大区间榜单时尽量分页，避免拉取过大数据量
- 分片场景下查询需要合并结果，性能略低于单 key，建议控制返回数量

## 异常与容错说明

- 组件内部捕获异常并记录日志，返回默认值避免影响业务主流程
- Redis 异常时返回空集合或默认值，业务层可按需重试或降级

## 常见问题

1) 为什么分片后查询变慢？
   分片查询需要聚合多分片结果，属于“空间换时间”的策略。

2) 迁移中为什么不读分片？
   迁移期间历史数据可能尚未全部拷贝，读分片会产生缺失数据。

3) 是否支持小数分数？
   当前实现按整数分数设计，小数会被截断；如需支持小数可调整分数计算与脚本。
