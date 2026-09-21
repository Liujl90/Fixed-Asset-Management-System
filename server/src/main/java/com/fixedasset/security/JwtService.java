package com.fixedasset.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Set;

@Service
public class JwtService {

    private final JwtProperties properties;
    private final SecretKey key;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
        this.key = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(AuthenticatedUser user) {
        Instant now = Instant.now();
        // 将用户 ID、员工 ID、角色和权限码写入 Token，避免每次请求重复查询 RBAC 关系。
        // 代价是角色权限变更后必须等待 Token 过期或重新登录才能生效。
        return Jwts.builder()
                .subject(user.username())
                .claim("uid", user.userId())
                .claim("realName", user.realName())
                .claim("employeeId", user.employeeId())
                .claim("roles", user.roles())
                .claim("permissions", user.permissions())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(properties.expirationMinutes(), ChronoUnit.MINUTES)))
                .signWith(key)
                .compact();
    }

    public AuthenticatedUser parseToken(String token) {
        // 签名校验失败、Token 过期或格式非法时由 JJWT 抛出异常，过滤器统一清空上下文。
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        Long employeeId = claims.get("employeeId", Long.class);
        return new AuthenticatedUser(
                claims.get("uid", Long.class),
                claims.getSubject(),
                claims.get("realName", String.class),
                employeeId,
                Set.copyOf(claims.get("roles", List.class)),
                Set.copyOf(claims.get("permissions", List.class))
        );
    }
}
