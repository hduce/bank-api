package com.hduce.bank_api.exception;

import com.hduce.bank_api.domain.Currency;

public class MissMatchedCurrencyException extends RuntimeException {
  public MissMatchedCurrencyException(Currency first, Currency second) {
    super("Cannot compare amounts with different currencies: " + first + " and " + second);
  }
}
