package com.hduce.bank_api.exception;

import com.hduce.bank_api.domain.AccountNumber;
import com.hduce.bank_api.domain.Amount;

public class MaximumBalanceExceededException extends RuntimeException {
  public MaximumBalanceExceededException(
      AccountNumber accountNumber, Amount depositAmount, Amount currentBalance, Amount maxBalance) {
    super(
        "Deposit would exceed maximum balance for account "
            + accountNumber.value()
            + ". Current balance: "
            + currentBalance
            + ", deposit amount: "
            + depositAmount
            + ", maximum allowed: "
            + maxBalance);
  }
}
