package com.barclays.eagle_bank_api.mapper;

import com.barclays.eagle_bank_api.domain.AccountType;
import com.barclays.eagle_bank_api.domain.Currency;
import com.barclays.eagle_bank_api.domain.SortCode;
import com.barclays.eagle_bank_api.entity.Account;
import com.barclays.eagle_bank_api.model.BankAccountResponse;
import org.springframework.stereotype.Component;

@Component
public class AccountDtoMapper {

  public BankAccountResponse toDto(Account account) {
    return new BankAccountResponse()
        .accountNumber(account.getAccountNumber().value())
        .sortCode(mapSortCode(account.getSortCode()))
        .name(account.getName())
        .accountType(mapAccountType(account.getAccountType()))
        .balance(account.getBalance().value())
        .currency(mapCurrency(account.getBalance().currency()))
        .createdTimestamp(account.getCreatedTimestamp())
        .updatedTimestamp(account.getUpdatedTimestamp());
  }

  private BankAccountResponse.SortCodeEnum mapSortCode(String sortCode) {
    if (SortCode.DEFAULT.equals(sortCode)) {
      return BankAccountResponse.SortCodeEnum._10_10_10;
    }
    throw new IllegalArgumentException("Unknown sort code: " + sortCode);
  }

  private BankAccountResponse.AccountTypeEnum mapAccountType(AccountType accountType) {
    return switch (accountType) {
      case PERSONAL -> BankAccountResponse.AccountTypeEnum.PERSONAL;
    };
  }

  private BankAccountResponse.CurrencyEnum mapCurrency(Currency currency) {
    return switch (currency) {
      case GBP -> BankAccountResponse.CurrencyEnum.GBP;
    };
  }
}
