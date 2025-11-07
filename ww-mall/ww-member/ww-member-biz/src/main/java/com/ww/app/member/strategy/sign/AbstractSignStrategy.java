package com.ww.app.member.strategy.sign;

import com.ww.app.common.common.ClientUser;
import com.ww.app.member.component.SignComponent;
import com.ww.app.member.component.key.SignRedisKeyBuilder;
import com.ww.app.member.entity.mongo.MemberSignRecord;
import com.ww.app.member.strategy.sign.time.SignBitmapStrategy;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * @author ww
 * @create 2023-07-21- 09:16
 * @description: 签到策略抽象类
 */
public abstract class AbstractSignStrategy implements SignBitmapStrategy, SignStrategy {

    @Resource
    protected StringRedisTemplate stringRedisTemplate;

    @Resource
    protected SignRedisKeyBuilder signRedisKeyBuilder;

    @Resource
    protected SignComponent signComponent;

    @Override
    public int doSign(LocalDate date, ClientUser clientUser) {
        // 获取偏移量
        int offset = getOffset(date);

        // 构建 Key
        String signKey = buildSignKey(clientUser.getId(), date);

        // 查看是否已签到
        boolean isSigned = signComponent.isSigned(signKey, offset);
        if (isSigned) {
            return getContinuousSignCount(date, clientUser);
        }

        // 签到
        signComponent.sign(signKey, offset);

        // 处理签到奖励
        processSignReward(clientUser.getId(), date);

        // 统计连续签到的次数
        return getContinuousSignCount(date, clientUser);
    }

    @Override
    public int getContinuousSignCount(LocalDate date, ClientUser clientUser) {
        // 获取位数[当前周期内有多少位]
        int bits = getBitCount(date);

        // 获取当前位置[获取当前时间在哪个bit位上]
        int position = getOffset(date);

        // 构建 Key
        String signKey = buildSignKey(clientUser.getId(), date);

        return signComponent.getStreakSignCount(signKey, bits, position);
    }

    @Override
    public int getSignCount(LocalDate date, ClientUser clientUser) {
        // 构建 Key
        String signKey = buildSignKey(clientUser.getId(), date);

        return signComponent.getSignCount(signKey);
    }

    @Override
    public Map<String, Boolean> getSignInfo(LocalDate date, ClientUser clientUser) {
        // 构建 Key
        String signKey = buildSignKey(clientUser.getId(), date);

        // 构建一个自动排序的 Map
        Map<String, Boolean> signInfo = new TreeMap<>();

        // 获取位数
        int bits = getBitCount(date);

        // 获取签到数据
        List<Long> list = signComponent.getSignInfo(signKey, bits);
        if (list == null || list.isEmpty()) {
            return signInfo;
        }

        long v = list.get(0) == null ? 0 : list.get(0);

        // 填充签到信息
        fillSignInfo(date, v, signInfo);

        return signInfo;
    }

    @Override
    public List<Boolean> getSignDetailInfo(ClientUser clientUser) {
        // 获取日期
        LocalDate date = LocalDate.now();
        // 构建 Key
        String signKey = buildSignKey(clientUser.getId(), date);
        // 获取当前签到bitmap
        byte[] signBytes = signComponent.getSignBytes(signKey);
        if (signBytes == null) {
            return Collections.emptyList();
        }
        // 获取位数
        int bits = getBitCount(date);
        return signComponent.decodeHexBitmapToBooleans(MemberSignRecord.encodeBitmap(signBytes), bits);
    }

    /**
     * 处理签到奖励
     */
    protected abstract void processSignReward(Long userId, LocalDate date);

    /**
     * 构建签到Key
     */
    protected abstract String buildSignKey(Long userId, LocalDate date);

    /**
     * 填充签到信息
     */
    @Deprecated
    protected void fillSignInfo(LocalDate date, long bitValue, Map<String, Boolean> signInfo) {}

}