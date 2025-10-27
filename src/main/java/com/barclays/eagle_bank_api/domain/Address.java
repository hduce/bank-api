package com.barclays.eagle_bank_api.domain;

import jakarta.persistence.Embeddable;
import java.io.Serializable;

@Embeddable
public record Address(
    String line1, String line2, String line3, String town, String county, String postcode)
    implements Serializable {}
