package com.hduce.bank_api.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.hduce.bank_api.TestcontainersConfiguration;
import com.hduce.bank_api.entity.User;
import com.hduce.bank_api.model.*;
import com.hduce.bank_api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UserIntegrationTest {

  @Autowired private TestRestTemplate restTemplate;
  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private UserRepository userRepository;
  @Autowired private PasswordEncoder passwordEncoder;
  @Autowired private TestAuthHelper authHelper;

  @BeforeEach
  void setUp() {
    jdbcTemplate.execute("TRUNCATE TABLE users CASCADE");
  }

  private User createAndSaveUser(String email) {
    return authHelper.createAndSaveUser(email);
  }

  private HttpHeaders createAuthHeaders(User user) {
    return authHelper.createAuthHeaders(user);
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

  @Nested
  class FetchUser {
    @Test
    void shouldFetchUserSuccessfully() {
      // Given
      final var user = createAndSaveUser("foo@gmail.com");

      // When
      var response =
          restTemplate.exchange(
              "/v1/users/" + user.getId(),
              HttpMethod.GET,
              new HttpEntity<>(createAuthHeaders(user)),
              UserResponse.class);

      // Then
      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      var userResponse = response.getBody();
      assertThat(userResponse).isNotNull();
      assertThat(userResponse.getId()).isEqualTo(user.getId());
      assertThat(userResponse.getName()).isEqualTo("Test User");
      assertThat(userResponse.getEmail()).isEqualTo("foo@gmail.com");
      assertThat(userResponse.getPhoneNumber()).isEqualTo("07988220214");

      var addressResponse = userResponse.getAddress();
      assertThat(addressResponse).isNotNull();
      assertThat(addressResponse.getLine1()).isEqualTo("line1");
      assertThat(addressResponse.getLine2()).isEqualTo("line2");
      assertThat(addressResponse.getLine3()).isEqualTo("line3");
      assertThat(addressResponse.getTown()).isEqualTo("town");
      assertThat(addressResponse.getCounty()).isEqualTo("county");
      assertThat(addressResponse.getPostcode()).isEqualTo("postcode");
    }

    @Test
    void shouldForbidAccessToOtherUsersData() {
      // Given
      final var user1 = createAndSaveUser("user1@gmail.com");
      final var user2 = createAndSaveUser("user2@gmail.com");

      // When
      var response =
          restTemplate.exchange(
              "/v1/users/" + user2.getId(),
              HttpMethod.GET,
              new HttpEntity<>(createAuthHeaders(user1)),
              String.class);

      // Then
      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void shouldForbidAccessWithoutAuthentication() {
      // Given
      final var user = createAndSaveUser("user@gmail.com");

      // When
      var response =
          restTemplate.exchange(
              "/v1/users/" + user.getId(), HttpMethod.GET, HttpEntity.EMPTY, String.class);

      // Then
      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
  }

  @Nested
  class UpdateUser {

    @Test
    void shouldUpdateOwnUserDetailsSuccessfully() {
      // Given
      final var user = createAndSaveUser("original@example.com");
      final var updateRequest =
          new UpdateUserRequest()
              .name("Updated Name")
              .email("updated@example.com")
              .phoneNumber("+449876543210")
              .address(
                  new CreateUserRequestAddress()
                      .line1("New Line 1")
                      .line2("New Line 2")
                      .line3("New Line 3")
                      .town("New Town")
                      .county("New County")
                      .postcode("NW1 1AA"));

      // When
      final var response =
          restTemplate.exchange(
              "/v1/users/" + user.getId(),
              HttpMethod.PATCH,
              new HttpEntity<>(updateRequest, createAuthHeaders(user)),
              UserResponse.class);

      // Then
      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      assertThat(response.getBody()).isNotNull();

      final var userResponse = response.getBody();
      assertThat(userResponse.getId()).isEqualTo(user.getId());
      assertThat(userResponse.getName()).isEqualTo("Updated Name");
      assertThat(userResponse.getEmail()).isEqualTo("updated@example.com");
      assertThat(userResponse.getPhoneNumber()).isEqualTo("+449876543210");

      final var addressResponse = userResponse.getAddress();
      assertThat(addressResponse.getLine1()).isEqualTo("New Line 1");
      assertThat(addressResponse.getLine2()).isEqualTo("New Line 2");
      assertThat(addressResponse.getLine3()).isEqualTo("New Line 3");
      assertThat(addressResponse.getTown()).isEqualTo("New Town");
      assertThat(addressResponse.getCounty()).isEqualTo("New County");
      assertThat(addressResponse.getPostcode()).isEqualTo("NW1 1AA");

      // Verify database was updated
      final var updatedUser = userRepository.findById(user.getId()).orElseThrow();
      assertThat(updatedUser.getName()).isEqualTo("Updated Name");
      assertThat(updatedUser.getEmail()).isEqualTo("updated@example.com");
      assertThat(updatedUser.getPhoneNumber()).isEqualTo("+449876543210");
      var updatedAddress = updatedUser.getAddress();
      assertThat(updatedAddress.line1()).isEqualTo("New Line 1");
      assertThat(updatedAddress.line2()).isEqualTo("New Line 2");
      assertThat(updatedAddress.line3()).isEqualTo("New Line 3");
      assertThat(updatedAddress.town()).isEqualTo("New Town");
      assertThat(updatedAddress.county()).isEqualTo("New County");
      assertThat(updatedAddress.postcode()).isEqualTo("NW1 1AA");
    }

    @Test
    void shouldPartiallyUpdateOwnUserDetails() {
      // Given
      final var user = createAndSaveUser("user@gmail.com");
      final var updateRequest = new UpdateUserRequest().phoneNumber("+441112223334");

      // When
      final var response =
          restTemplate.exchange(
              "/v1/users/" + user.getId(),
              HttpMethod.PATCH,
              new HttpEntity<>(updateRequest, createAuthHeaders(user)),
              UserResponse.class);

      // Then
      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      assertThat(response.getBody()).isNotNull();
      final var userResponse = response.getBody();
      assertThat(userResponse.getPhoneNumber()).isEqualTo("+441112223334");
      assertThat(userResponse.getName()).isEqualTo("Test User"); // Unchanged

      // Verify database was updated
      final var updatedUser = userRepository.findById(user.getId()).orElseThrow();
      assertThat(updatedUser.getPhoneNumber()).isEqualTo("+441112223334");
      assertThat(updatedUser.getName()).isEqualTo("Test User"); // Unchanged
    }

    @Test
    void shouldFailToUpdateAnotherUsersDetails() {
      // Given
      final var user1 = createAndSaveUser("user1@example.com");
      final var user2 = createAndSaveUser("user2@example.com");
      var updateRequest = new UpdateUserRequest().name("Hacked Name");

      // When
      var response =
          restTemplate.exchange(
              "/v1/users/" + user2.getId(),
              HttpMethod.PATCH,
              new HttpEntity<>(updateRequest, createAuthHeaders(user1)),
              String.class);

      // Then
      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

      // Verify user2 was NOT updated
      var unchangedUser = userRepository.findById(user2.getId()).orElseThrow();
      assertThat(unchangedUser.getName()).isEqualTo("Test User");
    }

    @Test
    void shouldFailToUpdateUserWithoutAuthentication() {
      // Given
      final var user = createAndSaveUser("test@example.com");
      var updateRequest = new UpdateUserRequest().name("New Name");

      // When
      var response =
          restTemplate.exchange(
              "/v1/users/" + user.getId(),
              HttpMethod.PATCH,
              new HttpEntity<>(updateRequest, HttpHeaders.EMPTY),
              String.class);

      // Then
      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

      // Verify user was NOT updated
      var unchangedUser = userRepository.findById(user.getId()).orElseThrow();
      assertThat(unchangedUser.getName()).isEqualTo("Test User");
    }

    @Test
    void shouldFailToUpdateUserWhenEmailAlreadyExists() {
      // Given
      final var user1 = createAndSaveUser("user@gmail.com");
      final var user2 = createAndSaveUser("otheruser@gmail.com");
      final var updateRequest = new UpdateUserRequest().email(user1.getEmail());

      // When
      final var response =
          restTemplate.exchange(
              "/v1/users/" + user2.getId(),
              HttpMethod.PATCH,
              new HttpEntity<>(updateRequest, createAuthHeaders(user2)),
              String.class);

      // Then
      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
      // Verify user2's email was NOT updated
      final var unchangedUser = userRepository.findById(user2.getId()).orElseThrow();
      assertThat(unchangedUser.getEmail()).isEqualTo("otheruser@gmail.com");
    }
  }

  @Nested
  class DeleteUser {

    @Test
    void shouldDeleteUserSuccessfully() {
      // Given
      final var user = createAndSaveUser("user@example.com");

      // When
      var response =
          restTemplate.exchange(
              "/v1/users/" + user.getId(),
              HttpMethod.DELETE,
              new HttpEntity<>(createAuthHeaders(user)),
              Void.class);

      // Then
      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

      var deletedUser = userRepository.findById(user.getId());
      assertThat(deletedUser).isEmpty();
    }

    @Test
    void shouldFailToDeleteUserWhenTheyHaveBankAccounts() {
      // Given
      final var user = createAndSaveUser("user@example.com");

      // Create a bank account for the user
      var createRequest =
          new CreateBankAccountRequest()
              .name("My Account")
              .accountType(CreateBankAccountRequest.AccountTypeEnum.PERSONAL);
      restTemplate.postForEntity(
          "/v1/accounts",
          new HttpEntity<>(createRequest, createAuthHeaders(user)),
          BankAccountResponse.class);

      // When
      var response =
          restTemplate.exchange(
              "/v1/users/" + user.getId(),
              HttpMethod.DELETE,
              new HttpEntity<>(createAuthHeaders(user)),
              String.class);

      // Then
      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

      // Verify user was NOT deleted
      var unchangedUser = userRepository.findById(user.getId());
      assertThat(unchangedUser).isPresent();
    }

    @Test
    void shouldFailToDeleteAnotherUser() {
      // Given
      final var user1 = createAndSaveUser("user1@example.com");
      final var user2 = createAndSaveUser("user2@example.com");

      // When
      var response =
          restTemplate.exchange(
              "/v1/users/" + user2.getId(),
              HttpMethod.DELETE,
              new HttpEntity<>(createAuthHeaders(user1)),
              String.class);

      // Then
      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

      // Verify user2 was NOT deleted
      var unchangedUser = userRepository.findById(user2.getId());
      assertThat(unchangedUser).isPresent();
    }
  }
}
