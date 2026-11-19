package com.example.aiclient.security;

import com.example.aiclient.entity.UserEntity;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

/**
 * HS256 JWT：sub=userId，claims 含 username/role，有效期由 app.jwt.expire-minutes 控制。
 */
@Service
public class JwtService {

    private final SecretKey key;
    private final Duration expire;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.expire-minutes:120}") long expireMinutes) {
        byte[] keyBytes = secret == null ? new byte[0] : secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException("app.jwt.secret 至少需要 32 字节（请检查 JWT_SECRET 或 application-dev.yml）");
        }
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.expire = Duration.ofMinutes(expireMinutes);
    }

    public String generate(UserEntity user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claim("username", user.getUsername())
                .claim("role", user.getRole())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expire)))
                .signWith(key)
                .compact();
    }

    /** 解析并校验签名/过期时间，失败抛 JwtException 子类。 */
    public LoginUser parse(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return new LoginUser(
                Long.valueOf(claims.getSubject()),
                claims.get("username", String.class),
                claims.get("role", String.class));
    }
}