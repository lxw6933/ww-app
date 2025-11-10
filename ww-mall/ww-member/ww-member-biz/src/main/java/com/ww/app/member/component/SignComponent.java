package com.ww.app.member.component;

import com.ww.app.common.exception.ApiException;
import com.ww.app.member.component.key.SignRedisKeyBuilder;
import com.ww.app.member.entity.mongo.MemberSignRecord;
import com.ww.app.member.enums.SignPeriod;
import com.ww.app.member.strategy.sign.AbstractSignStrategy;
import com.ww.app.member.strategy.sign.SignStrategyFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.DecoderException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author ww
 * @create 2025-10-16 17:03
 * @description:
 */
@Slf4j
@Component
public class SignComponent {

    @Resource
    private SignRedisKeyBuilder signRedisKeyBuilder;

    @Resource
    private MongoTemplate mongoTemplate;

    @Resource
    private SignStrategyFactory signStrategyFactory;

    public AbstractSignStrategy getDefaultStrategy() {
        return signStrategyFactory.getDefaultStrategy();
    }

    /**
     * 获取对应的签名策略实现类
     */
    public AbstractSignStrategy getStrategy(SignPeriod type) {
        return signStrategyFactory.getStrategy(type);
    }

    /**
     * 查询签到历史记录
     */
    public List<Boolean> getPeriodSignDetailFromHistory(Long memberId, String periodKey) {
        MemberSignRecord record = mongoTemplate.findOne(MemberSignRecord.buildQuery(memberId, periodKey), MemberSignRecord.class);
        if (record == null) {
            return Collections.emptyList();
        }
        AbstractSignStrategy strategy = signStrategyFactory.getStrategy(record.getSignPeriod());
        return decodeHexBitmapToBooleans(record.getBitmap(), strategy.getBitCount(strategy.getEndDate(periodKey)));
    }

    /**
     * 将 Hex 位图转换为布尔列表；按 Redis 位序（每字节高位在前）解释。
     */
    public List<Boolean> decodeHexBitmapToBooleans(String hexBitmap, int periodDays) {
        List<Boolean> result = new ArrayList<>(periodDays);
        byte[] bytes;
        try {
            bytes = MemberSignRecord.decodeBitmap(hexBitmap);
        } catch (DecoderException e) {
            log.warn("解码签到位图 Hex 失败，periodDays={}，返回空列表", periodDays, e);
            throw new ApiException("解码签到位图失败");
        }
        for (int i = 0; i < periodDays; i++) {
            int byteIndex = i / 8;
            int bitInByte = 7 - (i % 8); // 高位在前
            boolean signed = false;
            if (byteIndex < bytes.length) {
                signed = ((bytes[byteIndex] >> bitInByte) & 1) == 1;
            }
            result.add(signed);
        }
        return result;
    }

    /**
     * 将 byte[] 重新放回 Redis bitmap
     * 
     * @param memberSignRecord 签到记录
     * @return 是否成功
     */
    public boolean restoreSignBitmap(MemberSignRecord memberSignRecord) {
        if (memberSignRecord == null) {
            return false;
        }

        String signKey = signRedisKeyBuilder.buildSignKey(memberSignRecord.getMemberId(), memberSignRecord.getPeriodKey());

        try {
            byte[] bitmapBytes = MemberSignRecord.decodeBitmap(memberSignRecord.getBitmap());
            return getDefaultStrategy().restoreSignBitmap(signKey, bitmapBytes);
        } catch (Exception e) {
            log.error("恢复签到 bitmap 失败，signKey={}", signKey, e);
            return false;
        }
    }

}
