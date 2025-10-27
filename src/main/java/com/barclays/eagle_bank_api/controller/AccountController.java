package com.barclays.eagle_bank_api.controller;

import com.barclays.eagle_bank_api.api.AccountApi;
import com.barclays.eagle_bank_api.domain.AccountNumber;
import com.barclays.eagle_bank_api.entity.User;
import com.barclays.eagle_bank_api.mapper.AccountDtoMapper;
import com.barclays.eagle_bank_api.model.BankAccountResponse;
import com.barclays.eagle_bank_api.model.CreateBankAccountRequest;
import com.barclays.eagle_bank_api.model.ListBankAccountsResponse;
import com.barclays.eagle_bank_api.model.UpdateBankAccountRequest;
import com.barclays.eagle_bank_api.service.AccountService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;

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
    var account = accountService.createAccount(createBankAccountRequest, user);
    return new ResponseEntity<>(accountMapper.toDto(account), HttpStatus.CREATED);
  }

  @Override
  public ResponseEntity<Void> deleteAccountByAccountNumber(String accountNumber) {
    var user = getAuthenticatedUser();
    accountService.deleteAccount(new AccountNumber(accountNumber), user);
    return ResponseEntity.noContent().build();
  }

  @Override
  public ResponseEntity<BankAccountResponse> fetchAccountByAccountNumber(String accountNumber) {
    var user = getAuthenticatedUser();
    var account = accountService.getAccountByAccountNumber(new AccountNumber(accountNumber), user);
    return ResponseEntity.ok(accountMapper.toDto(account));
  }

  @Override
  public ResponseEntity<ListBankAccountsResponse> listAccounts() {
    var user = getAuthenticatedUser();
    var accounts = accountService.listAccountsForUser(user);
    var accountResponses = accounts.stream().map(accountMapper::toDto).toList();

    var response = new ListBankAccountsResponse();
    response.setAccounts(accountResponses);

    return ResponseEntity.ok(response);
  }

  @Override
  public ResponseEntity<BankAccountResponse> updateAccountByAccountNumber(
      String accountNumber, UpdateBankAccountRequest updateBankAccountRequest) {
    var user = getAuthenticatedUser();
    var account =
        accountService.updateAccount(
            new AccountNumber(accountNumber), updateBankAccountRequest, user);
    return ResponseEntity.ok(accountMapper.toDto(account));
  }

  private User getAuthenticatedUser() {
    return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
  }
}
