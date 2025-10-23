package com.barclays.eagle_bank_api.controller;

import com.barclays.eagle_bank_api.api.TransactionApi;
import com.barclays.eagle_bank_api.model.CreateTransactionRequest;
import com.barclays.eagle_bank_api.model.ListTransactionsResponse;
import com.barclays.eagle_bank_api.model.TransactionResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class TransactionController implements TransactionApi {

  @Override
  public ResponseEntity<TransactionResponse> createTransaction(
      String accountNumber, CreateTransactionRequest createTransactionRequest) {
    throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED, "Not implemented");
  }

  @Override
  public ResponseEntity<TransactionResponse> fetchAccountTransactionByID(
      String accountNumber, String transactionId) {
    throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED, "Not implemented");
  }

  @Override
  public ResponseEntity<ListTransactionsResponse> listAccountTransaction(String accountNumber) {
    throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED, "Not implemented");
  }
}
