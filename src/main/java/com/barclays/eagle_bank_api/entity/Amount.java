package com.barclays.eagle_bank_api.entity;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Embeddable
public record Amount(double value, @Enumerated(EnumType.STRING) Currency currency) {

  private static final double MIN_VALUE = 0.00;
  private static final double MAX_VALUE = 10000.00;
  private static final int DECIMAL_PLACES = 2;

  public Amount {
    if (value < MIN_VALUE || value > MAX_VALUE) {
      throw new IllegalArgumentException(
          "Amount must be between " + MIN_VALUE + " and " + MAX_VALUE + ", got: " + value);
    }

    value = roundToTwoDecimalPlaces(value);
  }

  public static Amount gbp(double value) {
    return new Amount(value, Currency.GBP);
  }

  public static Amount zero() {
    return new Amount(0.00, Currency.GBP);
  }

  public Amount add(Amount other) {
    if (!this.currency.equals(other.currency)) {
      throw new IllegalArgumentException(
          "Cannot add amounts with different currencies: "
              + this.currency
              + " and "
              + other.currency);
    }
    return new Amount(this.value + other.value, this.currency);
  }

  public Amount subtract(Amount other) {
    if (!this.currency.equals(other.currency)) {
      throw new IllegalArgumentException(
          "Cannot subtract amounts with different currencies: "
              + this.currency
              + " and "
              + other.currency);
    }
    return new Amount(this.value - other.value, this.currency);
  }

  public boolean isGreaterThanOrEqualTo(Amount other) {
    if (!this.currency.equals(other.currency)) {
      throw new IllegalArgumentException(
          "Cannot compare amounts with different currencies: "
              + this.currency
              + " and "
              + other.currency);
    }
    return this.value >= other.value;
  }

  private static double roundToTwoDecimalPlaces(double value) {
    return BigDecimal.valueOf(value).setScale(DECIMAL_PLACES, RoundingMode.HALF_UP).doubleValue();
  }
}
