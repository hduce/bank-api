package com.barclays.eagle_bank_api.exception;

import com.barclays.eagle_bank_api.entity.AccountNumber;

public class AccountNotFoundException extends RuntimeException {
  public AccountNotFoundException(AccountNumber accountNumber, String userId) {
    super("Account with account number " + accountNumber.value() + " not found for user " + userId);
  }
}
