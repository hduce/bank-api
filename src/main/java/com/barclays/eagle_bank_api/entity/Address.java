package com.barclays.eagle_bank_api.entity;

import jakarta.persistence.Embeddable;

@Embeddable
public record Address(
    String line1,
    String line2,
    String line3,
    String town,
    String county,
    String postcode
) {
}
