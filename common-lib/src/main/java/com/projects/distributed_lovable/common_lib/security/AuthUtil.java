package com.projects.distributed_lovable.common_lib.security;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.projects.distributed_lovable.common_lib.dto.UserDto;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Component
public class AuthUtil {

    private static final int MIN_HS256_KEY_BYTES = 32;

    @Value("${jwt.secret-key}")
    private String jwtSecretKey;

    private SecretKey getSecretKey() {
        byte[] keyBytes = jwtSecretKey.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < MIN_HS256_KEY_BYTES) {
            throw new IllegalStateException("jwt.secret-key must be at least 32 characters for HS256");
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateAccessToken(JwtUserPrincipal user) {
        // Dummy implementation for illustration purposes
        return Jwts.builder()
                .subject(user.username())
                .claim("userId", user.userId().toString())
                .claim("name", user.name())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 1000 * 60 * 100)) // 100 minutes expiration
                .signWith(getSecretKey(), Jwts.SIG.HS256)
                .compact();
    }

    public JwtUserPrincipal verifyAccessToken(String token) {
        var claims = Jwts.parser()
                .verifyWith(getSecretKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        Long userId = Long.parseLong(claims.get("userId", String.class));
        String name = claims.get("name", String.class);
        String username = claims.getSubject();

        return new JwtUserPrincipal(userId, name, username, null, new ArrayList<>());
    }

    public Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof JwtUserPrincipal userPrinciple)) {
            throw new AuthenticationCredentialsNotFoundException("User not authenticated, no JWT found ");
        }
        return userPrinciple.userId();
    }
}
