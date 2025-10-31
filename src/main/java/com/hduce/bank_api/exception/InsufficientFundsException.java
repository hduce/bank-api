package com.hduce.bank_api.exception;

import com.hduce.bank_api.domain.AccountNumber;
import com.hduce.bank_api.domain.Amount;

public class InsufficientFundsException extends RuntimeException {
  public InsufficientFundsException(AccountNumber accountNumber, Amount amount, Amount balance) {
    super(
        "Insufficient funds in account "
            + accountNumber.value()
            + ". Current balance: "
            + balance
            + ", withdrawal amount: "
            + amount);
  }
}
