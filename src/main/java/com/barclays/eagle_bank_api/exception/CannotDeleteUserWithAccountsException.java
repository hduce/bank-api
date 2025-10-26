package com.barclays.eagle_bank_api.exception;

public class CannotDeleteUserWithAccountsException extends RuntimeException {
  public CannotDeleteUserWithAccountsException(String userId) {
    super(
        "Cannot delete user "
            + userId
            + " because they have associated bank accounts. Please delete all accounts first.");
  }
}
