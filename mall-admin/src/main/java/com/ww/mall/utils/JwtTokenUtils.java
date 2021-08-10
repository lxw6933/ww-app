package com.ww.mall.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * @description: jwt
 * @author: ww
 * @create: 2021/6/26 上午7:14
 **/
@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtTokenUtils {

    public long expiration;
    private String header;
    private String secret;

    /**
     * 从token中获取登录用户的username
     *
     * @param token token
     * @return username
     */
    public String getUsernameFromToken(String token) throws Exception {
        Claims claims = getAllClaimsFromToken(token);
        return claims.getSubject();
    }

    /**
     * 从token中获取token的过期时间
     *
     * @param token token
     * @return Date
     */
    private Date getExpirationDateFromToken(String token) {
        Date expiration;
        try {
            Claims claims = getAllClaimsFromToken(token);
            expiration = claims.getExpiration();
        } catch (Exception e) {
            expiration = null;
        }
        return expiration;
    }

    /**
     * 判断当前token是否过期
     *
     * @param token token
     * @return boolean
     */
    public Boolean isTokenExpired(String token) {
        Date expiration = getExpirationDateFromToken(token);
        return expiration.before(new Date());
    }

    /**
     * 根据一个user生成一个对应的token
     *
     * @param userDetails userDetails
     * @return newToken
     */
    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        claims.put("sub", userDetails.getUsername());
        return generateNewToken(claims);
    }

    /**
     * 通过claims生成新的token
     *
     * @param claims claims
     * @return newToken
     */
    private String generateNewToken(Map<String, Object> claims) {
        return Jwts.builder().setClaims(claims)
                .setExpiration(new Date(System.currentTimeMillis() + expiration * 1000))
                .signWith(SignatureAlgorithm.HS512, secret).compact();
    }

    /**
     * 验证token的有效性
     *
     * @param token       token
     * @param userDetails userDetails
     * @return boolean
     */
    public Boolean validateToken(String token, UserDetails userDetails) throws Exception {
        String username = getUsernameFromToken(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

    /**
     * 通过token获取相应的数据声明（通过秘钥对token进行解析）
     *
     * @param token token
     * @return 获取token内部中的信息
     */
    private Claims getAllClaimsFromToken(String token) {
        return Jwts.parser().setSigningKey(secret).parseClaimsJws(token).getBody();
    }

    /**
     * 刷新token
     *
     * @param token old token
     * @return 刷新后的token
     */
    public String refreshToken(String token) {
        Claims claims = getAllClaimsFromToken(token);
        claims.put("created", new Date());
        return generateNewToken(claims);
    }

}
