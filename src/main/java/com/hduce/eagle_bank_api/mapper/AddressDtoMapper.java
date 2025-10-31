package com.hduce.eagle_bank_api.mapper;

import com.hduce.eagle_bank_api.domain.Address;
import com.hduce.eagle_bank_api.model.CreateUserRequestAddress;
import org.springframework.stereotype.Component;

@Component
public class AddressDtoMapper {

  public Address toEntity(CreateUserRequestAddress dto) {
    return new Address(
        dto.getLine1(),
        dto.getLine2(),
        dto.getLine3(),
        dto.getTown(),
        dto.getCounty(),
        dto.getPostcode());
  }

  public CreateUserRequestAddress toDto(Address address) {
    return new CreateUserRequestAddress()
        .line1(address.line1())
        .line2(address.line2())
        .line3(address.line3())
        .town(address.town())
        .county(address.county())
        .postcode(address.postcode());
  }
}
