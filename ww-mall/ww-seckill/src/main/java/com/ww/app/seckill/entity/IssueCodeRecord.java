package com.ww.app.seckill.entity;

import com.ww.app.mongodb.common.BaseDoc;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

/**
 * @author ww
 * @create 2024-08-27- 14:18
 * @description:
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Document("issue_code_record")
public class IssueCodeRecord extends BaseDoc {

    private String outOrderCode;

    private List<String> codes;

    private String issueTime;

}
