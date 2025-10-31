package com.hduce.eagle_bank_api.exception;

public class AccountNumberGenerationException extends RuntimeException {
  public AccountNumberGenerationException(int attempts) {
    super(
        "Unlucky... Failed to generate unique account number after "
            + attempts
            + " attempts. Try again...");
  }
}
