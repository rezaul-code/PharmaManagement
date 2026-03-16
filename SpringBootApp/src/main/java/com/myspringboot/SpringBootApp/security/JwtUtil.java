package com.myspringboot.SpringBootApp.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.function.Function;

@Component
public class JwtUtil {

    @Value("${jwt.secret:pharma_saas_secret_key_2026}")
    private String jwtSecret;

    @Value("${jwt.expiration:86400000}")
    private long jwtExpirationMs;

    private SecretKey getSigningKey() {
        // Fallback to ensuring the key is long enough (HMAC-SHA256 requires 256 bits / 32 bytes)
        String finalSecret = jwtSecret;
        if (finalSecret.length() < 32) {
            finalSecret = finalSecret + "12345678901234567890123456789012";
        }
        return Keys.hmacShaKeyFor(finalSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    public String generateToken(CustomUserDetails userDetails) {
        return Jwts.builder()
                .setSubject(userDetails.getUsername())
                .claim("userId", userDetails.getUser().getId())
                .claim("role", userDetails.getUser().getRole().name())
                .claim("tenantId", userDetails.getUser().getPharmacy() != null ? userDetails.getUser().getPharmacy().getTenantId() : null)
                .claim("pharmacyId", userDetails.getUser().getPharmacy() != null ? userDetails.getUser().getPharmacy().getId() : null)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder().setSigningKey(getSigningKey()).build().parseClaimsJws(token).getBody();
    }

    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public Boolean validateToken(String token, CustomUserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }
}
