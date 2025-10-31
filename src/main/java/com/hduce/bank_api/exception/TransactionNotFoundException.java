package com.hduce.bank_api.exception;

import com.hduce.bank_api.domain.AccountNumber;

public class TransactionNotFoundException extends RuntimeException {
  public TransactionNotFoundException(String transactionId, AccountNumber accountNumber) {
    super(
        "Transaction with ID " + transactionId + " not found for account " + accountNumber.value());
  }
}
