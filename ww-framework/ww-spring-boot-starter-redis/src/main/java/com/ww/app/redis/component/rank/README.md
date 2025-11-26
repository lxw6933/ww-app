# 通用排行榜组件

基于Redis Sorted Set实现的高性能、高扩展性排行榜组件，适用于各种业务场景。

## 特性

- ✅ **高性能**：基于Redis Sorted Set，O(log N)时间复杂度
- ✅ **高扩展性**：支持多业务类型、多排行榜实例
- ✅ **默认TOP 50**：默认最多展示50条，支持自定义分页
- ✅ **批量操作**：支持批量添加、批量删除，提升性能
- ✅ **灵活排序**：支持升序/降序排序
- ✅ **完善功能**：支持排名查询、分数查询、区间统计等
- ✅ **过期时间**：支持设置排行榜过期时间
- ✅ **生产级别**：完善的异常处理、日志记录、参数校验

## 核心功能

### 1. 添加/更新分数

```java
@Resource
private RankingRedisComponent rankingRedisComponent;

// 添加或更新成员分数
boolean success = rankingRedisComponent.addOrUpdateScore("game", "level1", "user123", 100.0);

// 增量更新分数
Double newScore = rankingRedisComponent.incrementScore("game", "level1", "user123", 10.0);
```

### 2. 批量操作

```java
// 批量添加分数
Map<String, Double> memberScores = new HashMap<>();
memberScores.put("user1", 100.0);
memberScores.put("user2", 200.0);
memberScores.put("user3", 150.0);
long added = rankingRedisComponent.batchAddOrUpdateScore("game", "level1", memberScores);
```

### 3. 获取排行榜

```java
// 获取默认TOP 50（降序）
List<RankingItem> top50 = rankingRedisComponent.getRanking("game", "level1");

// 分页获取排行榜
List<RankingItem> ranking = rankingRedisComponent.getRanking("game", "level1", 1, 20, true);

// 按排名区间获取
List<RankingItem> ranking = rankingRedisComponent.getRankingByRankRange("game", "level1", 1, 10, true);
```

### 4. 查询成员信息

```java
// 获取成员排名
long rank = rankingRedisComponent.getMemberRank("game", "level1", "user123");

// 获取成员分数
Double score = rankingRedisComponent.getMemberScore("game", "level1", "user123");

// 获取成员完整排名信息（排名+分数）
RankingItem item = rankingRedisComponent.getMemberRankingInfo("game", "level1", "user123", true);
```

### 5. 统计功能

```java
// 获取排行榜总数
long count = rankingRedisComponent.getRankingCount("game", "level1");

// 按分数区间统计
long count = rankingRedisComponent.countByScoreRange("game", "level1", 100.0, 200.0);
```

### 6. 删除操作

```java
// 删除单个成员
boolean success = rankingRedisComponent.removeMember("game", "level1", "user123");

// 批量删除成员
List<String> memberIds = Arrays.asList("user1", "user2", "user3");
long removed = rankingRedisComponent.batchRemoveMembers("game", "level1", memberIds);

// 删除整个排行榜
boolean success = rankingRedisComponent.deleteRanking("game", "level1");
```

### 7. 过期时间

```java
// 设置排行榜7天后过期
boolean success = rankingRedisComponent.expireRanking("game", "level1", 7 * 24 * 3600);
```

## 使用场景

### 游戏排行榜

```java
// 游戏关卡排行榜
rankingRedisComponent.addOrUpdateScore("game", "level1", userId, score);
List<RankingItem> topPlayers = rankingRedisComponent.getRanking("game", "level1");
```

### 活动排行榜

```java
// 活动积分排行榜
rankingRedisComponent.addOrUpdateScore("activity", "spring2024", userId, points);
List<RankingItem> topUsers = rankingRedisComponent.getRanking("activity", "spring2024", 1, 50, true);
```

### 商品销量排行榜

```java
// 商品销量排行榜
rankingRedisComponent.incrementScore("product", "sales", productId, 1.0);
List<RankingItem> topProducts = rankingRedisComponent.getRanking("product", "sales");
```

### 用户贡献度排行榜

```java
// 用户贡献度排行榜（按周）
String weekKey = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
rankingRedisComponent.addOrUpdateScore("contribution", weekKey, userId, contribution);
List<RankingItem> topContributors = rankingRedisComponent.getRanking("contribution", weekKey);
```

## 参数说明

### bizType（业务类型）
- 用于区分不同的排行榜类型
- 例如：`"game"`, `"activity"`, `"product"`, `"sales"` 等
- **必填**

### bizId（业务ID）
- 可选参数，用于区分同一业务类型下的不同排行榜实例
- 例如：关卡ID、活动ID、日期等
- **可选**

### memberId（成员ID）
- 排行榜中的成员标识
- 可以是用户ID、商品ID等
- **必填**

### score（分数）
- 成员的分数值
- 支持小数
- **必填**

## 性能优化

1. **批量操作**：使用 `batchAddOrUpdateScore` 和 `batchRemoveMembers` 进行批量操作，减少网络往返
2. **Pipeline**：在获取成员排名信息时使用Pipeline，一次请求获取多个数据
3. **分页限制**：最大分页大小为1000，避免一次性查询过多数据
4. **分批处理**：批量操作自动分批处理，每批1000条，避免bigkey问题

## 注意事项

1. **分数范围**：支持最大 99,999,999 的分数范围（约1亿），满足充值金额等业务场景
2. **分数精度**：内部使用时间戳作为分数的小数部分，确保相同分数时按时间排序（先达到的排在前面）
3. **大用户量支持**：
   - 通过纳秒级时间戳处理并发，可支持高并发场景下的排序稳定性
   - **自动分片**：当用户量超过10,000时，自动启用分片（64个分片），避免bigkey问题
   - 分片后查询会合并所有分片的结果，性能略有下降但可接受
4. **排名从1开始**：返回的排名从1开始，0表示未上榜
5. **默认降序**：默认按分数从高到低排序
6. **过期时间**：建议为排行榜设置合理的过期时间，避免数据积累
7. **分页大小**：建议分页大小不超过100，保证查询性能
8. **Redis性能**：
   - 未分片时：Redis Sorted Set 可以支持百万级数据量，性能稳定
   - 分片后：每个分片最多约10,000条数据，避免bigkey，查询时需要合并结果
9. **分片迁移**：启用分片时会自动迁移现有数据，迁移过程可能耗时，建议在低峰期操作

## 数据结构

### RankingItem

```java
public class RankingItem {
    private String memberId;    // 成员ID
    private Double score;       // 分数
    private Long rank;          // 排名（从1开始）
    private Long updateTime;    // 更新时间戳
    private String extraData;   // 扩展数据（JSON格式）
}
```

## 技术实现

- **底层数据结构**：Redis Sorted Set (ZSet)
- **时间复杂度**：
  - 添加/更新：O(log N)
  - 查询排名：O(log N)
  - 获取排行榜：O(log N + M)，M为返回数量
- **存储优化**：
  - **分数倍数**：使用 1e8 (100,000,000)，支持最大 99,999,999 的分数范围（约1亿）
    - 满足充值金额等业务场景（100万 × 100倍 = 1亿）
  - **时间戳处理**：
    - 使用毫秒时间戳的后8位 + 纳秒时间戳的后3位
    - 组合成11位时间戳后缀，确保唯一性
    - 支持约2.7小时的时间窗口，可处理同一毫秒内的并发情况
  - **分数计算公式**：
    ```
    最终分数 = 原始分数 × 1e8 + (1e11 - 时间戳后缀) / 1e8
    ```
  - **排序规则**：既保留分数大小关系，又支持时间排序（相同分数时，先达到的排在前面）
  - **大用户量支持**：
    - 通过纳秒级时间戳处理并发，确保高并发场景下的排序稳定性
    - **自动分片机制**：当用户量超过10,000时，自动启用分片，将数据分散到64个zset中，避免bigkey问题
    - 分片策略：根据memberId的hash值进行分片，确保同一用户始终在同一分片

## 扩展性

组件设计支持多种扩展场景：

1. **多业务类型**：通过 `bizType` 区分不同业务
2. **多排行榜实例**：通过 `bizId` 区分同一业务下的不同实例
3. **自定义排序**：支持升序/降序排序
4. **灵活查询**：支持排名查询、分数查询、区间统计等

## 最佳实践

1. **合理设置过期时间**：根据业务需求设置排行榜过期时间
2. **使用批量操作**：批量更新时使用批量接口，提升性能
3. **分页查询**：避免一次性查询过多数据，使用分页查询
4. **异常处理**：组件内部已处理异常，返回默认值，业务层可根据需要补充处理
5. **日志监控**：组件已记录详细日志，便于问题排查和性能监控

