package com.ww.mall.utils;

/**
 * @description: 快递物流信息查询api
 * https://www.showapi.com/apiGateway/view/64?skuid=5ef065e16e36b58b13b16678
 * @author: ww
 * @create: 2021-05-25 15:23
 */
public class DeliveryQueryUtils {

    /**
     * EXPRESS_APP_ID 122703
     */
    private static final String EXPRESS_APP_ID = "122773";
    /**
     * 电商加密私钥，快递鸟提供，注意保管，不要泄漏 85de8f4744a943d69699f09bea711d7d
     */
    private static final String EXPRESS_APP_SECRET = "87a73283ba34463b9bd7c9d50b277594";
    /**
     * 物流信息请求url
     */
    private static final String LOGISTICS_LIST_URL = "http://route.showapi.com/64-19";
    /**
     * 物流公司列表请求url
     */
    private static final String EXPRESS_COMPANY_LIST_URL = "http://route.showapi.com/64-20";

    private DeliveryQueryUtils() {
    }

    public static void main(String[] args) {
        System.out.println(getExpressList(""));
    }

    /**
     * 查询物流公司信息列表
     *
     * @param expName 物流公司名称模糊查询
     * @return 物流公司列表
     */
    public static String getExpressList(String expName) {
        return getExpressList(expName, "", "");
    }

    /**
     * 查询物流公司信息列表
     *
     * @param expName 物流公司名称模糊查询
     * @param maxSize 每页显示数据量
     * @param page 当前页
     * @return 物流公司列表
     */
    public static String getExpressList(String expName, String maxSize, String page) {
//        return new ShowApiRequest(EXPRESS_COMPANY_LIST_URL, EXPRESS_APP_ID, EXPRESS_APP_SECRET)
//                .addTextPara("expName", expName)
//                .addTextPara("maxSize", maxSize)
//                .addTextPara("page", page)
//                .post();
        return null;
    }

    /**
     * 根据订单号查询物流信息
     * expressCode 快递公司字母简称,可以从"快递公司列表"或"快递公司查询" 接口中查到该信息 如不知道快递公司名,可以使用"auto"代替,此时将自动识别快递单号 【查询顺丰时，为了保证效率，请尽量提供寄件人或者收件人查询】
     * 参考 https://www.showapi.com/book/view/3157/3
     *
     * @param expressCode 快递公司识别码
     * @param expressNo   订单号
     * @return 物流信息
     */
    public static String getLogisticsInfoByJson(String expressCode, String expressNo) {
        return getLogisticsInfoByJson(expressCode, expressNo, "");
    }

    /**
     * 根据订单号查询物流信息
     *
     * @param expressCode 快递公司识别码
     * @param expressNo   订单号
     * @param phone       手机尾号后四位，顺丰快递必须填写本字段【寄件人手机号或者收件人手机号】
     * @return 物流信息
     */
    public static String getLogisticsInfoByJson(String expressCode, String expressNo, String phone) {
//        return new ShowApiRequest(LOGISTICS_LIST_URL, EXPRESS_APP_ID, EXPRESS_APP_SECRET)
//                .addTextPara("com", expressCode)
//                .addTextPara("nu", expressNo)
//                .addTextPara("phone", phone)
//                .post();
        return null;
    }

}
