package com.example.aigc.service;

import com.example.aigc.config.AuthProperties;
import com.example.aigc.entity.AppUser;
import com.example.aigc.enums.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Service
public class JwtTokenService {
    private final AuthProperties authProperties;
    private final SecretKey signingKey;

    public JwtTokenService(AuthProperties authProperties) {
        this.authProperties = authProperties;
        this.signingKey = Keys.hmacShaKeyFor(authProperties.getJwtSecret().getBytes(StandardCharsets.UTF_8));
    }

    public TokenPayload createToken(AppUser user) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(authProperties.getJwtExpireMinutes(), ChronoUnit.MINUTES);
        String token = Jwts.builder()
                .issuer(authProperties.getJwtIssuer())
                .subject(user.userId)
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .claim("username", user.username)
                .claim("displayName", user.displayName)
                .claim("role", user.role == null ? UserRole.STUDENT.name() : user.role.name())
                .claim("orgUnitId", user.orgUnitId)
                .claim("sessionVersion", user.sessionVersion)
                .signWith(signingKey)
                .compact();
        return new TokenPayload(token, expiresAt);
    }

    public AuthenticatedPrincipal parseToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .requireIssuer(authProperties.getJwtIssuer())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        String userId = claims.getSubject();
        String username = claims.get("username", String.class);
        String displayName = claims.get("displayName", String.class);
        String roleValue = claims.get("role", String.class);
        String orgUnitId = claims.get("orgUnitId", String.class);
        Long sessionVersion = claims.get("sessionVersion", Long.class);
        UserRole role = roleValue == null || roleValue.isBlank() ? UserRole.STUDENT : UserRole.valueOf(roleValue);
        return new AuthenticatedPrincipal(
                userId,
                username,
                displayName == null || displayName.isBlank() ? username : displayName,
                role,
                orgUnitId,
                sessionVersion == null ? 0 : sessionVersion
        );
    }

    public record TokenPayload(String accessToken, Instant expiresAt) {
    }

    public record AuthenticatedPrincipal(
            String userId,
            String username,
            String displayName,
            UserRole role,
            String orgUnitId,
            long sessionVersion
    ) {
    }
}
