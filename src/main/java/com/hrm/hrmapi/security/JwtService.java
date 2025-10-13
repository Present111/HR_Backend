// security/JwtService.java
package com.hrm.hrmapi.security;
import io.jsonwebtoken.*; import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value; import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets; import java.security.Key; import java.time.Instant; import java.util.Date;

@Component
public class JwtService {
    @Value("${hrm.jwt.secret}") private String secret;
    @Value("${hrm.jwt.ttlMinutes}") private long ttl;

    private Key key() { return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)); }

    public String issue(String userId, String role) {
        Instant now = Instant.now();
        return Jwts.builder()
                .setSubject(userId)
                .claim("role", role)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plusSeconds(ttl * 60)))
                .signWith(key(), SignatureAlgorithm.HS256)
                .compact();
    }

    public Jws<Claims> parse(String token) {
        return Jwts.parserBuilder().setSigningKey(key()).build().parseClaimsJws(token);
    }
}
