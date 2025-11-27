package org.example.javamusicapp.config.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.example.javamusicapp.model.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * 🛠️ **Was geht hier ab?**
 * Das ist unser Schweizer Taschenmesser für alles, was mit JSON Web Tokens (JWTs) zu tun hat.
 * Diese Utility-Klasse ist der Go-To-Guy für die `JwtAuthenticationFilter` und den `AuthController`.
 *
 * Ihre Skills:
 * - **Token generieren:** Erstellt einen neuen, signierten JWT für einen User, z.B. direkt nach dem Login.
 *   In den Token packt sie wichtige Infos wie E-Mail (als Subject), User-Rollen und ein Ablaufdatum.
 * - **Token validieren:** Checkt, ob ein Token, der mit einem Request reinkommt, echt ist (über die Signatur)
 *   und ob er nicht schon abgelaufen ist.
 * - **Infos auslesen:** Kann alle Claims (die Infos im Token) auslesen, z.B. das Subject (die E-Mail des Users),
 *   um den User in der Datenbank zu finden.
 *
 * Absolut central für die ganze Auth-Logik.
 */
@Component
public class JwtUtil {
    @Value("${jwt.secret}")
    private String superSecretKey;
    @Value("${jwt.expiration.ms}")
    private long jwtExpirationInMs;

    public String generateToken(UserDetails userDetails) {
        if (!(userDetails instanceof User)) {
            throw new IllegalArgumentException("UserDetails must be an instance of User");
        }
        User user = (User) userDetails;

        Map<String, Object> map = new HashMap<>();
        map.put("userName", user.getUsername()); // Keep username in claims for other purposes
        map.put("authorities", user.getAuthorities());
        map.put("roles", user.getAuthorities());
        map.put("isAccountNonExpired", true);
        map.put("isAccountNonLocked", true);
        map.put("isCredentialsNonExpired", true);
        map.put("isEnabled", true);

        return Jwts.builder()
                .setClaims(map)
                .setSubject(user.getEmail()) // Use email as the subject
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpirationInMs))
                .signWith(getSigningKey())
                .compact();
    }

    public String extractSubject(String token) {
        return extractClaims(token, Claims::getSubject);
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        if (!(userDetails instanceof User)) {
            return false;
        }
        try {
            final String subject = extractSubject(token);
            // Subject is now email
            return (subject.equals(((User) userDetails).getEmail()) && !isTokenExpired(token));
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
