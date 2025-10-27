package com.barclays.eagle_bank_api.service;

import static com.barclays.eagle_bank_api.domain.AccountConstants.MAX_ACCOUNT_BALANCE;

import com.barclays.eagle_bank_api.domain.AccountNumber;
import com.barclays.eagle_bank_api.domain.Amount;
import com.barclays.eagle_bank_api.domain.Currency;
import com.barclays.eagle_bank_api.domain.TransactionType;
import com.barclays.eagle_bank_api.entity.Account;
import com.barclays.eagle_bank_api.entity.Transaction;
import com.barclays.eagle_bank_api.entity.User;
import com.barclays.eagle_bank_api.exception.InsufficientFundsException;
import com.barclays.eagle_bank_api.exception.MaximumBalanceExceededException;
import com.barclays.eagle_bank_api.model.CreateTransactionRequest;
import com.barclays.eagle_bank_api.repository.AccountRepository;
import com.barclays.eagle_bank_api.repository.TransactionRepository;
import jakarta.persistence.OptimisticLockException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransactionService {

  private final TransactionRepository transactionRepository;
  private final AccountRepository accountRepository;
  private final AccountService accountService;

  public TransactionService(
      TransactionRepository transactionRepository,
      AccountRepository accountRepository,
      AccountService accountService) {
    this.transactionRepository = transactionRepository;
    this.accountRepository = accountRepository;
    this.accountService = accountService;
  }

  @Transactional
  @Retryable(retryFor = OptimisticLockException.class, backoff = @Backoff(delay = 100))
  public Transaction createTransaction(
      AccountNumber accountNumber, CreateTransactionRequest request, User user) {
    var account = accountService.getAccountByAccountNumber(accountNumber, user);

    var amount = new Amount(request.getAmount(), mapCurrency(request.getCurrency()));
    var transactionType = mapTransactionType(request.getType());

    switch (transactionType) {
      case DEPOSIT -> deposit(account, amount);
      case WITHDRAWAL -> withdraw(account, amount);
    }

    accountRepository.save(account);

    var transaction =
        Transaction.builder()
            .account(account)
            .type(transactionType)
            .amount(amount)
            .reference(request.getReference())
            .userId(user.getId())
            .build();

    return transactionRepository.save(transaction);
  }

  private void deposit(Account account, Amount amount) {
    var newBalance = account.getBalance().add(amount);
    if (newBalance.isGreaterThan(MAX_ACCOUNT_BALANCE)) {
      throw new MaximumBalanceExceededException(
          account.getAccountNumber(), amount, account.getBalance(), MAX_ACCOUNT_BALANCE);
    }
    account.setBalance(newBalance);
  }

  private void withdraw(Account account, Amount amount) {
    if (amount.isGreaterThan(account.getBalance())) {
      throw new InsufficientFundsException(
          account.getAccountNumber(), amount, account.getBalance());
    }
    account.setBalance(account.getBalance().subtract(amount));
  }

  private Currency mapCurrency(CreateTransactionRequest.CurrencyEnum currency) {
    return switch (currency) {
      case GBP -> Currency.GBP;
    };
  }

  private TransactionType mapTransactionType(CreateTransactionRequest.TypeEnum type) {
    return switch (type) {
      case DEPOSIT -> TransactionType.DEPOSIT;
      case WITHDRAWAL -> TransactionType.WITHDRAWAL;
    };
  }
}
