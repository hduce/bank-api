package com.barclays.eagle_bank_api.service;

import com.barclays.eagle_bank_api.entity.Account;
import com.barclays.eagle_bank_api.entity.AccountNumber;
import com.barclays.eagle_bank_api.entity.AccountType;
import com.barclays.eagle_bank_api.entity.Currency;
import com.barclays.eagle_bank_api.entity.User;
import com.barclays.eagle_bank_api.exception.AccountAccessForbiddenException;
import com.barclays.eagle_bank_api.exception.AccountNotFoundException;
import com.barclays.eagle_bank_api.exception.AccountNumberGenerationException;
import com.barclays.eagle_bank_api.model.CreateBankAccountRequest;
import com.barclays.eagle_bank_api.repository.AccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService {

  private static final int MAX_ACCOUNT_NUMBER_GENERATION_ATTEMPTS = 10;

  private final AccountRepository accountRepository;

  public AccountService(AccountRepository accountRepository) {
    this.accountRepository = accountRepository;
  }

  @Transactional
  public Account createAccount(CreateBankAccountRequest request, User user) {
    var account =
        Account.builder()
            .accountNumber(generateUniqueAccountNumber())
            .name(request.getName())
            .accountType(AccountType.valueOf(request.getAccountType().name()))
            .balance(0.0)
            .currency(Currency.GBP)
            .user(user)
            .build();

    return accountRepository.save(account);
  }

  public Account getAccountByAccountNumber(AccountNumber accountNumber, User user) {
    var account =
        accountRepository
            .findByAccountNumber(accountNumber)
            .orElseThrow(() -> new AccountNotFoundException(accountNumber, user.getId()));

    if (!account.getUser().getId().equals(user.getId())) {
      throw new AccountAccessForbiddenException(accountNumber, user.getId());
    }

    return account;
  }

  private AccountNumber generateUniqueAccountNumber() {
    for (int attempt = 0; attempt < MAX_ACCOUNT_NUMBER_GENERATION_ATTEMPTS; attempt++) {
      AccountNumber accountNumber = AccountNumber.generateRandom();
      if (!accountRepository.existsByAccountNumber(accountNumber)) {
        return accountNumber;
      }
    }
    throw new AccountNumberGenerationException(MAX_ACCOUNT_NUMBER_GENERATION_ATTEMPTS);
  }
}
