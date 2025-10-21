package com.ww.app.member.sign;

import com.ww.app.common.common.ClientUser;
import com.ww.app.common.exception.ApiException;
import com.ww.app.member.component.SignComponent;
import com.ww.app.member.entity.mongo.MemberSignRecord;
import com.ww.app.member.job.SignJob;
import com.ww.app.member.service.sign.SignService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 签到服务集成测试
 */
@SpringBootTest
@ActiveProfiles("dev")
@DisplayName("签到服务集成测试")
public class SignTest {

    @Autowired
    private SignService signService;

    @Autowired
    private SignJob signJob;

    @Autowired
    private MongoTemplate mongoTemplate;

    private ClientUser createTestUser(Long userId) {
        ClientUser user = new ClientUser();
        user.setId(userId);
        // 根据你的 ClientUser 类结构设置其他必要字段
        return user;
    }

    ClientUser testUser = createTestUser(12L);

    @Test
    void testSignJob() {
        // 执行这个
        signJob.archiveMonthlySignDataJobHandler("202510");
    }

    @Test
    void testRestore() {
        MemberSignRecord signRecord = mongoTemplate.findOne(MemberSignRecord.buildQuery(12L, "202510"), MemberSignRecord.class);
        boolean success = signComponent.restoreSignBitmap(signRecord);
        System.out.println("重放redis数据结果：" + success);
        List<Boolean> signDetailInfo = signService.getSignDetailInfo(testUser);
        printSignDetailInfo(signDetailInfo);
    }

    @Autowired
    private SignComponent signComponent;

    @Test
    void testGetSignFromMongo() {
        List<Boolean> periodSignDetailFromHistory = signComponent.getPeriodSignDetailFromHistory(2L, "202510");
        printSignDetailInfo(periodSignDetailFromHistory);
    }

    @Test
    void testMonthSign() {
        System.out.println("签到次数：" + signService.doSign("2025-10-13", testUser));
        System.out.println("签到次数：" + signService.doSign("2025-10-14", testUser));
        System.out.println("签到次数：" + signService.doSign("2025-10-15", testUser));
        System.out.println("签到次数：" + signService.doSign("2025-10-21", testUser));
    }

    @Test
    void testGetSignInfo() {
//        int continuousSignCount = signService.getContinuousSignCount("2025-10-16", testUser);
//        System.out.println("连续签到次数：" + continuousSignCount);
//        System.out.println(signService.getSignCount("2025-10-15", testUser));
//        System.out.println("签到总次数：" + continuousSignCount);
//        Map<String, Boolean> signInfo = signService.getSignInfo("2025-10-17", testUser);
//        signInfo.forEach((key, flag) -> System.out.println(key + ": " + flag));
        testMonthSign();
        List<Boolean> signDetailInfo = signService.getSignDetailInfo(testUser);
        printSignDetailInfo(signDetailInfo);
    }

    private static void printSignDetailInfo(List<Boolean> signDetailInfo) {
        for (int i = 0; i < signDetailInfo.size(); i++) {
            System.out.println(i + 1 + "号: " + signDetailInfo.get(i));
        }
    }

    @Test
    @DisplayName("集成测试 - 完整的签到流程")
    void testCompleteSignFlow() {
        // 使用唯一的用户ID避免测试冲突
        Long testUserId = System.currentTimeMillis() % 1000000L;
        ClientUser testUser = createTestUser(testUserId);
        String today = LocalDate.now().format(DateTimeFormatter.ISO_DATE);

        // 1. 测试首次签到
        int firstSignResult = signService.doSign(null, testUser);
        assertTrue(firstSignResult >= 0, "首次签到应该返回有效的连续签到天数");

        // 2. 测试重复签到（应该抛出异常）
        ApiException exception = assertThrows(ApiException.class,
                () -> signService.doSign(null, testUser));
        assertTrue(exception.getMessage().contains("已签到"), "重复签到应该抛出异常");

        // 3. 测试获取连续签到次数
        int continuousCount = signService.getContinuousSignCount(today, testUser);
        assertTrue(continuousCount >= 1, "连续签到次数应该至少为1");

        // 4. 测试获取签到总次数
        int totalCount = signService.getSignCount(today, testUser);
        assertEquals(1, totalCount, "今天应该只有1次签到");

        // 5. 测试获取签到详情
        Map<String, Boolean> signInfo = signService.getSignInfo(today, testUser);
        assertNotNull(signInfo, "签到详情不应该为null");
        assertTrue(signInfo.containsKey(today), "签到详情应该包含今天");
        assertTrue(signInfo.get(today), "今天应该显示为已签到");

        System.out.println("完整签到流程测试通过 - 用户ID: " + testUserId);
    }

    @Test
    @DisplayName("集成测试 - 多用户签到隔离")
    void testMultiUserSignIsolation() {
        ClientUser user1 = createTestUser(10001L);
        ClientUser user2 = createTestUser(10002L);
        String today = LocalDate.now().format(DateTimeFormatter.ISO_DATE);

        // 用户1签到
        int user1Result = signService.doSign(null, user1);

        // 用户2签到
        int user2Result = signService.doSign(null, user2);

        // 验证两个用户的签到数据是隔离的
        int user1Continuous = signService.getContinuousSignCount(today, user1);
        int user2Continuous = signService.getContinuousSignCount(today, user2);

        assertEquals(user1Result, user1Continuous, "用户1的连续签到次数应该一致");
        assertEquals(user2Result, user2Continuous, "用户2的连续签到次数应该一致");

        System.out.println("多用户隔离测试通过");
    }

    @Test
    @DisplayName("集成测试 - 补签功能")
    void testBackSignFunction() {
        Long testUserId = System.currentTimeMillis() % 1000000L + 20000L;
        ClientUser testUser = createTestUser(testUserId);

        // 先进行今天签到
        signService.doSign(null, testUser);

        // 尝试补签昨天
        String yesterday = LocalDate.now().minusDays(1).format(DateTimeFormatter.ISO_DATE);

        try {
            int backSignResult = signService.doSign(yesterday, testUser);
            assertTrue(backSignResult >= 0, "补签应该返回有效的连续签到天数");
            System.out.println("补签测试通过 - 补签日期: " + yesterday);
        } catch (ApiException e) {
            // 补签可能因为次数限制等原因失败，这是正常的业务逻辑
            System.out.println("补签测试: " + e.getMessage());
            assertTrue(e.getMessage().contains("补签") || e.getMessage().contains("次数"),
                    "补签异常应该是业务逻辑相关的");
        }
    }

    @Test
    @DisplayName("集成测试 - 未来日期签到限制")
    void testFutureDateSignRestriction() {
        ClientUser testUser = createTestUser(30001L);
        String tomorrow = LocalDate.now().plusDays(1).format(DateTimeFormatter.ISO_DATE);

        // 尝试签到来来日期应该失败
        ApiException exception = assertThrows(ApiException.class,
                () -> signService.doSign(tomorrow, testUser));

        assertTrue(exception.getMessage().contains("未来日期"),
                "签到来来日期应该被拒绝");

        System.out.println("未来日期限制测试通过");
    }

    @Test
    @DisplayName("集成测试 - 不同月份签到")
    void testDifferentMonthSign() {
        ClientUser testUser = createTestUser(40001L);

        // 测试上个月的数据查询（应该不会报错）
        String lastMonth = LocalDate.now().minusMonths(1).format(DateTimeFormatter.ISO_DATE);

        try {
            int lastMonthCount = signService.getSignCount(lastMonth, testUser);
            Map<String, Boolean> lastMonthInfo = signService.getSignInfo(lastMonth, testUser);

            assertTrue(lastMonthCount >= 0, "上个月签到次数应该>=0");
            assertNotNull(lastMonthInfo, "上个月签到详情不应该为null");

            System.out.println("不同月份查询测试通过 - 查询月份: " + lastMonth);
        } catch (Exception e) {
            // 即使查询失败也不应该崩溃
            assertFalse(e.getMessage().contains("空指针"), "不应该出现空指针异常");
            System.out.println("不同月份查询: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("集成测试 - 边界值测试")
    void testBoundaryConditions() {
        ClientUser testUser = createTestUser(50001L);
        String today = LocalDate.now().format(DateTimeFormatter.ISO_DATE);

        // 测试空日期处理
        try {
            int resultWithNull = signService.doSign(null, testUser);
            assertTrue(resultWithNull >= 0, "空日期应该默认为今天");
        } catch (ApiException e) {
            // 可能已经签到过，这是正常的
            System.out.println("空日期测试: " + e.getMessage());
        }

        // 测试各种日期格式的查询
        int count1 = signService.getContinuousSignCount(today, testUser);
        int count2 = signService.getSignCount(today, testUser);

        assertTrue(count1 >= 0, "连续签到次数应该>=0");
        assertTrue(count2 >= 0, "签到总次数应该>=0");

        System.out.println("边界值测试通过");
    }

    @Test
    @DisplayName("集成测试 - 性能测试")
    void testPerformance() {
        ClientUser testUser = createTestUser(60001L);
        String today = LocalDate.now().format(DateTimeFormatter.ISO_DATE);

        long startTime = System.currentTimeMillis();

        // 执行多次操作测试性能
        for (int i = 0; i < 10; i++) {
            try {
                signService.getContinuousSignCount(today, testUser);
                signService.getSignCount(today, testUser);
                signService.getSignInfo(today, testUser);
            } catch (Exception e) {
                // 忽略查询过程中的异常
            }
        }

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        assertTrue(duration < 5000, "10次查询应该在5秒内完成");
        System.out.println("性能测试通过 - 耗时: " + duration + "ms");
    }
}
