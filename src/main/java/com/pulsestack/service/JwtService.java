package com.pulsestack.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class JwtService {
//    replace the secret key with something better in the future but for development leave it here
    private static final String SECRET_KEY = "TEjojQAks4vJpfK2FoATMCZ3ssbXVAkRRJVNaSAa9aECebWGvBWY5csOHh2EiONh9eA";

    public String generateToken(String username, String role) {
        long nowMillis = System.currentTimeMillis();
        long expMillis = nowMillis + (1000 * 60 * 60); // token validity upto 1 hour

        return Jwts.builder()
                .setSubject(username)
                .claim("role", role)
                .setIssuedAt(new Date(nowMillis))
                .setExpiration(new Date(expMillis))
                .signWith(SignatureAlgorithm.HS256, SECRET_KEY)
                .compact();
    }

    public String validateToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(SECRET_KEY)
                .build()
                .parseClaimsJws(token)
                .getBody();

        if (claims.getExpiration().before(new Date())) {
            throw new RuntimeException("token is expired");
        }

        return claims.getSubject();
    }

    public Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(SECRET_KEY)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
