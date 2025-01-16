package com.ww.app.im.entity;

import com.ww.app.mongodb.common.BaseDoc;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;

/**
 * @author ww
 * @create 2025-01-16- 16:21
 * @description:
 */
@Data
@Document
@EqualsAndHashCode(callSuper = true)
public class GroupMember extends BaseDoc {

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 加入时间
     */
    private Date joinTime;

    /**
     * 成员角色（1：普通成员 2：管理员 3：群主）
     */
    private int role;

}
