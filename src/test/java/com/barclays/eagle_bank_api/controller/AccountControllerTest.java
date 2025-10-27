package com.barclays.eagle_bank_api.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.barclays.eagle_bank_api.TestcontainersConfiguration;
import com.barclays.eagle_bank_api.domain.AccountNumber;
import com.barclays.eagle_bank_api.domain.AccountType;
import com.barclays.eagle_bank_api.domain.Currency;
import com.barclays.eagle_bank_api.entity.User;
import com.barclays.eagle_bank_api.model.BankAccountResponse;
import com.barclays.eagle_bank_api.model.CreateBankAccountRequest;
import com.barclays.eagle_bank_api.model.CreateTransactionRequest;
import com.barclays.eagle_bank_api.model.ListBankAccountsResponse;
import com.barclays.eagle_bank_api.model.TransactionResponse;
import com.barclays.eagle_bank_api.model.UpdateBankAccountRequest;
import com.barclays.eagle_bank_api.repository.AccountRepository;
import com.barclays.eagle_bank_api.repository.TransactionRepository;
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

@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AccountControllerTest {

  @Autowired private TestRestTemplate restTemplate;
  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private AccountRepository accountRepository;
  @Autowired private TransactionRepository transactionRepository;
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

  private BankAccountResponse createAccount(User user, String accountName) {
    var createRequest =
        new CreateBankAccountRequest()
            .name(accountName)
            .accountType(CreateBankAccountRequest.AccountTypeEnum.PERSONAL);
    var response =
        restTemplate.postForEntity(
            "/v1/accounts",
            new HttpEntity<>(createRequest, createAuthHeaders(user)),
            BankAccountResponse.class);
    return response.getBody();
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
      assertThat(account.getBalance().value()).isEqualTo(0.0);
      assertThat(account.getUser().getId()).isEqualTo(user.getId());
      assertThat(account.getAccountType()).isEqualTo(AccountType.PERSONAL);
      assertThat(account.getBalance().currency()).isEqualTo(Currency.GBP);
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

  @Nested
  class FetchAccountByAccountNumber {

    @Test
    void shouldFetchAccountSuccessfully() {
      // Given
      var user = createAndSaveUser();
      var createdAccount = createAccount(user, "My Savings Account");

      // When
      var response =
          restTemplate.exchange(
              "/v1/accounts/" + createdAccount.getAccountNumber(),
              HttpMethod.GET,
              new HttpEntity<>(createAuthHeaders(user)),
              BankAccountResponse.class);

      // Then
      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      assertThat(response.getBody()).isNotNull();

      var accountResponse = response.getBody();
      assertThat(accountResponse.getAccountNumber()).isEqualTo(createdAccount.getAccountNumber());
      assertThat(accountResponse.getName()).isEqualTo("My Savings Account");
      assertThat(accountResponse.getSortCode())
          .isEqualTo(BankAccountResponse.SortCodeEnum._10_10_10);
      assertThat(accountResponse.getAccountType())
          .isEqualTo(BankAccountResponse.AccountTypeEnum.PERSONAL);
      assertThat(accountResponse.getBalance()).isEqualTo(0.0);
      assertThat(accountResponse.getCurrency()).isEqualTo(BankAccountResponse.CurrencyEnum.GBP);
      assertThat(accountResponse.getCreatedTimestamp()).isNotNull();
      assertThat(accountResponse.getUpdatedTimestamp()).isNotNull();
    }

    @Test
    void shouldReturnForbiddenWhenFetchingAnotherUsersAccount() {
      // Given
      var user1 = createAndSaveUser();
      var user2 = testAuthHelper.createAndSaveUser("user2@example.com");
      var user1Account = createAccount(user1, "User 1 Account");

      // When
      var response =
          restTemplate.exchange(
              "/v1/accounts/" + user1Account.getAccountNumber(),
              org.springframework.http.HttpMethod.GET,
              new HttpEntity<>(createAuthHeaders(user2)),
              String.class);

      // Then
      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void shouldReturnNotFoundWhenAccountDoesNotExist() {
      // Given
      var user = createAndSaveUser();
      var nonExistentAccountNumber = "01999999";

      // When
      var response =
          restTemplate.exchange(
              "/v1/accounts/" + nonExistentAccountNumber,
              org.springframework.http.HttpMethod.GET,
              new HttpEntity<>(createAuthHeaders(user)),
              String.class);

      // Then
      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void shouldFetchCorrectAccountWhenUserHasMultipleAccounts() {
      // Given
      var user = createAndSaveUser();
      createAccount(user, "Savings Account");
      var currentAccount = createAccount(user, "Current Account");

      // When
      var response =
          restTemplate.exchange(
              "/v1/accounts/" + currentAccount.getAccountNumber(),
              HttpMethod.GET,
              new HttpEntity<>(createAuthHeaders(user)),
              BankAccountResponse.class);

      // Then
      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().getAccountNumber())
          .isEqualTo(currentAccount.getAccountNumber());
      assertThat(response.getBody().getName()).isEqualTo("Current Account");
    }
  }

  @Nested
  class ListAccounts {

    @Test
    void shouldListAllAccountsForUser() {
      // Given
      var user = createAndSaveUser();
      var savingsAccount = createAccount(user, "Savings Account");
      var currentAccount = createAccount(user, "Current Account");

      // When
      var response =
          restTemplate.exchange(
              "/v1/accounts",
              HttpMethod.GET,
              new HttpEntity<>(createAuthHeaders(user)),
              ListBankAccountsResponse.class);

      // Then
      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().getAccounts()).hasSize(2);
      assertThat(response.getBody().getAccounts())
          .extracting("accountNumber")
          .containsExactlyInAnyOrder(
              savingsAccount.getAccountNumber(), currentAccount.getAccountNumber());
      assertThat(response.getBody().getAccounts())
          .extracting("name")
          .containsExactlyInAnyOrder("Savings Account", "Current Account");
    }

    @Test
    void shouldReturnEmptyListWhenUserHasNoAccounts() {
      // Given
      var user = createAndSaveUser();

      // When
      var response =
          restTemplate.exchange(
              "/v1/accounts",
              HttpMethod.GET,
              new HttpEntity<>(createAuthHeaders(user)),
              ListBankAccountsResponse.class);

      // Then
      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().getAccounts()).isEmpty();
    }

    @Test
    void shouldOnlyReturnAccountsForAuthenticatedUser() {
      // Given
      var user1 = createAndSaveUser();
      var user2 = testAuthHelper.createAndSaveUser("user2@example.com");
      var user1Account = createAccount(user1, "User 1 Account");
      createAccount(user2, "User 2 Account");

      // When
      var response =
          restTemplate.exchange(
              "/v1/accounts",
              HttpMethod.GET,
              new HttpEntity<>(createAuthHeaders(user1)),
              ListBankAccountsResponse.class);

      // Then
      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().getAccounts()).hasSize(1);
      assertThat(response.getBody().getAccounts().getFirst().getAccountNumber())
          .isEqualTo(user1Account.getAccountNumber());
      assertThat(response.getBody().getAccounts().getFirst().getName()).isEqualTo("User 1 Account");
    }

    @Test
    void shouldFailToListAccountsWithoutAuthentication() {
      // When
      var response =
          restTemplate.exchange(
              "/v1/accounts", HttpMethod.GET, new HttpEntity<>(null), String.class);

      // Then
      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
  }

  @Nested
  class UpdateAccount {

    @Test
    void shouldUpdateAccountSuccessfully() {
      // Given
      var user = createAndSaveUser();
      var account = createAccount(user, "Old Account Name");
      var updateRequest =
          new UpdateBankAccountRequest()
              .name("New Account Name")
              .accountType(UpdateBankAccountRequest.AccountTypeEnum.PERSONAL);

      // When
      var response =
          restTemplate.exchange(
              "/v1/accounts/" + account.getAccountNumber(),
              HttpMethod.PATCH,
              new HttpEntity<>(updateRequest, createAuthHeaders(user)),
              BankAccountResponse.class);

      // Then
      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().getAccountNumber()).isEqualTo(account.getAccountNumber());
      assertThat(response.getBody().getName()).isEqualTo("New Account Name");
      assertThat(response.getBody().getAccountType())
          .isEqualTo(BankAccountResponse.AccountTypeEnum.PERSONAL);
      assertThat(response.getBody().getBalance()).isEqualTo(0.0);
      assertThat(response.getBody().getCurrency()).isEqualTo(BankAccountResponse.CurrencyEnum.GBP);
      assertThat(response.getBody().getSortCode())
          .isEqualTo(BankAccountResponse.SortCodeEnum._10_10_10);

      // Verify database state
      var updatedAccount =
          accountRepository.findByAccountNumber(new AccountNumber(account.getAccountNumber()));
      assertThat(updatedAccount).isPresent();
      assertThat(updatedAccount.get().getName()).isEqualTo("New Account Name");
    }

    @Test
    void shouldReturnForbiddenWhenUpdatingAnotherUsersAccount() {
      // Given
      var user1 = createAndSaveUser();
      var user2 = testAuthHelper.createAndSaveUser("user2@example.com");
      var user1Account = createAccount(user1, "User 1 Account");
      var updateRequest = new UpdateBankAccountRequest().name("Hacked Name");

      // When
      var response =
          restTemplate.exchange(
              "/v1/accounts/" + user1Account.getAccountNumber(),
              HttpMethod.PATCH,
              new HttpEntity<>(updateRequest, createAuthHeaders(user2)),
              String.class);

      // Then
      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void shouldReturnNotFoundWhenAccountDoesNotExist() {
      // Given
      var user = createAndSaveUser();
      var nonExistentAccountNumber = "01999999";
      var updateRequest = new UpdateBankAccountRequest().name("New Name");

      // When
      var response =
          restTemplate.exchange(
              "/v1/accounts/" + nonExistentAccountNumber,
              HttpMethod.PATCH,
              new HttpEntity<>(updateRequest, createAuthHeaders(user)),
              String.class);

      // Then
      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void shouldUpdateOnlyProvidedFields() {
      // Given
      var user = createAndSaveUser();
      var account = createAccount(user, "Original Name");
      var updateRequest =
          new UpdateBankAccountRequest()
              .accountType(UpdateBankAccountRequest.AccountTypeEnum.PERSONAL);

      // When
      var response =
          restTemplate.exchange(
              "/v1/accounts/" + account.getAccountNumber(),
              HttpMethod.PATCH,
              new HttpEntity<>(updateRequest, createAuthHeaders(user)),
              BankAccountResponse.class);

      // Then
      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().getName()).isEqualTo("Original Name");
      assertThat(response.getBody().getAccountType())
          .isEqualTo(BankAccountResponse.AccountTypeEnum.PERSONAL);
    }
  }

  @Nested
  class DeleteAccount {

    @Test
    void shouldDeleteAccountSuccessfully() {
      // Given
      var user = createAndSaveUser();
      var account = createAccount(user, "Account To Delete");

      // When
      var response =
          restTemplate.exchange(
              "/v1/accounts/" + account.getAccountNumber(),
              HttpMethod.DELETE,
              new HttpEntity<>(createAuthHeaders(user)),
              Void.class);

      // Then
      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

      // Verify account was deleted from database
      var accounts = accountRepository.findByUserId(user.getId());
      assertThat(accounts).isEmpty();
    }

    @Test
    void shouldReturnForbiddenWhenDeletingAnotherUsersAccount() {
      // Given
      var user1 = createAndSaveUser();
      var user2 = testAuthHelper.createAndSaveUser("user2@example.com");
      var user1Account = createAccount(user1, "User 1 Account");

      // When
      var response =
          restTemplate.exchange(
              "/v1/accounts/" + user1Account.getAccountNumber(),
              HttpMethod.DELETE,
              new HttpEntity<>(createAuthHeaders(user2)),
              String.class);

      // Then
      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

      // Verify account was NOT deleted
      var accounts = accountRepository.findByUserId(user1.getId());
      assertThat(accounts).hasSize(1);
    }

    @Test
    void shouldReturnNotFoundWhenDeletingNonExistentAccount() {
      // Given
      var user = createAndSaveUser();
      var nonExistentAccountNumber = "01999999";

      // When
      var response =
          restTemplate.exchange(
              "/v1/accounts/" + nonExistentAccountNumber,
              HttpMethod.DELETE,
              new HttpEntity<>(createAuthHeaders(user)),
              String.class);

      // Then
      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void shouldDeleteTransactionsWhenAccountIsDeleted() {
      // Given
      var user = createAndSaveUser();
      var accountToDelete = createAccount(user, "Account To Delete");
      var accountToKeep = createAccount(user, "Account To Keep");

      // Create multiple transactions for the account to delete
      var depositRequest1 =
          new CreateTransactionRequest()
              .amount(100.0)
              .currency(CreateTransactionRequest.CurrencyEnum.GBP)
              .type(CreateTransactionRequest.TypeEnum.DEPOSIT)
              .reference("Deposit 1");
      restTemplate.postForEntity(
          "/v1/accounts/" + accountToDelete.getAccountNumber() + "/transactions",
          new HttpEntity<>(depositRequest1, createAuthHeaders(user)),
          TransactionResponse.class);

      var depositRequest2 =
          new CreateTransactionRequest()
              .amount(50.0)
              .currency(CreateTransactionRequest.CurrencyEnum.GBP)
              .type(CreateTransactionRequest.TypeEnum.DEPOSIT)
              .reference("Deposit 2");
      restTemplate.postForEntity(
          "/v1/accounts/" + accountToDelete.getAccountNumber() + "/transactions",
          new HttpEntity<>(depositRequest2, createAuthHeaders(user)),
          TransactionResponse.class);

      var withdrawalRequest =
          new CreateTransactionRequest()
              .amount(30.0)
              .currency(CreateTransactionRequest.CurrencyEnum.GBP)
              .type(CreateTransactionRequest.TypeEnum.WITHDRAWAL)
              .reference("Withdrawal 1");
      restTemplate.postForEntity(
          "/v1/accounts/" + accountToDelete.getAccountNumber() + "/transactions",
          new HttpEntity<>(withdrawalRequest, createAuthHeaders(user)),
          TransactionResponse.class);

      // Create transactions for the account to keep
      var keepDepositRequest =
          new CreateTransactionRequest()
              .amount(200.0)
              .currency(CreateTransactionRequest.CurrencyEnum.GBP)
              .type(CreateTransactionRequest.TypeEnum.DEPOSIT)
              .reference("Keep Deposit");
      restTemplate.postForEntity(
          "/v1/accounts/" + accountToKeep.getAccountNumber() + "/transactions",
          new HttpEntity<>(keepDepositRequest, createAuthHeaders(user)),
          TransactionResponse.class);

      // Verify transactions were created
      var transactionsBeforeDelete =
          transactionRepository.findByAccountAccountNumberOrderByCreatedTimestampAsc(
              new AccountNumber(accountToDelete.getAccountNumber()));
      assertThat(transactionsBeforeDelete).hasSize(3);

      var keepTransactionsBeforeDelete =
          transactionRepository.findByAccountAccountNumberOrderByCreatedTimestampAsc(
              new AccountNumber(accountToKeep.getAccountNumber()));
      assertThat(keepTransactionsBeforeDelete).hasSize(1);

      // When - Delete the first account
      var deleteResponse =
          restTemplate.exchange(
              "/v1/accounts/" + accountToDelete.getAccountNumber(),
              HttpMethod.DELETE,
              new HttpEntity<>(createAuthHeaders(user)),
              Void.class);

      // Then - Verify the deleted account is gone
      assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
      var accounts = accountRepository.findByUserId(user.getId());
      assertThat(accounts).hasSize(1);
      assertThat(accounts.getFirst().getAccountNumber().value())
          .isEqualTo(accountToKeep.getAccountNumber());

      // Verify transactions for deleted account were cascade deleted
      var transactionsAfterDelete =
          transactionRepository.findByAccountAccountNumberOrderByCreatedTimestampAsc(
              new AccountNumber(accountToDelete.getAccountNumber()));
      assertThat(transactionsAfterDelete).isEmpty();

      // Verify transactions for the kept account still exist
      var keepTransactionsAfterDelete =
          transactionRepository.findByAccountAccountNumberOrderByCreatedTimestampAsc(
              new AccountNumber(accountToKeep.getAccountNumber()));
      assertThat(keepTransactionsAfterDelete).hasSize(1);
      assertThat(keepTransactionsAfterDelete.getFirst().getReference()).isEqualTo("Keep Deposit");
      assertThat(keepTransactionsAfterDelete.getFirst().getAmount().value()).isEqualTo(200.0);
    }
  }
}
