package com.barclays.eagle_bank_api.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.barclays.eagle_bank_api.TestcontainersConfiguration;
import com.barclays.eagle_bank_api.entity.AccountType;
import com.barclays.eagle_bank_api.entity.Currency;
import com.barclays.eagle_bank_api.entity.User;
import com.barclays.eagle_bank_api.model.BankAccountResponse;
import com.barclays.eagle_bank_api.model.CreateBankAccountRequest;
import com.barclays.eagle_bank_api.repository.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AccountControllerTest {

  @Autowired private TestRestTemplate restTemplate;
  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private AccountRepository accountRepository;
  @Autowired private TestAuthHelper testAuthHelper;

  @BeforeEach
  void setUp() {
    jdbcTemplate.execute("TRUNCATE TABLE accounts, users CASCADE");
  }

  private User createAndSaveUser() {
    return testAuthHelper.createAndSaveUser("user@example.com");
  }

  private HttpHeaders createAuthHeaders(User user) {
    return testAuthHelper.createAuthHeaders(user);
  }

  @Nested
  class CreateAccount {

    @Test
    void shouldCreateAccountSuccessfully() {
      // Given
      var user = createAndSaveUser();
      var createRequest =
          new CreateBankAccountRequest()
              .name("My Savings Account")
              .accountType(CreateBankAccountRequest.AccountTypeEnum.PERSONAL);

      // When
      var response =
          restTemplate.postForEntity(
              "/v1/accounts",
              new HttpEntity<>(createRequest, createAuthHeaders(user)),
              BankAccountResponse.class);

      // Then - Verify HTTP response
      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
      assertThat(response.getBody()).isNotNull();

      var accountResponse = response.getBody();
      assertThat(accountResponse.getAccountNumber()).isNotNull().startsWith("01");
      assertThat(accountResponse.getAccountNumber()).hasSize(8);
      assertThat(accountResponse.getSortCode())
          .isEqualTo(BankAccountResponse.SortCodeEnum._10_10_10);
      assertThat(accountResponse.getName()).isEqualTo("My Savings Account");
      assertThat(accountResponse.getAccountType())
          .isEqualTo(BankAccountResponse.AccountTypeEnum.PERSONAL);
      assertThat(accountResponse.getBalance()).isEqualTo(0.0);
      assertThat(accountResponse.getCurrency()).isEqualTo(BankAccountResponse.CurrencyEnum.GBP);
      assertThat(accountResponse.getCreatedTimestamp()).isNotNull();
      assertThat(accountResponse.getUpdatedTimestamp()).isNotNull();

      // Verify database state
      var accounts = accountRepository.findByUserId(user.getId());
      assertThat(accounts).hasSize(1);
      var account = accounts.getFirst();
      assertThat(account.getAccountNumber().value()).isEqualTo(accountResponse.getAccountNumber());
      assertThat(account.getSortCode()).isEqualTo("10-10-10");
      assertThat(account.getName()).isEqualTo("My Savings Account");
      assertThat(account.getBalance()).isEqualTo(0.0);
      assertThat(account.getUser().getId()).isEqualTo(user.getId());
      assertThat(account.getAccountType()).isEqualTo(AccountType.PERSONAL);
      assertThat(account.getCurrency()).isEqualTo(Currency.GBP);
    }

    @Test
    void shouldFailWhenAccountTypeIsMissing() {
      // Given
      var user = createAndSaveUser();
      var createRequest = new CreateBankAccountRequest().name("My Savings Account");

      // When
      var response =
          restTemplate.postForEntity(
              "/v1/accounts",
              new HttpEntity<>(createRequest, createAuthHeaders(user)),
              String.class);

      // Then
      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void shouldFailToCreateAccountWithoutAuthentication() {
      // Given
      var createRequest =
          new CreateBankAccountRequest()
              .name("My Savings Account")
              .accountType(CreateBankAccountRequest.AccountTypeEnum.PERSONAL);

      // When
      var response =
          restTemplate.postForEntity("/v1/accounts", new HttpEntity<>(createRequest), String.class);

      // Then
      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void shouldAllowCreatingMultipleAccountsForSameUser() {
      // Given
      var user = createAndSaveUser();
      var firstRequest =
          new CreateBankAccountRequest()
              .name("Savings Account")
              .accountType(CreateBankAccountRequest.AccountTypeEnum.PERSONAL);
      var firstResponse =
          restTemplate.postForEntity(
              "/v1/accounts",
              new HttpEntity<>(firstRequest, createAuthHeaders(user)),
              BankAccountResponse.class);
      var secondRequest =
          new CreateBankAccountRequest()
              .name("Current Account")
              .accountType(CreateBankAccountRequest.AccountTypeEnum.PERSONAL);

      // When
      var secondResponse =
          restTemplate.postForEntity(
              "/v1/accounts",
              new HttpEntity<>(secondRequest, createAuthHeaders(user)),
              BankAccountResponse.class);

      // Then
      assertThat(firstResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
      assertThat(secondResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
      assertThat(firstResponse.getBody()).isNotNull();
      assertThat(secondResponse.getBody()).isNotNull();

      // Verify unique account numbers
      assertThat(firstResponse.getBody().getAccountNumber())
          .isNotEqualTo(secondResponse.getBody().getAccountNumber());

      // Verify database state
      var accounts = accountRepository.findByUserId(user.getId());
      assertThat(accounts).hasSize(2);
      assertThat(accounts)
          .extracting("name")
          .containsExactlyInAnyOrder("Savings Account", "Current Account");
    }
  }
}
