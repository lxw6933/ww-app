package com.ww.app.open.common;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Assert;
import com.alibaba.fastjson.JSON;
import com.ww.app.common.enums.GlobalResCodeConstants;
import com.ww.app.common.exception.ApiException;
import com.ww.app.common.utils.DigitalSignatureUtil;
import com.ww.app.common.utils.IdUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;

import java.util.Date;

/**
 * @author ww
 * @create 2024-05-25 10:59
 * @description:
 */
@Data
@Slf4j
@NoArgsConstructor
@AllArgsConstructor
public class BaseOpenRequest<T> {

    /**
     * 流水号唯一
     */
    private String transId;

    /**
     * 商户编码【申请，开放平台生成商户唯一标识】【8位】
     */
    private String sysCode;

    /**
     * 应用编码【请求应用开发接口标识】
     */
    private String appCode;

    /**
     * 方法编码【对应某个业务功能】
     */
    private String methodCode;

    /**
     * 请求时间【yyyy-MM-dd HH:mm:ss】
     */
    private String reqTime;

    /**
     * 签名
     */
    private String sign;

    /**
     * 具体接⼝的请求数据
     */
    private T data;

    private void verifyReqSign(String publicKey) {
        String reqData = this.sysCode + this.appCode + this.methodCode + this.transId + JSON.toJSON(data);
        log.info("请求reqData：[{}]商户[{}]生成sign：[{}]", reqData, this.sysCode, this.sign);
        boolean success = DigitalSignatureUtil.verifySignature(reqData, this.sign, Base64.decodeBase64(publicKey));
        Assert.isTrue(success, () -> new ApiException(GlobalResCodeConstants.SIGN_ERROR));
    }

    public static void main(String[] args) {
        BaseOpenRequest<Demo> req = new BaseOpenRequest<>();
        req.setTransId(IdUtil.nextIdStr());
        req.setSysCode("DEMO");
        req.setAppCode("TEST");
        req.setReqTime(DateUtil.format(new Date(), DatePattern.NORM_DATETIME_PATTERN));
        req.setData(new Demo().setId(1L).setName("demo"));

        String reqData = req.getSysCode() + req.getAppCode() + req.getMethodCode() + req.getTransId() + JSON.toJSON(req.getData());
        log.info("商户：{}", reqData);
        String publicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAj3IbfcESYGBFC3YO7523jIzgUmX5gN2ozRTDcewAnQ1nJZJv/tBecKn2N98yz85UxSaOxwIB8LCxb6l0wq0gQ5HasNTVMRz1QHjc90qKnzfzMtJ2XQ0LwuvjzZpfKykoGXTEcwn3b5raKHCKNACequ2Uh9oESvPd8jJrWf2iUk5mJYAAz3wfDs1ymPXknJZTzlNipoVs3Zr5I6hzZs+HMNMq6gB2hEvNbV+0/IsWWXceuDkKu1lazgMoA+Hu1RXefCo+904ZC6RaRm8xdBOIX9PSqkDpQUIySf6s1H12pGjxZ82ARSOU1WoFIPgOXUJLs1+PGF9vNW79tev+ONtcRQIDAQAB";
        String privateKey = "MIIEvwIBADANBgkqhkiG9w0BAQEFAASCBKkwggSlAgEAAoIBAQCPcht9wRJgYEULdg7vnbeMjOBSZfmA3ajNFMNx7ACdDWclkm/+0F5wqfY33zLPzlTFJo7HAgHwsLFvqXTCrSBDkdqw1NUxHPVAeNz3SoqfN/My0nZdDQvC6+PNml8rKSgZdMRzCfdvmtoocIo0AJ6q7ZSH2gRK893yMmtZ/aJSTmYlgADPfB8OzXKY9eScllPOU2KmhWzdmvkjqHNmz4cw0yrqAHaES81tX7T8ixZZdx64OQq7WVrOAygD4e7VFd58Kj73ThkLpFpGbzF0E4hf09KqQOlBQjJJ/qzUfXakaPFnzYBFI5TVagUg+A5dQkuzX48YX281bv216/4421xFAgMBAAECggEARSA5+0n1bxg1baaA4Bvi/gpNd6fIqr8mv12IKfgO+6Af2s1Mx0DmoehlzNr0g4vs8ez41RI0mSas+yBwMfh4GdfJyXlnG3nW/jDAWzNCxiOSQSWHAKRuzIoch1IjFouI1Wud18ovwUqgYuiI0TKTQ8+lyinX8769cB+39+/wLzT7NJ0Pp25kVOdkAGjG7f97IP63qRwLI0Yq34Y4YEaSBOFsfZmL0Iunw7pS2cUXwHwxCcwDF66wl2xtW5cMkoG5N8dpKuhDgtVErCND7gg448siIT7E/sORkDBnz3H/PPG3XSllC1j0zKTHr5Ngnuw2t+CUbyZxImJBJPjN+kPbQQKBgQDRzYJjj1/PDwyrfAi40+fsYTCApdCT5kLk8ttSwkIhG2IxUalX4o6zGnMamaEvi98JGVJfed9ZvJmOnlt9jSkSEZEmpxis5k4VWvvdgyTMbiXDgf+Txe0a+aoES1OP1DpJ7FLRZxwU4AtpgLNeJdZo0UQDWqZZ2hFil/17jxko6QKBgQCvCBSqTAJ5DY0vQk3xxZ0paZZOJyF239OQDQ/KhZiuBRJqtmWJYrIHFecFg1JLAu5mVtTL4FZie81T4WE4DpkI13ideWQkMGtlrByUBAdeWnj7hZAWtq3ZC95gltTuIh//DRp27kHcatwRbx8wXjnDLBqSFQQoZibD7Z775vm+/QKBgQDETY2aqiPzERnBuiRPC7cNLUK8nGk0eVZN8g3UuX42i/CsRMQ2Pv1WB4F7ehOe8TiWwuKYAuhAhn8HOpRQPSwYg+dUSzSDUlntEVxoPrTHsqgS7ie8lIztmHzD19cv1FtVn5E97UQRJDCJXqp1hOHA4UzJ9p9/otJxddTvL01TQQKBgQCaYalAj+x6c159OaFgR+oYVd5SLqeQn1mOrEaqXe3OSAD3iMvEQv49y67KKQtTyFEYiSwGa2gmU4ZGnvtOI2oN52emliSi2uZPdmB6mZcaPPiK+UKfFh/+2j4ZudAz/nt0Tk1yazJCRSq4YegDlIikQmlpQgo5y+gTVqqtCwxrzQKBgQC9tkSpgywXLZ460+SxsM9J+Tj5z+1yPLegJjFf1awNAU85owa1yd6z6up6SzpdXu2kdRVA6artv8jfpGlvr1vkAxp+69+U5XwWHcyd40zYT+C0l4+suiRXi7nM0nGEemNtJMutcqTNKk4a5oVPYTXMzhGdkJqF+DbGT+y/ghUskg==";
        String sign = DigitalSignatureUtil.generationSignature(reqData, Base64.decodeBase64(privateKey));
        req.setSign(sign);
        req.verifyReqSign(publicKey);
    }

}
