package com.hduce.bank_api.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.hduce.bank_api.TestcontainersConfiguration;
import com.hduce.bank_api.entity.User;
import com.hduce.bank_api.model.LoginRequest;
import com.hduce.bank_api.model.LoginResponse;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.time.Instant;
import java.util.Date;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthIntegrationTest {

  @Autowired private TestRestTemplate restTemplate;
  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private TestAuthHelper authHelper;

  @Value("${jwt.secret}")
  private String jwtSecret;

  @Value("${jwt.expiration}")
  private Long jwtExpirationMs;

  @BeforeEach
  void setUp() {
    jdbcTemplate.execute("TRUNCATE TABLE users CASCADE");
  }

  @Nested
  class Login {

    private User createAndSaveUser(String password) {
      return authHelper.createAndSaveUser("user@gmail.com", password);
    }

    @Test
    void shouldLoginSuccessfullyWithValidCredentials() {
      // Given
      var password = "SecurePassword123!";
      var user = createAndSaveUser(password);

      var loginRequest = new LoginRequest().email(user.getEmail()).password(password);

      // When
      var loginResponse =
          restTemplate.postForEntity("/v1/auth/login", loginRequest, LoginResponse.class);

      // Then
      assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
      assertThat(loginResponse.getBody()).isNotNull();
      assertThat(loginResponse.getBody().getUserId()).isEqualTo(user.getId());
      var token = loginResponse.getBody().getToken();
      assertThat(token).isNotNull().isNotEmpty();

      // Parse and verify JWT token
      var signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
      var claims = Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token);

      // Verify token claims
      assertThat(claims.getPayload().getSubject()).isEqualTo(user.getId());
      assertThat(claims.getPayload().getExpiration()).isAfter(Date.from(Instant.now()));
      assertThat(claims.getPayload().getExpiration())
          .isBeforeOrEqualTo(Date.from(Instant.now().plusMillis(jwtExpirationMs + 1000)));
    }

    @Test
    void shouldFailToLoginWithWrongPassword() {
      // Given
      var password = "SecurePassword123!";
      var user = createAndSaveUser(password);

      var loginRequest = new LoginRequest().email(user.getEmail()).password("WrongPassword123!");

      // When
      var loginResponse = restTemplate.postForEntity("/v1/auth/login", loginRequest, String.class);

      // Then
      assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void shouldFailToLoginWithNonExistentEmail() {
      // Given
      var loginRequest =
          new LoginRequest().email("nonexistent@example.com").password("SecurePassword123!");

      // When
      var loginResponse = restTemplate.postForEntity("/v1/auth/login", loginRequest, String.class);

      // Then
      assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
  }
}
