package com.example.NextPlan.auth;


import com.example.NextPlan.common.CustomException;
import com.example.NextPlan.common.ErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;


@Component
public class JwtProvider {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access-token-expiration-ms}")
    private long accessTokenExpirationMs;

    @Value("${jwt.refresh-token-expiration-ms}")
    private long refreshTokenExpirationMs;

    private SecretKey key;

    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String createAccessToken(UUID userId) {
        long now = System.currentTimeMillis();

        return Jwts.builder()
                .subject(userId.toString())
                .claim("type", "access")
                .issuedAt(new Date(now))
                .expiration(new Date(now + accessTokenExpirationMs))
                .signWith(key)
                .compact();
    }

    public String createRefreshToken(UUID userId) {
        long now = System.currentTimeMillis();

        return Jwts.builder()
                .subject(userId.toString())
                .claim("type", "refresh")
                .issuedAt(new Date(now))
                .expiration(new Date(now + refreshTokenExpirationMs))
                .signWith(key)
                .compact();
    }

    public UUID getUserId(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            return UUID.fromString(claims.getSubject());
        } catch (ExpiredJwtException e) {
            throw new CustomException(ErrorCode.REFRESH_TOKEN_EXPIRED);
        } catch (IllegalArgumentException e) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        } catch (JwtException e) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
    }

    public void validateRefreshToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String type = claims.get("type", String.class);

            if (!"refresh".equals(type)) {
                throw new CustomException(ErrorCode.FORBIDDEN);
            }
        } catch (ExpiredJwtException e) {
            throw new CustomException(ErrorCode.REFRESH_TOKEN_EXPIRED);
        } catch (IllegalArgumentException e) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        } catch (JwtException e) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
    }

    public long getAccessTokenExpirationSec() {
        return accessTokenExpirationMs / 1000;
    }

    public long getRefreshTokenExpirationSec() {
        return refreshTokenExpirationMs / 1000;
    }
}