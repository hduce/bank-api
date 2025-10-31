package com.hduce.eagle_bank_api.entity;

import com.hduce.eagle_bank_api.domain.AccountNumber;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class AccountNumberConverter implements AttributeConverter<AccountNumber, String> {

  @Override
  public String convertToDatabaseColumn(AccountNumber accountNumber) {
    if (accountNumber == null) {
      throw new IllegalStateException("AccountNumber cannot be null");
    }
    return accountNumber.value();
  }

  @Override
  public AccountNumber convertToEntityAttribute(String dbData) {
    if (dbData == null) {
      throw new IllegalStateException("Account number in database cannot be null");
    }
    return new AccountNumber(dbData);
  }
}
