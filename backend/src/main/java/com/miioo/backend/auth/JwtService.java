package com.miioo.backend.auth;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {
    private final SecretKey secretKey;
    private final long expireSeconds;

    public JwtService(@Value("${miioo.jwt.secret}") String secret,
                      @Value("${miioo.jwt.expire-seconds}") long expireSeconds) {
        String fixed = String.format("%-32s", secret).substring(0, 32);
        this.secretKey = Keys.hmacShaKeyFor(fixed.getBytes(StandardCharsets.UTF_8));
        this.expireSeconds = expireSeconds;
    }

    public String generateToken(long userId, String username) {
        return generateToken(userId, username, expireSeconds);
    }

    public String generateToken(long userId, String username, long customExpireSeconds) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + customExpireSeconds * 1000);
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("username", username)
                .issuedAt(now)
                .expiration(exp)
                .signWith(secretKey)
                .compact();
    }

    public Long parseUserId(String token) {
        Claims claims = Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload();
        return Long.parseLong(claims.getSubject());
    }
}
