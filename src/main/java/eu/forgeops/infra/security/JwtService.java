package eu.forgeops.infra.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class JwtService {

    private static final long ACCESS_TOKEN_TTL_MS  = 15 * 60 * 1000L;      // 15 min
    private static final long REFRESH_TOKEN_TTL_MS = 7 * 24 * 60 * 60 * 1000L; // 7 days

    private final SecretKey signingKey;

    public JwtService(@Value("${forgeops.jwt.secret}") String secret) {
        byte[] keyBytes = Decoders.BASE64.decode(
            java.util.Base64.getEncoder().encodeToString(secret.getBytes())
        );
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateAccessToken(UUID userId, String username, java.util.Set<String> roles) {
        return Jwts.builder()
            .subject(userId.toString())
            .claim("username", username)
            .claim("roles", roles)
            .claim("type", "access")
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + ACCESS_TOKEN_TTL_MS))
            .signWith(signingKey)
            .compact();
    }

    public String generateRefreshToken(UUID userId) {
        return Jwts.builder()
            .subject(userId.toString())
            .claim("type", "refresh")
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + REFRESH_TOKEN_TTL_MS))
            .signWith(signingKey)
            .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
            .verifyWith(signingKey)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    public boolean isAccessToken(Claims claims) {
        return "access".equals(claims.get("type"));
    }

    public boolean isRefreshToken(Claims claims) {
        return "refresh".equals(claims.get("type"));
    }

    public boolean isValid(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException e) {
            return false;
        }
    }
}
