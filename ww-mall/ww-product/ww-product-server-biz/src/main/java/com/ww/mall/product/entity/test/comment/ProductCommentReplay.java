package com.ww.mall.product.entity.test.comment;

import com.ww.app.mongodb.common.BaseDoc;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * @author ww
 * @create 2023-07-29- 10:48
 * @description:
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Document("product_comment_replay")
public class ProductCommentReplay extends BaseDoc {

    /**
     * 评论id
     */
    private Long commentId;

    /**
     * 回复id
     */
    private Long replyId;

}
