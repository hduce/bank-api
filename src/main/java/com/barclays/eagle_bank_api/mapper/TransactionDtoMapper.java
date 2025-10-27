package com.barclays.eagle_bank_api.mapper;

import com.barclays.eagle_bank_api.domain.Currency;
import com.barclays.eagle_bank_api.domain.TransactionType;
import com.barclays.eagle_bank_api.entity.Transaction;
import com.barclays.eagle_bank_api.model.TransactionResponse;
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
