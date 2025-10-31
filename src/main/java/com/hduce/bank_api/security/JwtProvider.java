package com.hduce.bank_api.security;

import com.hduce.bank_api.entity.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtProvider {
  private final Long jwtExpirationMs;
  private final Key signingKey;

  public JwtProvider(
      @Value("${jwt.secret}") String jwtSecret, @Value("${jwt.expiration}") Long jwtExpirationMs) {
    this.jwtExpirationMs = jwtExpirationMs;
    this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
  }

  public String generateToken(User user) {
    return Jwts.builder()
        .signWith(signingKey)
        .subject(user.getId())
        .expiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
        .compact();
  }

  public String extractUserId(String token) {
    return Jwts.parser()
        .verifyWith((SecretKey) signingKey)
        .build()
        .parseSignedClaims(token)
        .getPayload()
        .getSubject();
  }

  public boolean isTokenValid(String token) {
    try {
      Jwts.parser().verifyWith((SecretKey) signingKey).build().parseSignedClaims(token);
      return true;
    } catch (Exception e) {
      return false;
    }
  }
}
