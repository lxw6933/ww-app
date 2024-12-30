package com.ww.app.seckill.view.bo;

import com.ww.app.common.annotation.Sensitive;
import com.ww.app.common.enums.SensitiveDataType;
import lombok.Data;

/**
 * @author ww
 * @create 2024-05-14 23:46
 * @description:
 */
@Data
public class UserInfoVO {

    @Sensitive(type = SensitiveDataType.USERNAME)
    private String username;

    @Sensitive(type = SensitiveDataType.PASSWORD)
    private String password;

    @Sensitive(type = SensitiveDataType.EMAIL)
    private String email;

    @Sensitive(type = SensitiveDataType.PHONE)
    private String mobile;

    @Sensitive(type = SensitiveDataType.ID_CARD)
    private String idCard;
}
