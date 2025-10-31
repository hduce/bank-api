package com.hduce.bank_api.exception;

import com.hduce.bank_api.domain.AccountNumber;

public class AccountAccessForbiddenException extends RuntimeException {
  public AccountAccessForbiddenException(AccountNumber accountNumber, String userId) {
    super("User " + userId + " is not authorized to access account " + accountNumber.value());
  }
}
