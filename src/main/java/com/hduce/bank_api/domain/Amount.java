package com.hduce.bank_api.domain;

import com.hduce.bank_api.exception.MissMatchedCurrencyException;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Embeddable
public record Amount(double value, @Enumerated(EnumType.STRING) Currency currency) {

  private static final double MIN_VALUE = 0.00;
  private static final int DECIMAL_PLACES = 2;

  public Amount {
    value = roundToTwoDecimalPlaces(value);

    if (value < MIN_VALUE) {
      throw new IllegalArgumentException("Amount must be greater than or equal to " + MIN_VALUE);
    }
  }

  public static Amount gbp(double value) {
    return new Amount(value, Currency.GBP);
  }

  public static Amount zero() {
    return new Amount(0.00, Currency.GBP);
  }

  public Amount add(Amount other) {
    validateSameCurrency(other);
    return new Amount(this.value + other.value, this.currency);
  }

  public Amount subtract(Amount other) {
    validateSameCurrency(other);
    return new Amount(this.value - other.value, this.currency);
  }

  public boolean isGreaterThan(Amount other) {
    validateSameCurrency(other);
    return this.value > other.value;
  }

  private void validateSameCurrency(Amount other) {
    if (!this.currency.equals(other.currency)) {
      throw new MissMatchedCurrencyException(this.currency, other.currency);
    }
  }

  private static double roundToTwoDecimalPlaces(double value) {
    return BigDecimal.valueOf(value).setScale(DECIMAL_PLACES, RoundingMode.HALF_UP).doubleValue();
  }
}
