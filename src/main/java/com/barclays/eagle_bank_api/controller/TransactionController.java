package com.barclays.eagle_bank_api.controller;

import com.barclays.eagle_bank_api.api.TransactionApi;
import com.barclays.eagle_bank_api.domain.AccountNumber;
import com.barclays.eagle_bank_api.entity.User;
import com.barclays.eagle_bank_api.mapper.TransactionDtoMapper;
import com.barclays.eagle_bank_api.model.CreateTransactionRequest;
import com.barclays.eagle_bank_api.model.ListTransactionsResponse;
import com.barclays.eagle_bank_api.model.TransactionResponse;
import com.barclays.eagle_bank_api.service.TransactionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class TransactionController implements TransactionApi {

  private final TransactionService transactionService;
  private final TransactionDtoMapper transactionMapper;

  public TransactionController(
      TransactionService transactionService, TransactionDtoMapper transactionMapper) {
    this.transactionService = transactionService;
    this.transactionMapper = transactionMapper;
  }

  @Override
  public ResponseEntity<TransactionResponse> createTransaction(
      String accountNumber, CreateTransactionRequest createTransactionRequest) {
    var user = getAuthenticatedUser();
    log.info(
        "Creating {} transaction of amount {} for account {} by user: {}",
        createTransactionRequest.getType(),
        createTransactionRequest.getAmount(),
        accountNumber,
        user.getId());
    var transaction =
        transactionService.createTransaction(
            new AccountNumber(accountNumber), createTransactionRequest, user);
    log.info(
        "Successfully created transaction {} for account {} by user: {}",
        transaction.getId(),
        accountNumber,
        user.getId());
    return ResponseEntity.status(201).body(transactionMapper.toDto(transaction));
  }

  @Override
  public ResponseEntity<TransactionResponse> fetchAccountTransactionByID(
      String accountNumber, String transactionId) {
    var user = getAuthenticatedUser();
    log.debug(
        "Fetching transaction {} for account {} by user: {}",
        transactionId,
        accountNumber,
        user.getId());
    var transaction =
        transactionService.getTransactionById(
            new AccountNumber(accountNumber), transactionId, user);
    return ResponseEntity.ok(transactionMapper.toDto(transaction));
  }

  @Override
  public ResponseEntity<ListTransactionsResponse> listAccountTransaction(String accountNumber) {
    var user = getAuthenticatedUser();
    log.debug("Listing transactions for account {} by user: {}", accountNumber, user.getId());
    var transactions = transactionService.listTransactions(new AccountNumber(accountNumber), user);
    log.debug(
        "Found {} transactions for account {} by user: {}",
        transactions.size(),
        accountNumber,
        user.getId());
    var response =
        new ListTransactionsResponse()
            .transactions(transactions.stream().map(transactionMapper::toDto).toList());
    return ResponseEntity.ok(response);
  }

  private User getAuthenticatedUser() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    return (User) authentication.getPrincipal();
  }
}
