package com.barclays.eagle_bank_api.exception;

import jakarta.validation.constraints.Email;

public class UserEmailAlreadyExistsException extends RuntimeException {
  public UserEmailAlreadyExistsException(@Email String email) {
    super("User with email " + email + " already exists");
  }
}
