package org.example.javamusicapp.config.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtUtil {
    @Value("${jwt.secret}")
    private String superSecretKey;
    @Value("${jwt.expiration.ms}")
    private long jwtExpirationInMs;

    public String generateToken(UserDetails userDetails) {
        Map<String, Object> map = new HashMap<>();
        map.put("userName", userDetails.getUsername());
        map.put("authorities", userDetails.getAuthorities());
        map.put("roles", userDetails.getAuthorities());
        map.put("isAccountNonExpired", true);
        map.put("isAccountNonLocked", true);
        map.put("isCredentialsNonExpired", true);
        map.put("isEnabled", true);

        return Jwts.builder()
                .setClaims(map)
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpirationInMs))
                .signWith(getSigningKey())
                .compact();
    }

    public String extractUsername(String token) {
        return extractClaims(token, Claims::getSubject);
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            final String benutzerName = extractUsername(token);
            return (benutzerName.equals(userDetails.getUsername()) && !isTokenExpired(token));
        }
        catch (Exception e) {
            return false;
        }
    }

    private boolean isTokenExpired(String token) {
        final Date expiration = extractExpiration(token);
        return expiration.before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaims(token, Claims::getExpiration);
    }

    // Helper functions
    private <T> T extractClaims(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    // Erzeugt den geheimen Key aus deinem Base64-String
    private Key getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(superSecretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
