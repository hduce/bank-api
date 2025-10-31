package com.hduce.bank_api.mapper;

import com.hduce.bank_api.domain.Currency;
import com.hduce.bank_api.domain.TransactionType;
import com.hduce.bank_api.entity.Transaction;
import com.hduce.bank_api.model.TransactionResponse;
import org.springframework.stereotype.Component;

@Component
public class TransactionDtoMapper {

  public TransactionResponse toDto(Transaction transaction) {
    return new TransactionResponse()
        .id(transaction.getId())
        .amount(transaction.getAmount().value())
        .currency(mapCurrency(transaction.getAmount().currency()))
        .type(mapTransactionType(transaction.getType()))
        .reference(transaction.getReference())
        .userId(transaction.getUserId())
        .createdTimestamp(transaction.getCreatedTimestamp());
  }

  private TransactionResponse.CurrencyEnum mapCurrency(Currency currency) {
    return switch (currency) {
      case GBP -> TransactionResponse.CurrencyEnum.GBP;
    };
  }

  private TransactionResponse.TypeEnum mapTransactionType(TransactionType type) {
    return switch (type) {
      case DEPOSIT -> TransactionResponse.TypeEnum.DEPOSIT;
      case WITHDRAWAL -> TransactionResponse.TypeEnum.WITHDRAWAL;
    };
  }
}
