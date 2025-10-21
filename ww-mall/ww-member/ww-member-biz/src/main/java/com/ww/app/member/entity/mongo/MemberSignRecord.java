package com.ww.app.member.entity.mongo;

import com.ww.app.member.enums.SignPeriod;
import com.ww.app.mongodb.common.BaseDoc;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

/**
 * @author ww
 * @create 2025-10-15 16:04
 * @description: 用户签到记录表
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Builder
@Accessors(chain = true)
@Document(collection = "member_sign_record")
public class MemberSignRecord extends BaseDoc {

    /**
     * 用户id
     */
    private Long memberId;

    /**
     * 签到类型
     */
    private SignPeriod signPeriod;

    /**
     * 周期标识 如202510或2025W42
     */
    private String periodKey;

    /**
     * 签到位图（Hex或Base64）
     */
    private String bitmap;

    /**
     * 周期内签到天数
     */
    private int totalSignDays;

    /**
     * 当前连续签到天数
     */
    private int currentStreakSignDays;

    /**
     * 补签次数
     */
    private int retroSignDays;

    public static Query buildQuery(Long memberId, String periodKey) {
        return new Query().addCriteria(Criteria.where("memberId").is(memberId).and("periodKey").is(periodKey));
    }

    public static String encodeBitmap(byte[] bitmap) {
        return Hex.encodeHexString(bitmap);
    }

    public static byte[] decodeBitmap(String bitmap) throws DecoderException {
        return Hex.decodeHex(bitmap.trim().toCharArray());
    }

}
