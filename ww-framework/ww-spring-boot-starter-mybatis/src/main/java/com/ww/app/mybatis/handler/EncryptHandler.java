package com.ww.app.mybatis.handler;

import cn.hutool.core.util.StrUtil;
import com.ww.app.common.utils.AesUtil;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author ww
 * @create 2024-11-29- 10:33
 * @description: 加密处理器
 * 使用方法：
 * 1.实体字段上添加 @TableField(typeHandler = EncryptHandler.class)
 * 2.xml文件上 <result property="code" column="code" typeHandler="com.xx.xxx.EncryptHandler"/>
 * 3.设置 更新 SQL 的 SET 片段
 *  * @param condition 是否加入 set
 *  * @param column    字段
 *  * @param val       值
 *  * @param mapping   例: javaType=int,jdbcType=NUMERIC,typeHandler=xxx.xxx.MyTypeHandler
 *  * @return children
 *  Children set(boolean condition, R column, Object val, String mapping);
 **/
public class EncryptHandler extends BaseTypeHandler<String> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, String parameter, JdbcType jdbcType) throws SQLException {
        ps.setString(i, AesUtil.encrypt(parameter));
    }

    @Override
    public String getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String columnValue = rs.getString(columnName);
        return StrUtil.isNotEmpty(columnValue) ? AesUtil.decrypt(columnValue) : StrUtil.EMPTY;
    }

    @Override
    public String getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String columnValue = rs.getString(columnIndex);
        return StrUtil.isNotEmpty(columnValue) ? AesUtil.decrypt(columnValue) : StrUtil.EMPTY;
    }

    @Override
    public String getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String columnValue = cs.getString(columnIndex);
        return StrUtil.isNotEmpty(columnValue) ? AesUtil.decrypt(columnValue) : StrUtil.EMPTY;
    }
}
