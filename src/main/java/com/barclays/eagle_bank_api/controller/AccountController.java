package com.barclays.eagle_bank_api.controller;

import com.barclays.eagle_bank_api.api.AccountApi;
import com.barclays.eagle_bank_api.entity.User;
import com.barclays.eagle_bank_api.mapper.AccountMapper;
import com.barclays.eagle_bank_api.model.BankAccountResponse;
import com.barclays.eagle_bank_api.model.CreateBankAccountRequest;
import com.barclays.eagle_bank_api.model.ListBankAccountsResponse;
import com.barclays.eagle_bank_api.model.UpdateBankAccountRequest;
import com.barclays.eagle_bank_api.service.AccountService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class AccountController implements AccountApi {

  private final AccountService accountService;
  private final AccountMapper accountMapper;

  public AccountController(AccountService accountService, AccountMapper accountMapper) {
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
    throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED, "Not implemented");
  }

  @Override
  public ResponseEntity<BankAccountResponse> fetchAccountByAccountNumber(String accountNumber) {
    throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED, "Not implemented");
  }

  @Override
  public ResponseEntity<ListBankAccountsResponse> listAccounts() {
    throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED, "Not implemented");
  }

  @Override
  public ResponseEntity<BankAccountResponse> updateAccountByAccountNumber(
      String accountNumber, UpdateBankAccountRequest updateBankAccountRequest) {
    throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED, "Not implemented");
  }

  private User getAuthenticatedUser() {
    return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
  }
}
