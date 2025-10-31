package com.hduce.bank_api.exception;

public class UserNotFoundException extends RuntimeException {
  public UserNotFoundException(String userId) {
    super("User with ID " + userId + " not found");
  }
}
