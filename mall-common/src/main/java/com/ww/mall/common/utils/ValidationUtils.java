package com.ww.mall.common.utils;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.ww.mall.common.exception.ApiException;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * @author ww
 * @create 2024-11-22- 16:51
 * @description: 通用校验工具类
 */
public class ValidationUtils {

    private static final String MOBILE_REG = "^1[3456789]\\d{9}$";
//    private static final String MOBILE_REG = "^(?:(?:\\+|00)86)?1(?:(?:3[\\d])|(?:4[0,1,4-9])|(?:5[0-3,5-9])|(?:6[2,5-7])|(?:7[0-8])|(?:8[\\d])|(?:9[0-3,5-9]))\\d{8}$";

    private static final Pattern PATTERN_MOBILE = Pattern.compile(MOBILE_REG);

    public static boolean isMobile(String mobile) {
        return StringUtils.hasText(mobile) && PATTERN_MOBILE.matcher(mobile).matches();
    }

    // 单位MB
    private static final int EXCEL_DEFAULT_MAX_SIZE = 20;

    private static final List<String> EXCEL_FLAGS = Arrays.asList("xls", "xlsx");

    public static boolean isExcel(MultipartFile file) {
        return isExcel(file, EXCEL_DEFAULT_MAX_SIZE);
    }

    public static boolean isExcel(MultipartFile file, int fileMaxSize) {
        String suffix = FileUtil.getSuffix(file.getOriginalFilename());
        String fileType = org.apache.commons.lang3.StringUtils.isBlank(suffix) ? StrUtil.EMPTY : suffix.toLowerCase(Locale.ROOT);
        if (!EXCEL_FLAGS.contains(fileType)) {
            throw new ApiException("仅支持" + EXCEL_FLAGS + "格式的文件");
        }
        long fileSize = file.getSize();
        if (fileSize > fileMaxSize) {
            throw new ApiException("上传文件超过" + fileMaxSize + "M");
        }
        return true;
    }

    private static final String URL_REG = "^(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]";

    private static final Pattern PATTERN_URL = Pattern.compile(URL_REG);

    public static boolean isURL(String url) {
        return StringUtils.hasText(url) && PATTERN_URL.matcher(url).matches();
    }


}
