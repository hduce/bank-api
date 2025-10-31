package com.hduce.eagle_bank_api.controller;

import com.hduce.eagle_bank_api.api.AccountApi;
import com.hduce.eagle_bank_api.domain.AccountNumber;
import com.hduce.eagle_bank_api.entity.User;
import com.hduce.eagle_bank_api.mapper.AccountDtoMapper;
import com.hduce.eagle_bank_api.model.BankAccountResponse;
import com.hduce.eagle_bank_api.model.CreateBankAccountRequest;
import com.hduce.eagle_bank_api.model.ListBankAccountsResponse;
import com.hduce.eagle_bank_api.model.UpdateBankAccountRequest;
import com.hduce.eagle_bank_api.service.AccountService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class AccountController implements AccountApi {

  private final AccountService accountService;
  private final AccountDtoMapper accountMapper;

  public AccountController(AccountService accountService, AccountDtoMapper accountMapper) {
    this.accountService = accountService;
    this.accountMapper = accountMapper;
  }

  @Override
  public ResponseEntity<BankAccountResponse> createAccount(
      CreateBankAccountRequest createBankAccountRequest) {
    var user = getAuthenticatedUser();
    log.info("Creating new account for user: {}", user.getId());
    var account = accountService.createAccount(createBankAccountRequest, user);
    log.info(
        "Successfully created account {} for user: {}", account.getAccountNumber(), user.getId());
    return new ResponseEntity<>(accountMapper.toDto(account), HttpStatus.CREATED);
  }

  @Override
  public ResponseEntity<Void> deleteAccountByAccountNumber(String accountNumber) {
    var user = getAuthenticatedUser();
    log.info("Deleting account {} for user: {}", accountNumber, user.getId());
    accountService.deleteAccount(new AccountNumber(accountNumber), user);
    log.info("Successfully deleted account {} for user: {}", accountNumber, user.getId());
    return ResponseEntity.noContent().build();
  }

  @Override
  public ResponseEntity<BankAccountResponse> fetchAccountByAccountNumber(String accountNumber) {
    var user = getAuthenticatedUser();
    log.debug("Fetching account {} for user: {}", accountNumber, user.getId());
    var account = accountService.getAccountByAccountNumber(new AccountNumber(accountNumber), user);
    return ResponseEntity.ok(accountMapper.toDto(account));
  }

  @Override
  public ResponseEntity<ListBankAccountsResponse> listAccounts() {
    var user = getAuthenticatedUser();
    log.debug("Listing accounts for user: {}", user.getId());
    var accounts = accountService.listAccountsForUser(user);
    var accountResponses = accounts.stream().map(accountMapper::toDto).toList();

    var response = new ListBankAccountsResponse();
    response.setAccounts(accountResponses);
    log.debug("Found {} accounts for user: {}", accounts.size(), user.getId());
    return ResponseEntity.ok(response);
  }

  @Override
  public ResponseEntity<BankAccountResponse> updateAccountByAccountNumber(
      String accountNumber, UpdateBankAccountRequest updateBankAccountRequest) {
    var user = getAuthenticatedUser();
    log.info("Updating account {} for user: {}", accountNumber, user.getId());
    var account =
        accountService.updateAccount(
            new AccountNumber(accountNumber), updateBankAccountRequest, user);
    log.info("Successfully updated account {} for user: {}", accountNumber, user.getId());
    return ResponseEntity.ok(accountMapper.toDto(account));
  }

  private User getAuthenticatedUser() {
    return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
  }
}
