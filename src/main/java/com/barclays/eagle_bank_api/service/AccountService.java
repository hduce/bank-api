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
import com.barclays.eagle_bank_api.model.UpdateBankAccountRequest;
import com.barclays.eagle_bank_api.repository.AccountRepository;
import java.util.List;
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
            .accountType(mapCreateAccountType(request.getAccountType()))
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

  public List<Account> listAccountsForUser(User user) {
    return accountRepository.findByUserId(user.getId());
  }

  @Transactional
  public Account updateAccount(
      AccountNumber accountNumber, UpdateBankAccountRequest request, User user) {
    var account = getAccountByAccountNumber(accountNumber, user);

    if (request.getName() != null) {
      account.setName(request.getName());
    }

    if (request.getAccountType() != null) {
      account.setAccountType(mapAccountType(request.getAccountType()));
    }

    return accountRepository.save(account);
  }

  private AccountType mapCreateAccountType(CreateBankAccountRequest.AccountTypeEnum accountType) {
    return switch (accountType) {
      case PERSONAL -> AccountType.PERSONAL;
    };
  }

  private AccountType mapAccountType(UpdateBankAccountRequest.AccountTypeEnum accountType) {
    return switch (accountType) {
      case PERSONAL -> AccountType.PERSONAL;
    };
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
