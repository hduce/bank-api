package com.barclays.eagle_bank_api.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.barclays.eagle_bank_api.TestcontainersConfiguration;
import com.barclays.eagle_bank_api.domain.AccountNumber;
import com.barclays.eagle_bank_api.entity.User;
import com.barclays.eagle_bank_api.model.BankAccountResponse;
import com.barclays.eagle_bank_api.model.CreateBankAccountRequest;
import com.barclays.eagle_bank_api.model.CreateTransactionRequest;
import com.barclays.eagle_bank_api.model.TransactionResponse;
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
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TransactionControllerTest {

  @Autowired private TestRestTemplate restTemplate;
  @Autowired private JdbcTemplate jdbcTemplate;
  @Autowired private AccountRepository accountRepository;
  @Autowired private TransactionRepository transactionRepository;
  @Autowired private TestAuthHelper testAuthHelper;

  @BeforeEach
  void setUp() {
    jdbcTemplate.execute("TRUNCATE TABLE transactions, accounts, users CASCADE");
  }

  private User createAndSaveUser(String email) {
    return testAuthHelper.createAndSaveUser(email);
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
  class CreateTransaction {

    @Test
    void shouldCreateDepositSuccessfully() {
      // Given
      var user = createAndSaveUser("user@example.com");
      var account = createAccount(user, "Test Account");

      var request = new CreateTransactionRequest();
      request.setAmount(50.00);
      request.setCurrency(CreateTransactionRequest.CurrencyEnum.GBP);
      request.setType(CreateTransactionRequest.TypeEnum.DEPOSIT);
      request.setReference("Test deposit");

      // When
      var response =
          restTemplate.postForEntity(
              "/v1/accounts/{accountNumber}/transactions",
              new HttpEntity<>(request, createAuthHeaders(user)),
              TransactionResponse.class,
              account.getAccountNumber());

      // Then - Verify HTTP response
      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
      assertThat(response.getBody()).isNotNull();

      var transactionResponse = response.getBody();
      assertThat(transactionResponse.getId()).isNotNull().startsWith("tan-");
      assertThat(transactionResponse.getAmount()).isEqualTo(50.00);
      assertThat(transactionResponse.getCurrency()).isEqualTo(TransactionResponse.CurrencyEnum.GBP);
      assertThat(transactionResponse.getType()).isEqualTo(TransactionResponse.TypeEnum.DEPOSIT);
      assertThat(transactionResponse.getReference()).isEqualTo("Test deposit");
      assertThat(transactionResponse.getCreatedTimestamp()).isNotNull();

      // Verify account balance was updated
      var updatedAccount =
          accountRepository.findByAccountNumber(new AccountNumber(account.getAccountNumber()));
      assertThat(updatedAccount).isPresent();
      assertThat(updatedAccount.get().getBalance().value()).isEqualTo(50.00);
      assertThat(updatedAccount.get().getVersion()).isEqualTo(1);

      // Verify transaction was persisted
      var persistedTransaction = transactionRepository.findById(transactionResponse.getId());
      assertThat(persistedTransaction).isPresent();
      assertThat(persistedTransaction.get().getAmount().value()).isEqualTo(50.00);
      assertThat(persistedTransaction.get().getType().name()).isEqualTo("DEPOSIT");
      assertThat(persistedTransaction.get().getReference()).isEqualTo("Test deposit");
    }

    @Test
    void shouldCreateWithdrawalSuccessfully() {
      // Given
      var user = createAndSaveUser("user@example.com");
      var account = createAccount(user, "Test Account");

      // First deposit some money
      var depositRequest = new CreateTransactionRequest();
      depositRequest.setAmount(100.00);
      depositRequest.setCurrency(CreateTransactionRequest.CurrencyEnum.GBP);
      depositRequest.setType(CreateTransactionRequest.TypeEnum.DEPOSIT);

      restTemplate.postForEntity(
          "/v1/accounts/{accountNumber}/transactions",
          new HttpEntity<>(depositRequest, createAuthHeaders(user)),
          TransactionResponse.class,
          account.getAccountNumber());

      // Withdraw
      var withdrawalRequest = new CreateTransactionRequest();
      withdrawalRequest.setAmount(30.00);
      withdrawalRequest.setCurrency(CreateTransactionRequest.CurrencyEnum.GBP);
      withdrawalRequest.setType(CreateTransactionRequest.TypeEnum.WITHDRAWAL);
      withdrawalRequest.setReference("Test withdrawal");

      // When
      var response =
          restTemplate.postForEntity(
              "/v1/accounts/{accountNumber}/transactions",
              new HttpEntity<>(withdrawalRequest, createAuthHeaders(user)),
              TransactionResponse.class,
              account.getAccountNumber());

      // Then - Verify HTTP response
      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
      assertThat(response.getBody()).isNotNull();

      var transactionResponse = response.getBody();
      assertThat(transactionResponse.getId()).isNotNull().startsWith("tan-");
      assertThat(transactionResponse.getAmount()).isEqualTo(30.00);
      assertThat(transactionResponse.getCurrency()).isEqualTo(TransactionResponse.CurrencyEnum.GBP);
      assertThat(transactionResponse.getType()).isEqualTo(TransactionResponse.TypeEnum.WITHDRAWAL);
      assertThat(transactionResponse.getReference()).isEqualTo("Test withdrawal");
      assertThat(transactionResponse.getCreatedTimestamp()).isNotNull();

      // Verify account balance was updated
      var updatedAccount =
          accountRepository.findByAccountNumber(new AccountNumber(account.getAccountNumber()));
      assertThat(updatedAccount).isPresent();
      assertThat(updatedAccount.get().getBalance().value()).isEqualTo(70.00);
      assertThat(updatedAccount.get().getVersion()).isEqualTo(2);

      // Verify transaction was persisted
      var persistedTransaction = transactionRepository.findById(transactionResponse.getId());
      assertThat(persistedTransaction).isPresent();
      assertThat(persistedTransaction.get().getAmount().value()).isEqualTo(30.00);
      assertThat(persistedTransaction.get().getType().name()).isEqualTo("WITHDRAWAL");
      assertThat(persistedTransaction.get().getReference()).isEqualTo("Test withdrawal");
    }

    @Test
    void shouldReturnUnprocessableEntityWhenWithdrawalExceedsBalance() {
      // Given
      var user = createAndSaveUser("user@example.com");
      var account = createAccount(user, "Test Account");

      // First deposit some money
      var depositRequest = new CreateTransactionRequest();
      depositRequest.setAmount(100.00);
      depositRequest.setCurrency(CreateTransactionRequest.CurrencyEnum.GBP);
      depositRequest.setType(CreateTransactionRequest.TypeEnum.DEPOSIT);

      restTemplate.postForEntity(
          "/v1/accounts/{accountNumber}/transactions",
          new HttpEntity<>(depositRequest, createAuthHeaders(user)),
          TransactionResponse.class,
          account.getAccountNumber());

      // Try to withdraw more than balance
      var withdrawalRequest = new CreateTransactionRequest();
      withdrawalRequest.setAmount(150.00);
      withdrawalRequest.setCurrency(CreateTransactionRequest.CurrencyEnum.GBP);
      withdrawalRequest.setType(CreateTransactionRequest.TypeEnum.WITHDRAWAL);

      // When
      var response =
          restTemplate.postForEntity(
              "/v1/accounts/{accountNumber}/transactions",
              new HttpEntity<>(withdrawalRequest, createAuthHeaders(user)),
              TransactionResponse.class,
              account.getAccountNumber());

      // Then
      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);

      // Verify account balance was not changed
      var updatedAccount =
          accountRepository.findByAccountNumber(new AccountNumber(account.getAccountNumber()));
      assertThat(updatedAccount).isPresent();
      assertThat(updatedAccount.get().getBalance().value()).isEqualTo(100.00);
    }

    @Test
    void shouldReturnForbiddenWhenCreatingTransactionForAnotherUsersAccount() {
      // Given
      var user1 = createAndSaveUser("user1@example.com");
      var user2 = createAndSaveUser("user2@example.com");
      var account = createAccount(user1, "User1 Account");

      var request = new CreateTransactionRequest();
      request.setAmount(50.00);
      request.setCurrency(CreateTransactionRequest.CurrencyEnum.GBP);
      request.setType(CreateTransactionRequest.TypeEnum.DEPOSIT);

      // When - user2 tries to deposit into user1's account
      var response =
          restTemplate.postForEntity(
              "/v1/accounts/{accountNumber}/transactions",
              new HttpEntity<>(request, createAuthHeaders(user2)),
              TransactionResponse.class,
              account.getAccountNumber());

      // Then
      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

      // Verify account balance was not changed
      var updatedAccount =
          accountRepository.findByAccountNumber(new AccountNumber(account.getAccountNumber()));
      assertThat(updatedAccount).isPresent();
      assertThat(updatedAccount.get().getBalance().value()).isEqualTo(0.00);
    }

    @Test
    void shouldReturnNotFoundWhenCreatingTransactionForNonExistentAccount() {
      // Given
      var user = createAndSaveUser("user@example.com");

      var request = new CreateTransactionRequest();
      request.setAmount(50.00);
      request.setCurrency(CreateTransactionRequest.CurrencyEnum.GBP);
      request.setType(CreateTransactionRequest.TypeEnum.DEPOSIT);

      // When
      var response =
          restTemplate.postForEntity(
              "/v1/accounts/{accountNumber}/transactions",
              new HttpEntity<>(request, createAuthHeaders(user)),
              TransactionResponse.class,
              "01999999");

      // Then
      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void shouldReturnBadRequestWhenMissingRequiredFields() {
      // Given
      var user = createAndSaveUser("user@example.com");
      var account = createAccount(user, "Test Account");

      var request = new CreateTransactionRequest();
      // Missing amount, currency, and type

      // When
      var response =
          restTemplate.postForEntity(
              "/v1/accounts/{accountNumber}/transactions",
              new HttpEntity<>(request, createAuthHeaders(user)),
              TransactionResponse.class,
              account.getAccountNumber());

      // Then
      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void shouldCreateDepositWithoutReference() {
      // Given
      var user = createAndSaveUser("user@example.com");
      var account = createAccount(user, "Test Account");

      var request = new CreateTransactionRequest();
      request.setAmount(25.00);
      request.setCurrency(CreateTransactionRequest.CurrencyEnum.GBP);
      request.setType(CreateTransactionRequest.TypeEnum.DEPOSIT);

      // When
      var response =
          restTemplate.postForEntity(
              "/v1/accounts/{accountNumber}/transactions",
              new HttpEntity<>(request, createAuthHeaders(user)),
              TransactionResponse.class,
              account.getAccountNumber());

      // Then - Verify HTTP response
      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
      assertThat(response.getBody()).isNotNull();

      var transactionResponse = response.getBody();
      assertThat(transactionResponse.getId()).isNotNull().startsWith("tan-");
      assertThat(transactionResponse.getAmount()).isEqualTo(25.00);
      assertThat(transactionResponse.getReference()).isNull();

      // Verify account balance was updated
      var updatedAccount =
          accountRepository.findByAccountNumber(new AccountNumber(account.getAccountNumber()));
      assertThat(updatedAccount).isPresent();
      assertThat(updatedAccount.get().getBalance().value()).isEqualTo(25.00);
    }

    @Test
    void shouldReturnUnprocessableEntityWhenDepositExceedsMaximumBalance() {
      // Given
      var user = createAndSaveUser("user@example.com");
      var account = createAccount(user, "Test Account");

      // First deposit to get close to max balance (10000)
      var initialDeposit = new CreateTransactionRequest();
      initialDeposit.setAmount(9500.00);
      initialDeposit.setCurrency(CreateTransactionRequest.CurrencyEnum.GBP);
      initialDeposit.setType(CreateTransactionRequest.TypeEnum.DEPOSIT);

      restTemplate.postForEntity(
          "/v1/accounts/{accountNumber}/transactions",
          new HttpEntity<>(initialDeposit, createAuthHeaders(user)),
          TransactionResponse.class,
          account.getAccountNumber());

      // Now try to deposit more than would exceed 10000
      var request = new CreateTransactionRequest();
      request.setAmount(600.00);
      request.setCurrency(CreateTransactionRequest.CurrencyEnum.GBP);
      request.setType(CreateTransactionRequest.TypeEnum.DEPOSIT);

      // When
      var response =
          restTemplate.postForEntity(
              "/v1/accounts/{accountNumber}/transactions",
              new HttpEntity<>(request, createAuthHeaders(user)),
              TransactionResponse.class,
              account.getAccountNumber());

      // Then
      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);

      // Verify account balance was not changed from initial deposit
      var updatedAccount =
          accountRepository.findByAccountNumber(new AccountNumber(account.getAccountNumber()));
      assertThat(updatedAccount).isPresent();
      assertThat(updatedAccount.get().getBalance().value()).isEqualTo(9500.00);
    }

    @Test
    void shouldCreateMultipleTransactionsAndPersistAll() {
      // Given
      var user = createAndSaveUser("user@example.com");
      var account = createAccount(user, "Test Account");

      // Create multiple transactions
      var deposit1 = new CreateTransactionRequest();
      deposit1.setAmount(100.00);
      deposit1.setCurrency(CreateTransactionRequest.CurrencyEnum.GBP);
      deposit1.setType(CreateTransactionRequest.TypeEnum.DEPOSIT);
      deposit1.setReference("First deposit");

      var deposit2 = new CreateTransactionRequest();
      deposit2.setAmount(50.00);
      deposit2.setCurrency(CreateTransactionRequest.CurrencyEnum.GBP);
      deposit2.setType(CreateTransactionRequest.TypeEnum.DEPOSIT);
      deposit2.setReference("Second deposit");

      var withdrawal1 = new CreateTransactionRequest();
      withdrawal1.setAmount(30.00);
      withdrawal1.setCurrency(CreateTransactionRequest.CurrencyEnum.GBP);
      withdrawal1.setType(CreateTransactionRequest.TypeEnum.WITHDRAWAL);
      withdrawal1.setReference("First withdrawal");

      var deposit3 = new CreateTransactionRequest();
      deposit3.setAmount(75.00);
      deposit3.setCurrency(CreateTransactionRequest.CurrencyEnum.GBP);
      deposit3.setType(CreateTransactionRequest.TypeEnum.DEPOSIT);
      deposit3.setReference("Third deposit");

      // When - Create all transactions
      var response1 =
          restTemplate.postForEntity(
              "/v1/accounts/{accountNumber}/transactions",
              new HttpEntity<>(deposit1, createAuthHeaders(user)),
              TransactionResponse.class,
              account.getAccountNumber());

      var response2 =
          restTemplate.postForEntity(
              "/v1/accounts/{accountNumber}/transactions",
              new HttpEntity<>(deposit2, createAuthHeaders(user)),
              TransactionResponse.class,
              account.getAccountNumber());

      var response3 =
          restTemplate.postForEntity(
              "/v1/accounts/{accountNumber}/transactions",
              new HttpEntity<>(withdrawal1, createAuthHeaders(user)),
              TransactionResponse.class,
              account.getAccountNumber());

      var response4 =
          restTemplate.postForEntity(
              "/v1/accounts/{accountNumber}/transactions",
              new HttpEntity<>(deposit3, createAuthHeaders(user)),
              TransactionResponse.class,
              account.getAccountNumber());

      // Then - Verify all responses were successful
      assertThat(response1.getStatusCode()).isEqualTo(HttpStatus.CREATED);
      assertThat(response2.getStatusCode()).isEqualTo(HttpStatus.CREATED);
      assertThat(response3.getStatusCode()).isEqualTo(HttpStatus.CREATED);
      assertThat(response4.getStatusCode()).isEqualTo(HttpStatus.CREATED);

      // Verify final account balance is correct (100 + 50 - 30 + 75 = 195)
      var updatedAccount =
          accountRepository.findByAccountNumber(new AccountNumber(account.getAccountNumber()));
      assertThat(updatedAccount).isPresent();
      assertThat(updatedAccount.get().getBalance().value()).isEqualTo(195.00);
      assertThat(updatedAccount.get().getVersion()).isEqualTo(4);

      // Verify all 4 transactions were persisted
      var transactions =
          transactionRepository.findByAccountAccountNumberOrderByCreatedTimestampAsc(
              new AccountNumber(account.getAccountNumber()));
      assertThat(transactions).hasSize(4);

      // Verify transaction details
      assertThat(transactions.getFirst().getAmount().value()).isEqualTo(100.00);
      assertThat(transactions.getFirst().getType().name()).isEqualTo("DEPOSIT");
      assertThat(transactions.getFirst().getReference()).isEqualTo("First deposit");

      assertThat(transactions.get(1).getAmount().value()).isEqualTo(50.00);
      assertThat(transactions.get(1).getType().name()).isEqualTo("DEPOSIT");
      assertThat(transactions.get(1).getReference()).isEqualTo("Second deposit");

      assertThat(transactions.get(2).getAmount().value()).isEqualTo(30.00);
      assertThat(transactions.get(2).getType().name()).isEqualTo("WITHDRAWAL");
      assertThat(transactions.get(2).getReference()).isEqualTo("First withdrawal");

      assertThat(transactions.get(3).getAmount().value()).isEqualTo(75.00);
      assertThat(transactions.get(3).getType().name()).isEqualTo("DEPOSIT");
      assertThat(transactions.get(3).getReference()).isEqualTo("Third deposit");
    }
  }
}
