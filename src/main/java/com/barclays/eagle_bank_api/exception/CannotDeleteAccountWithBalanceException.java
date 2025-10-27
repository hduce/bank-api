package com.barclays.eagle_bank_api.exception;

import com.barclays.eagle_bank_api.domain.AccountNumber;
import com.barclays.eagle_bank_api.domain.Amount;

public class CannotDeleteAccountWithBalanceException extends RuntimeException {
  public CannotDeleteAccountWithBalanceException(AccountNumber accountNumber, Amount balance) {
    super(
        "Cannot delete account "
            + accountNumber.value()
            + " because it has a balance of "
            + balance
            + ". Please withdraw all funds before deleting the account.");
  }
}
