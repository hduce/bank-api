package com.hduce.eagle_bank_api.exception;

import com.hduce.eagle_bank_api.domain.AccountNumber;

public class AccountNotFoundException extends RuntimeException {
  public AccountNotFoundException(AccountNumber accountNumber, String userId) {
    super("Account with account number " + accountNumber.value() + " not found for user " + userId);
  }
}
