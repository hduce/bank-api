package com.barclays.eagle_bank_api.domain;

import java.security.SecureRandom;

public record AccountNumber(String value) {

  private static final SecureRandom RANDOM = new SecureRandom();

  public AccountNumber {
    if (value == null || value.isEmpty()) {
      throw new IllegalArgumentException("Account number cannot be null or empty");
    }
    if (!value.matches("^01\\d{6}$")) {
      throw new IllegalArgumentException(
          "Account number must start with '01' followed by 6 digits, got: " + value);
    }
  }

  public static AccountNumber generateRandom() {
    int sixDigits = RANDOM.nextInt(900000) + 100000;
    return new AccountNumber("01" + sixDigits);
  }
}
