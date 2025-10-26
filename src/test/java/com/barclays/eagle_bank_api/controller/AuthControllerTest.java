package com.barclays.eagle_bank_api.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.barclays.eagle_bank_api.TestcontainersConfiguration;
import com.barclays.eagle_bank_api.model.CreateUserRequest;
import com.barclays.eagle_bank_api.model.CreateUserRequestAddress;
import com.barclays.eagle_bank_api.model.LoginRequest;
import com.barclays.eagle_bank_api.model.LoginResponse;
import com.barclays.eagle_bank_api.model.UserResponse;
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
class AuthControllerTest {

  @Autowired private TestRestTemplate restTemplate;
  @Autowired private JdbcTemplate jdbcTemplate;

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

    @Test
    void shouldLoginSuccessfullyWithValidCredentials() {
      // Given
      var createRequest = buildCreateUserRequest();
      var createResponse =
          restTemplate.postForEntity("/v1/users", createRequest, UserResponse.class);
      assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
      var createResponseBody = createResponse.getBody();
      assertThat(createResponseBody).isNotNull();

      var loginRequest =
          new LoginRequest().email(createRequest.getEmail()).password(createRequest.getPassword());

      // When
      var loginResponse =
          restTemplate.postForEntity("/v1/auth/login", loginRequest, LoginResponse.class);

      // Then - Verify response
      assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
      assertThat(loginResponse.getBody()).isNotNull();
      var token = loginResponse.getBody().getToken();
      assertThat(token).isNotNull().isNotEmpty();

      // Parse and verify JWT token
      var signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
      var claims = Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token);

      // Verify token claims
      assertThat(claims.getPayload().getSubject()).isEqualTo(createResponseBody.getEmail());
      assertThat(claims.getPayload().getExpiration()).isAfter(Date.from(Instant.now()));
      assertThat(claims.getPayload().getExpiration())
          .isBeforeOrEqualTo(Date.from(Instant.now().plusMillis(jwtExpirationMs + 1000)));
    }

    @Test
    void shouldFailToLoginWithWrongPassword() {
      // Given - Create a user first
      var createRequest = buildCreateUserRequest();
      var createResponse =
          restTemplate.postForEntity("/v1/users", createRequest, UserResponse.class);
      assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

      var loginRequest =
          new LoginRequest().email(createRequest.getEmail()).password("WrongPassword123!");

      // When
      var loginResponse = restTemplate.postForEntity("/v1/auth/login", loginRequest, String.class);

      // Then
      assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void shouldFailToLoginWithNonExistentEmail() {
      // Given
      var loginRequest =
          new LoginRequest().email("nonexistent@example.com").password("SecurePassword123!");

      // When
      var loginResponse = restTemplate.postForEntity("/v1/auth/login", loginRequest, String.class);

      // Then
      assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
  }

  private CreateUserRequest buildCreateUserRequest() {
    return new CreateUserRequest()
        .name("John Doe")
        .email("john.doe@example.com")
        .password("SecurePassword123!")
        .phoneNumber("+441234567890")
        .address(buildAddress());
  }

  private CreateUserRequestAddress buildAddress() {
    return new CreateUserRequestAddress()
        .line1("123 Main Street")
        .line2("Apt 4B")
        .town("London")
        .county("Greater London")
        .postcode("SW1A 1AA");
  }
}
