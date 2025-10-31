package com.hduce.eagle_bank_api.exception;

public class UserAlreadyExistsException extends RuntimeException {
  public UserAlreadyExistsException(String email) {
    super("User with email " + email + " already exists");
  }
}
