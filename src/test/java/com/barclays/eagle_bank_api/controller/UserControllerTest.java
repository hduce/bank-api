package com.barclays.eagle_bank_api.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.barclays.eagle_bank_api.TestcontainersConfiguration;
import com.barclays.eagle_bank_api.entity.User;
import com.barclays.eagle_bank_api.model.CreateUserRequest;
import com.barclays.eagle_bank_api.model.CreateUserRequestAddress;
import com.barclays.eagle_bank_api.model.UserResponse;
import com.barclays.eagle_bank_api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UserControllerTest {

  @Autowired private TestRestTemplate restTemplate;
  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private UserRepository userRepository;
  @Autowired private PasswordEncoder passwordEncoder;

  @BeforeEach
  void setUp() {
    jdbcTemplate.execute("TRUNCATE TABLE users CASCADE");
  }

  @Nested
  class CreateUser {

    @Test
    void shouldCreateUserSuccessfully() {
      // Given
      var createRequest = buildCreateUserRequest();

      // When
      var response = restTemplate.postForEntity("/v1/users", createRequest, UserResponse.class);

      // Then - Verify HTTP response
      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
      assertThat(response.getBody()).isNotNull();

      var userResponse = response.getBody();
      assertThat(userResponse.getId()).isNotNull().startsWith("usr-");
      assertThat(userResponse.getName()).isEqualTo("John Doe");
      assertThat(userResponse.getEmail()).isEqualTo("john.doe@example.com");
      assertThat(userResponse.getPhoneNumber()).isEqualTo("+441234567890");
      assertThat(userResponse.getCreatedTimestamp()).isNotNull();
      assertThat(userResponse.getUpdatedTimestamp()).isNotNull();

      // Verify address
      var addressResponse = userResponse.getAddress();
      assertThat(addressResponse).isNotNull();
      assertThat(addressResponse.getLine1()).isEqualTo("123 Main Street");
      assertThat(addressResponse.getLine2()).isEqualTo("Apt 4B");
      assertThat(addressResponse.getTown()).isEqualTo("London");
      assertThat(addressResponse.getCounty()).isEqualTo("Greater London");
      assertThat(addressResponse.getPostcode()).isEqualTo("SW1A 1AA");

      // Then - Verify database state
      var userId = userResponse.getId();
      var userCount =
          jdbcTemplate.queryForObject(
              "SELECT COUNT(*) FROM users WHERE id = ?", Integer.class, userId);
      assertThat(userCount).isEqualTo(1);

      User user = userRepository.findById(userId).orElseThrow();
      assertThat(user.getName()).isEqualTo("John Doe");
      assertThat(user.getEmail()).isEqualTo("john.doe@example.com");
      assertThat(user.getPhoneNumber()).isEqualTo("+441234567890");
      var address = user.getAddress();
      assertThat(address.line1()).isEqualTo("123 Main Street");
      assertThat(address.line2()).isEqualTo("Apt 4B");
      assertThat(address.town()).isEqualTo("London");
      assertThat(address.county()).isEqualTo("Greater London");
      assertThat(address.postcode()).isEqualTo("SW1A 1AA");
    }

    @Test
    void shouldEncryptUserPassword() {
      // Given
      var createRequest = buildCreateUserRequest();

      // When
      var response = restTemplate.postForEntity("/v1/users", createRequest, UserResponse.class);

      // Then
      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
      assertThat(response.getBody()).isNotNull();
      var userId = response.getBody().getId();

      // Verify password is encrypted and can be verified
      var dbPassword =
          jdbcTemplate.queryForObject(
              "SELECT password FROM users WHERE id = ?", String.class, userId);
      assertThat(passwordEncoder.matches(createRequest.getPassword(), dbPassword)).isTrue();
    }

    @Test
    void shouldFailToCreateUserWithDuplicateEmail() {
      // Given - Create first user
      var firstRequest = buildCreateUserRequest();
      var firstResponse = restTemplate.postForEntity("/v1/users", firstRequest, UserResponse.class);
      assertThat(firstResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

      // When - Try to create second user with same email
      var secondRequest =
          new CreateUserRequest()
              .name("Jane Smith")
              .email("john.doe@example.com") // Same email
              .password("DifferentPassword123!")
              .phoneNumber("+447890123456")
              .address(buildAddress());

      var secondResponse = restTemplate.postForEntity("/v1/users", secondRequest, String.class);

      // Then - Should fail with appropriate error
      assertThat(secondResponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

      // Verify only one user exists in database
      var userCount =
          jdbcTemplate.queryForObject(
              "SELECT COUNT(*) FROM users WHERE email = ?", Integer.class, "john.doe@example.com");
      assertThat(userCount).isEqualTo(1);
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
