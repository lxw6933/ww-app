package com.ww.app.member.entity.mongo;

import com.ww.app.member.enums.SignType;
import com.ww.app.mongodb.common.BaseDoc;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.springframework.data.mongodb.core.mapping.Document;

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
    private SignType signType;

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

}
