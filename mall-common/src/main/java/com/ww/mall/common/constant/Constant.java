package com.ww.mall.common.constant;

/**
 * @description: 系统常量
 * @author: ww
 * @create: 2021-05-13 10:37
 */
public class Constant {

    public final static String USER_TOKEN = "token";
    public final static String TEMP_USER_KEY = "temp-user-key";
    public final static Integer TEMP_USER_COOKIE_TIMEOUT = 30 * 24 * 60 * 60;

    public final static String TRACE_ID = "traceId";

    public final static String ENCRYPT_HEADER = "mall-encrypt";

    public final static String SMS_CODE_CACHE_PREFIX = "sms:code:";

    /**
     * 远程调用标识【相应结果不加密】
     */
    public final static String FEIGN_FLAG = "feign_flag";

    /**
     * 参数加密key
     */
    public static final String SECRET_KEY = "ww6933@sina.com.";

    public interface MsgLogStatus {
        /**
         * 消息投递中 (消息成功发送到exchange)
         */
        Integer DELIVERING = 0;
        /**
         * 投递成功（消息成功从exchange发送到queue）
         */
        Integer DELIVER_SUCCESS = 1;
        /**
         * 投递失败（消息投递到exchange、queue失败）
         */
        Integer DELIVER_FAIL = 2;
        /**
         * 已消费（消息成功被client消费）
         */
        Integer CONSUMED_SUCCESS = 3;
        /**
         * 消费失败（client消费失败）
         */
        Integer CONSUMED_FAIL = 4;
    }

    /**
     * 超级管理员ID
     */
    public static final int SUPER_ADMIN = 1;

    /**
     * 用户端超级管理员角色ID
     */
    public static final Long SUPER_ROLE = 1L;

    /**
     * 系统中心端ID
     */
    public static final Long SYS_CENTER_ID = 1L;

    /**
     * 数据权限过滤
     */
    public static final String SQL_FILTER = "sql_filter";

    /**
     * 当前页码
     */
    public static final String PAGE = "page";

    /**
     * 每页显示记录数
     */
    public static final String LIMIT = "limit";

    /**
     * 排序字段
     */
    public static final String ORDER_FIELD = "sidx";

    /**
     * 排序方式
     */
    public static final String ORDER = "order";

    /**
     * 升序
     */
    public static final String ASC = "asc";

    /**
     * 降序
     */
    public static final String DESC = "desc";

    /**
     * app版本更新提示
     */
    public static final String UPDATE_TIP = "当前版本过低，请及时更新最新版本app!";

    /**
     * 后台用户类型
     */
    public enum AdminType {
        /**
         * 后台管理员
         */
        ADMIN(0),
        /**
         * 医生
         */
        DOCTOR(1),
        /**
         * 销售
         */
        SALESMAN(2);

        private int value;

        AdminType(int value){this.value = value;}

        public int getValue() {
            return value;
        }
    }

    /**
     * 菜单类型
     */
    public enum SystemType {
        /**
         * 总管理后台
         */
        ADMIN_CENTER(0),
        /**
         * 分中心后台
         */
        SUB_CENTER(1);

        private int value;

        SystemType(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    /**
     * 菜单类型
     */
    public enum MenuType {
        /**
         * 目录
         */
        CATALOG(0),
        /**
         * 菜单
         */
        MENU(1),
        /**
         * 按钮
         */
        BUTTON(2);

        private int value;

        MenuType(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

}
