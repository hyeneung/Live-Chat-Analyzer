package org.example.chatserver.jwt;

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

    @Value("${jwt.secret}")
    private String secret;
    private SecretKey secretKey;

    /**
     * Initializes the secret key after the bean has been constructed.
     */
    @PostConstruct
    protected void init() {
        secretKey = Keys.hmacShaKeyFor(Base64.getDecoder().decode(secret));
    }

    /**
     * Validates the given JWT token.
     * @param token The token to validate.
     * @return True if the token is valid, otherwise false.
     */
    public boolean validateToken(String token) {
        try {
            getClaimsFromToken(token);
            return true;
        } catch (io.jsonwebtoken.JwtException e) {
            log.error("Invalid JWT: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Extracts the username (which is the user ID in this context) from the token.
     * This method is kept for compatibility with the AuthChannelInterceptor.
     * @param token The token to parse.
     * @return The username (user ID) as a String.
     */
    // This method is kept for compatibility with the AuthChannelInterceptor
    public String getUserIdFromToken(String token) {
        return getClaimsFromToken(token).getSubject();
    }

    /**
     * Parses the token and returns the claims (payload).
     * @param token The token to parse.
     * @return The claims from the token.
     * @throws JwtException if the token is invalid or expired.
     */
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