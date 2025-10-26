package com.barclays.eagle_bank_api.exception;

import com.barclays.eagle_bank_api.entity.AccountNumber;

public class AccountAccessForbiddenException extends RuntimeException {
  public AccountAccessForbiddenException(AccountNumber accountNumber, String userId) {
    super("User " + userId + " is not authorized to access account " + accountNumber.value());
  }
}
