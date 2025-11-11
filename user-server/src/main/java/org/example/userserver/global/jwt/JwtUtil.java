package org.example.userserver.global.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;

import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;

@Slf4j
@Component
public class JwtUtil {

    private final String secret;
    private final long accessTokenExpiration;
    @Getter
    private final long refreshTokenExpiration;
    private SecretKey secretKey;

    public JwtUtil(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-expiration}") long accessTokenExpiration,
            @Value("${jwt.refresh-token-expiration}") long refreshTokenExpiration
    ) {
        this.secret = secret;
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
    }

    @PostConstruct
    protected void init() {
        secretKey = Keys.hmacShaKeyFor(Base64.getUrlDecoder().decode(secret));
    }

    public String generateAccessToken(Long userId, String role) {
        return generateToken(userId, role, accessTokenExpiration);
    }

    public String generateRefreshToken(Long userId) {
        return generateToken(userId, null, refreshTokenExpiration);
    }

    private String generateToken(Long userId, String role, long expiration) {
        Date now = new Date();
        Date validity = new Date(now.getTime() + expiration);

        io.jsonwebtoken.JwtBuilder builder = Jwts.builder()
                .subject(String.valueOf(userId))
                .issuedAt(now)
                .expiration(validity)
                .signWith(secretKey);

        if (role != null) {
            builder.claim("role", role);
        }

        return builder.compact();
    }

    public boolean validateToken(String token) {
        try {
            getClaimsFromToken(token);
            return true;
        } catch (io.jsonwebtoken.JwtException e) {
            log.error("Invalid JWT: {}", e.getMessage());
            return false;
        }
    }

    public Long getUserIdFromToken(String token) {
        String userIdAsString = getClaimsFromToken(token).getSubject();
        return Long.parseLong(userIdAsString);
    }

    public String getRoleFromToken(String token) {
        return getClaimsFromToken(token).get("role", String.class);
    }


    public Claims getClaimsFromToken(String token) throws JwtException {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            throw new io.jsonwebtoken.JwtException("Expired JWT token", e);
        } catch (io.jsonwebtoken.UnsupportedJwtException e) {
            throw new io.jsonwebtoken.JwtException("Unsupported JWT token", e);
        } catch (io.jsonwebtoken.MalformedJwtException e) {
            throw new io.jsonwebtoken.JwtException("Invalid JWT token", e);
        } catch (io.jsonwebtoken.security.SignatureException e) {
            throw new io.jsonwebtoken.JwtException("Invalid JWT signature", e);
        } catch (IllegalArgumentException e) {
            throw new io.jsonwebtoken.JwtException("JWT claims string is empty", e);
        }
    }
}
