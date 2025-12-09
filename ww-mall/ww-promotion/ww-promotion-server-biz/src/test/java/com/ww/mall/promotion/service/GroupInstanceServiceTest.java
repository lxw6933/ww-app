package com.ww.mall.promotion.service;

import com.ww.app.common.exception.ApiException;
import com.ww.mall.promotion.controller.app.group.req.CreateGroupRequest;
import com.ww.mall.promotion.controller.app.group.req.JoinGroupRequest;
import com.ww.mall.promotion.controller.app.group.res.GroupInstanceVO;
import com.ww.mall.promotion.entity.group.GroupActivity;
import com.ww.mall.promotion.entity.group.GroupInstance;
import com.ww.mall.promotion.entity.group.GroupMember;
import com.ww.mall.promotion.enums.GroupStatus;
import com.ww.mall.promotion.key.GroupRedisKeyBuilder;
import com.ww.mall.promotion.service.group.GroupInstanceService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 拼团实例服务测试类
 * 
 * @author ww
 * @create 2025-12-08 20:00
 * @description: 测试GroupInstanceService的核心功能，包括创建拼团、加入拼团、查询等
 */
@Slf4j
@SpringBootTest
@Transactional
@Rollback
@DisplayName("拼团实例服务测试")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class GroupInstanceServiceTest {

    @Resource
    private GroupInstanceService groupInstanceService;

    @Resource
    private MongoTemplate mongoTemplate;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private GroupRedisKeyBuilder groupRedisKeyBuilder;

    // 测试数据
    private static String testActivityId;
    private static String testGroupId;
    private static final Long testUserId1 = 10001L;
    private static final Long testUserId2 = 10002L;
    private static final Long testUserId3 = 10003L;
    private static String testOrderId1;
    private static String testOrderId2;
    private static String testOrderId3;

    @BeforeEach
    void setUp() {
        log.info("========== 开始准备测试数据 ==========");
        testOrderId1 = randomOrderId();
        testOrderId2 = randomOrderId();
        testOrderId3 = randomOrderId();
        // 清理Redis测试数据
        cleanRedisTestData();
        // 清理MongoDB测试数据
        cleanMongoTestData();
    }

    @AfterEach
    void tearDown() {
        log.info("========== 清理测试数据 ==========");
        // 清理Redis测试数据
        cleanRedisTestData();
        // 清理MongoDB测试数据
        cleanMongoTestData();
    }

    /**
     * 测试创建拼团 - 正常流程
     */
    @Test
    @Order(1)
    @DisplayName("测试创建拼团 - 正常流程")
    void testCreateGroup_Success() {
        log.info("========== 测试创建拼团 - 正常流程 ==========");

        // 1. 创建测试活动
        GroupActivity activity = createTestActivity();
        testActivityId = activity.getId();
        log.info("创建测试活动成功: activityId={}", testActivityId);

        // 2. 创建拼团请求
        CreateGroupRequest request = buildCreateRequest(testActivityId, testUserId1, testOrderId1);

        // 3. 执行创建拼团
        GroupInstanceVO vo = groupInstanceService.createGroup(request);
        testGroupId = vo.getId();

        // 4. 验证结果
        assertNotNull(vo, "拼团实例不能为空");
        assertNotNull(vo.getId(), "拼团ID不能为空");
        assertEquals(GroupStatus.OPEN.getCode(), vo.getStatus(), "拼团状态应为OPEN");
        assertEquals(2, vo.getRequiredSize(), "需要人数应为2");
        assertEquals(1, vo.getCurrentSize(), "当前人数应为1");
        assertEquals(1, vo.getRemainingSlots(), "剩余名额应为1");
        assertEquals(testUserId1, vo.getLeaderUserId(), "团长用户ID应为testUserId1");
        assertEquals(testActivityId, vo.getActivityId(), "活动ID应匹配");

        log.info("创建拼团成功: groupId={}, status={}, remainingSlots={}", 
                vo.getId(), vo.getStatus(), vo.getRemainingSlots());
    }

    /**
     * 测试创建拼团 - 订单号为空
     */
    @Test
    @Order(2)
    @DisplayName("测试创建拼团 - 订单号为空")
    void testCreateGroup_OrderIdEmpty() {
        log.info("========== 测试创建拼团 - 订单号为空 ==========");

        // 1. 创建测试活动
        GroupActivity activity = createTestActivity();

        // 2. 创建拼团请求（订单号为空）
        CreateGroupRequest request = new CreateGroupRequest();
        request.setActivityId(activity.getId());
        request.setUserId(testUserId1);
        request.setOrderId(""); // 空订单号
        request.setOrderInfo("{}");

        // 3. 验证异常
        ApiException exception = assertThrows(ApiException.class, () -> groupInstanceService.createGroup(request), "应该抛出订单号不能为空的异常");

        assertTrue(exception.getMessage().contains("订单号"), 
                "异常信息应包含订单号相关提示");
        log.info("订单号为空测试通过，异常信息: {}", exception.getMessage());
    }

    /**
     * 测试创建拼团 - 活动不存在
     */
    @Test
    @Order(3)
    @DisplayName("测试创建拼团 - 活动不存在")
    void testCreateGroup_ActivityNotExists() {
        log.info("========== 测试创建拼团 - 活动不存在 ==========");

        // 1. 创建拼团请求（活动ID不存在）
        CreateGroupRequest request = buildCreateRequest("NON_EXIST_ACTIVITY", testUserId1, testOrderId1);

        // 2. 验证异常
        ApiException exception = assertThrows(ApiException.class, () -> groupInstanceService.createGroup(request), "应该抛出活动不存在的异常");

        assertTrue(exception.getMessage().contains("活动不存在"), 
                "异常信息应包含活动不存在");
        log.info("活动不存在测试通过，异常信息: {}", exception.getMessage());
    }

    /**
     * 测试创建拼团 - 库存不足
     */
    @Test
    @Order(4)
    @DisplayName("测试创建拼团 - 库存不足")
    void testCreateGroup_StockInsufficient() {
        log.info("========== 测试创建拼团 - 库存不足 ==========");

        // 1. 创建测试活动（库存为1）
        GroupActivity activity = createTestActivity();
        activity.setTotalStock(1);
        mongoTemplate.save(activity);

        // 2. 初始化Redis库存为0
        String stockKey = groupRedisKeyBuilder.buildActivityStockKey(activity.getId());
        stringRedisTemplate.opsForValue().set(stockKey, "0");

        // 3. 创建拼团请求
        CreateGroupRequest request = buildCreateRequest(activity.getId(), testUserId1, testOrderId1);

        // 4. 验证异常
        ApiException exception = assertThrows(ApiException.class, () -> groupInstanceService.createGroup(request), "应该抛出库存不足的异常");

        assertTrue(exception.getMessage().contains("库存"), 
                "异常信息应包含库存相关提示");
        log.info("库存不足测试通过，异常信息: {}", exception.getMessage());
    }

    /**
     * 测试加入拼团 - 正常流程
     */
    @Test
    @Order(5)
    @DisplayName("测试加入拼团 - 正常流程")
    void testJoinGroup_Success() {
        log.info("========== 测试加入拼团 - 正常流程 ==========");

        // 1. 创建测试活动
        GroupActivity activity = createTestActivity();
        testActivityId = activity.getId();

        // 2. 创建拼团
        CreateGroupRequest createRequest = buildCreateRequest(testActivityId, testUserId1, testOrderId1);

        GroupInstanceVO createdGroup = groupInstanceService.createGroup(createRequest);
        testGroupId = createdGroup.getId();
        log.info("创建拼团成功: groupId={}", testGroupId);

        // 3. 加入拼团
        JoinGroupRequest joinRequest = buildJoinRequest(testGroupId, testUserId2, testOrderId2);

        GroupInstanceVO joinedGroup = groupInstanceService.joinGroup(joinRequest);

        // 4. 验证结果
        assertNotNull(joinedGroup, "拼团实例不能为空");
        assertEquals(GroupStatus.SUCCESS.getCode(), joinedGroup.getStatus(), 
                "拼团状态应为SUCCESS（拼团完成）");
        assertEquals(2, joinedGroup.getCurrentSize(), "当前人数应为2");
        assertEquals(0, joinedGroup.getRemainingSlots(), "剩余名额应为0");
        assertNotNull(joinedGroup.getCompleteTime(), "完成时间不能为空");

        log.info("加入拼团成功: groupId={}, status={}, currentSize={}", 
                joinedGroup.getId(), joinedGroup.getStatus(), joinedGroup.getCurrentSize());
    }

    /**
     * 测试加入拼团 - 订单号为空
     */
    @Test
    @Order(6)
    @DisplayName("测试加入拼团 - 订单号为空")
    void testJoinGroup_OrderIdEmpty() {
        log.info("========== 测试加入拼团 - 订单号为空 ==========");

        // 1. 创建测试活动并拼团
        GroupActivity activity = createTestActivity();
        CreateGroupRequest createRequest = buildCreateRequest(activity.getId(), testUserId1, testOrderId1);
        GroupInstanceVO createdGroup = groupInstanceService.createGroup(createRequest);

        // 2. 加入拼团请求（订单号为空）
        JoinGroupRequest joinRequest = new JoinGroupRequest();
        joinRequest.setGroupId(createdGroup.getId());
        joinRequest.setUserId(testUserId2);
        joinRequest.setOrderId(""); // 空订单号
        joinRequest.setOrderInfo("{}");

        // 3. 验证异常
        ApiException exception = assertThrows(ApiException.class, () -> groupInstanceService.joinGroup(joinRequest), "应该抛出订单号不能为空的异常");

        assertTrue(exception.getMessage().contains("订单号"), 
                "异常信息应包含订单号相关提示");
        log.info("订单号为空测试通过，异常信息: {}", exception.getMessage());
    }

    /**
     * 测试加入拼团 - 拼团不存在
     */
    @Test
    @Order(7)
    @DisplayName("测试加入拼团 - 拼团不存在")
    void testJoinGroup_GroupNotExists() {
        log.info("========== 测试加入拼团 - 拼团不存在 ==========");

        // 1. 加入拼团请求（拼团ID不存在）
        JoinGroupRequest joinRequest = new JoinGroupRequest();
        joinRequest.setGroupId("NON_EXIST_GROUP");
        joinRequest.setUserId(testUserId2);
        joinRequest.setOrderId(testOrderId2);
        joinRequest.setOrderInfo("{}");

        // 2. 验证异常
        ApiException exception = assertThrows(ApiException.class, () -> groupInstanceService.joinGroup(joinRequest), "应该抛出拼团不存在的异常");

        assertTrue(exception.getMessage().contains("拼团不存在"), 
                "异常信息应包含拼团不存在");
        log.info("拼团不存在测试通过，异常信息: {}", exception.getMessage());
    }

    /**
     * 测试加入拼团 - 重复加入（幂等性）
     */
    @Test
    @Order(8)
    @DisplayName("测试加入拼团 - 重复加入（幂等性）")
    void testJoinGroup_DuplicateJoin() {
        log.info("========== 测试加入拼团 - 重复加入（幂等性） ==========");

        // 1. 创建测试活动并拼团
        GroupActivity activity = createTestActivity();
        CreateGroupRequest createRequest = buildCreateRequest(activity.getId(), testUserId1, testOrderId1);
        GroupInstanceVO createdGroup = groupInstanceService.createGroup(createRequest);

        // 2. 第一次加入拼团
        JoinGroupRequest joinRequest = buildJoinRequest(createdGroup.getId(), testUserId2, testOrderId2);

        GroupInstanceVO firstJoin = groupInstanceService.joinGroup(joinRequest);
        assertEquals(GroupStatus.SUCCESS.getCode(), firstJoin.getStatus(), 
                "第一次加入应该成功，拼团完成");

        // 3. 第二次使用相同订单号加入（应该失败）
        ApiException exception = assertThrows(ApiException.class, () -> groupInstanceService.joinGroup(joinRequest), "应该抛出订单已存在的异常");

        assertTrue(exception.getMessage().contains("订单已存在"), 
                "异常信息应包含订单已存在");
        log.info("重复加入测试通过，异常信息: {}", exception.getMessage());
    }

    /**
     * 测试加入拼团 - 拼团已满
     */
    @Test
    @Order(9)
    @DisplayName("测试加入拼团 - 拼团已满")
    void testJoinGroup_GroupFull() {
        log.info("========== 测试加入拼团 - 拼团已满 ==========");

        // 1. 创建测试活动（2人拼团）
        GroupActivity activity = createTestActivity();
        CreateGroupRequest createRequest = buildCreateRequest(activity.getId(), testUserId1, testOrderId1);
        GroupInstanceVO createdGroup = groupInstanceService.createGroup(createRequest);

        // 2. 第一个用户加入（拼团完成）
        JoinGroupRequest joinRequest1 = buildJoinRequest(createdGroup.getId(), testUserId2, testOrderId2);
        groupInstanceService.joinGroup(joinRequest1);

        // 3. 第二个用户尝试加入（应该失败）
        JoinGroupRequest joinRequest2 = buildJoinRequest(createdGroup.getId(), testUserId3, testOrderId3);

        ApiException exception = assertThrows(ApiException.class, () -> groupInstanceService.joinGroup(joinRequest2), "应该抛出拼团已满的异常");

        assertTrue(exception.getMessage().contains("没有剩余名额") || 
                   exception.getMessage().contains("拼团未开放"), 
                "异常信息应包含相关提示");
        log.info("拼团已满测试通过，异常信息: {}", exception.getMessage());
    }

    /**
     * 测试查询拼团详情
     */
    @Test
    @Order(10)
    @DisplayName("测试查询拼团详情")
    void testGetGroupDetail() {
        log.info("========== 测试查询拼团详情 ==========");

        // 1. 创建测试活动并拼团
        GroupActivity activity = createTestActivity();
        CreateGroupRequest createRequest = buildCreateRequest(activity.getId(), testUserId1, testOrderId1);
        GroupInstanceVO createdGroup = groupInstanceService.createGroup(createRequest);

        // 2. 查询拼团详情
        GroupInstanceVO detail = groupInstanceService.getGroupDetail(createdGroup.getId());

        // 3. 验证结果
        assertNotNull(detail, "拼团详情不能为空");
        assertEquals(createdGroup.getId(), detail.getId(), "拼团ID应匹配");
        assertEquals(GroupStatus.OPEN.getCode(), detail.getStatus(), "拼团状态应为OPEN");
        assertEquals(2, detail.getRequiredSize(), "需要人数应为2");
        assertEquals(1, detail.getCurrentSize(), "当前人数应为1");
        assertEquals(1, detail.getRemainingSlots(), "剩余名额应为1");

        log.info("查询拼团详情成功: groupId={}, status={}, currentSize={}, remainingSlots={}", 
                detail.getId(), detail.getStatus(), detail.getCurrentSize(), detail.getRemainingSlots());
    }

    /**
     * 测试查询用户参与的拼团列表
     */
    @Test
    @Order(11)
    @DisplayName("测试查询用户参与的拼团列表")
    void testGetUserGroups() {
        log.info("========== 测试查询用户参与的拼团列表 ==========");

        // 1. 创建测试活动
        GroupActivity activity = createTestActivity();

        // 2. 用户1创建拼团
        CreateGroupRequest createRequest = buildCreateRequest(activity.getId(), testUserId1, testOrderId1);
        GroupInstanceVO createdGroup = groupInstanceService.createGroup(createRequest);

        // 3. 查询用户1的拼团列表
        List<GroupInstanceVO> userGroups = groupInstanceService.getUserGroups(testUserId1);

        // 4. 验证结果
        assertNotNull(userGroups, "拼团列表不能为空");
        assertFalse(userGroups.isEmpty(), "拼团列表应至少包含1个");
        assertEquals(createdGroup.getId(), userGroups.get(0).getId(), 
                "拼团ID应匹配");

        log.info("查询用户拼团列表成功: userId={}, groupCount={}", 
                testUserId1, userGroups.size());
    }

    /**
     * 测试查询活动下的拼团列表
     */
    @Test
    @Order(12)
    @DisplayName("测试查询活动下的拼团列表")
    void testGetActivityGroups() {
        log.info("========== 测试查询活动下的拼团列表 ==========");

        // 1. 创建测试活动
        GroupActivity activity = createTestActivity();

        // 2. 创建多个拼团
        for (int i = 1; i <= 3; i++) {
            CreateGroupRequest createRequest = buildCreateRequest(activity.getId(), testUserId1 + i, "ORDER_TEST_" + i);
            groupInstanceService.createGroup(createRequest);
        }

        // 3. 查询活动下的拼团列表
        List<GroupInstanceVO> activityGroups = groupInstanceService.getActivityGroups(
                activity.getId(), GroupStatus.OPEN.getCode());

        // 4. 验证结果
        assertNotNull(activityGroups, "拼团列表不能为空");
        assertTrue(activityGroups.size() >= 3, "拼团列表应至少包含3个");

        log.info("查询活动拼团列表成功: activityId={}, groupCount={}", 
                activity.getId(), activityGroups.size());
    }

    /**
     * 测试完整拼团流程 - 创建到完成
     */
    @Test
    @Order(13)
    @DisplayName("测试完整拼团流程 - 创建到完成")
    void testCompleteGroupFlow() {
        log.info("========== 测试完整拼团流程 - 创建到完成 ==========");

        // 1. 创建测试活动（2人拼团）
        GroupActivity activity = createTestActivity();

        // 2. 用户1创建拼团
        CreateGroupRequest createRequest = buildCreateRequest(activity.getId(), testUserId1, testOrderId1);
        
        GroupInstanceVO createdGroup = groupInstanceService.createGroup(createRequest);
        assertEquals(GroupStatus.OPEN.getCode(), createdGroup.getStatus(), 
                "创建后状态应为OPEN");
        assertEquals(1, createdGroup.getRemainingSlots(), "剩余名额应为1");
        log.info("步骤1: 用户1创建拼团成功 - groupId={}, remainingSlots={}", 
                createdGroup.getId(), createdGroup.getRemainingSlots());

        // 3. 用户2加入拼团
        JoinGroupRequest joinRequest = buildJoinRequest(createdGroup.getId(), testUserId2, testOrderId2);

        GroupInstanceVO completedGroup = groupInstanceService.joinGroup(joinRequest);
        assertEquals(GroupStatus.SUCCESS.getCode(), completedGroup.getStatus(), 
                "加入后状态应为SUCCESS");
        assertEquals(0, completedGroup.getRemainingSlots(), "剩余名额应为0");
        assertEquals(2, completedGroup.getCurrentSize(), "当前人数应为2");
        assertNotNull(completedGroup.getCompleteTime(), "完成时间不能为空");
        log.info("步骤2: 用户2加入拼团成功 - groupId={}, status={}, completeTime={}", 
                completedGroup.getId(), completedGroup.getStatus(), completedGroup.getCompleteTime());

        // 4. 验证拼团详情
        GroupInstanceVO detail = groupInstanceService.getGroupDetail(completedGroup.getId());
        assertEquals(GroupStatus.SUCCESS.getCode(), detail.getStatus(), 
                "查询详情状态应为SUCCESS");
        log.info("步骤3: 查询拼团详情成功 - status={}", detail.getStatus());
    }

    /**
     * 创建测试活动
     */
    private GroupActivity createTestActivity() {
        GroupActivity activity = new GroupActivity();
        activity.setName("测试拼团活动-" + System.currentTimeMillis());
        activity.setDescription("这是一个测试活动");
        activity.setSpuId(1001L);
        activity.setSkuId(2001L);
        activity.setGroupPrice(new BigDecimal("99.00"));
        activity.setOriginalPrice(new BigDecimal("199.00"));
        activity.setRequiredSize(2); // 2人拼团
        activity.setExpireHours(24); // 24小时有效期
        activity.setStartTime(new Date());
        activity.setEndTime(new Date(System.currentTimeMillis() + 7 * 24 * 3600 * 1000L)); // 7天后结束
        activity.setStatus(1); // 进行中
        activity.setTotalStock(1000);
        activity.setSoldCount(0);
        activity.setLimitPerUser(1);
        activity.setEnabled(1);
        activity.setImageUrl("https://example.com/test.jpg");
        activity.setSortWeight(100);

        activity = mongoTemplate.save(activity);

        // 初始化Redis库存
        String stockKey = groupRedisKeyBuilder.buildActivityStockKey(activity.getId());
        stringRedisTemplate.opsForValue().set(stockKey, String.valueOf(activity.getTotalStock()));

        return activity;
    }

    private CreateGroupRequest buildCreateRequest(String activityId, Long userId, String orderId) {
        CreateGroupRequest request = new CreateGroupRequest();
        request.setActivityId(activityId);
        request.setUserId(userId);
        request.setOrderId(orderId);
        request.setOrderInfo("{\"amount\":99.00,\"quantity\":1}");
        return request;
    }

    private JoinGroupRequest buildJoinRequest(String groupId, Long userId, String orderId) {
        JoinGroupRequest request = new JoinGroupRequest();
        request.setGroupId(groupId);
        request.setUserId(userId);
        request.setOrderId(orderId);
        request.setOrderInfo("{\"amount\":99.00,\"quantity\":1}");
        return request;
    }

    private String randomOrderId() {
        return "ORDER_TEST_" + System.nanoTime();
    }

    /**
     * 清理Redis测试数据
     */
    private void cleanRedisTestData() {
        try {
            if (testGroupId != null) {
                String metaKey = groupRedisKeyBuilder.buildGroupMetaKey(testGroupId);
                String slotsKey = groupRedisKeyBuilder.buildGroupSlotsKey(testGroupId);
                String membersKey = groupRedisKeyBuilder.buildGroupMembersKey(testGroupId);
                String ordersKey = groupRedisKeyBuilder.buildGroupOrdersKey(testGroupId);
                stringRedisTemplate.delete(metaKey);
                stringRedisTemplate.delete(slotsKey);
                stringRedisTemplate.delete(membersKey);
                stringRedisTemplate.delete(ordersKey);
            }
            if (testActivityId != null) {
                String stockKey = groupRedisKeyBuilder.buildActivityStockKey(testActivityId);
                stringRedisTemplate.delete(stockKey);
            }
            String expiryIndexKey = groupRedisKeyBuilder.buildExpiryIndexKey();
            stringRedisTemplate.delete(expiryIndexKey);
        } catch (Exception e) {
            log.warn("清理Redis测试数据失败", e);
        }
    }

    /**
     * 清理MongoDB测试数据
     */
    private void cleanMongoTestData() {
        try {
            if (testGroupId != null) {
                Query groupQuery = GroupInstance.buildIdQuery(testGroupId);
                mongoTemplate.remove(groupQuery, GroupInstance.class);
                
                Query memberQuery = GroupMember.buildGroupInstanceIdQuery(testGroupId);
                mongoTemplate.remove(memberQuery, GroupMember.class);
            }
            if (testActivityId != null) {
                mongoTemplate.remove(GroupActivity.buildIdQuery(testActivityId), GroupActivity.class);
            }
        } catch (Exception e) {
            log.warn("清理MongoDB测试数据失败", e);
        }
    }

}

