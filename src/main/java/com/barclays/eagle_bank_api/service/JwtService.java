package com.barclays.eagle_bank_api.service;

import com.barclays.eagle_bank_api.entity.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.util.Date;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
  private final Long jwtExpirationMs;
  private final Key signingKey;

  public JwtService(
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
}
