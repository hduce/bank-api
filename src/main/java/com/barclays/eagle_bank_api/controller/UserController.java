package com.barclays.eagle_bank_api.controller;

import com.barclays.eagle_bank_api.api.UserApi;
import com.barclays.eagle_bank_api.entity.Address;
import com.barclays.eagle_bank_api.entity.User;
import com.barclays.eagle_bank_api.model.CreateUserRequest;
import com.barclays.eagle_bank_api.model.CreateUserRequestAddress;
import com.barclays.eagle_bank_api.model.UpdateUserRequest;
import com.barclays.eagle_bank_api.model.UserResponse;
import com.barclays.eagle_bank_api.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class UserController implements UserApi {

  private final AuthService authService;

  public UserController(AuthService authService) {
    this.authService = authService;
  }

  @Override
  public ResponseEntity<UserResponse> createUser(CreateUserRequest createUserRequest) {
    var user = authService.register(createUserRequest);
    return new ResponseEntity<>(toDto(user), HttpStatus.CREATED);
  }

  @Override
  public ResponseEntity<Void> deleteUserByID(String userId) {
    throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED, "Not implemented");
  }

  @Override
  public ResponseEntity<UserResponse> fetchUserByID(String userId) {
    throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED, "Not implemented");
  }

  @Override
  public ResponseEntity<UserResponse> updateUserByID(
      String userId, UpdateUserRequest updateUserRequest) {
    throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED, "Not implemented");
  }

  private UserResponse toDto(User user) {
    return new UserResponse()
        .id(user.getId())
        .name(user.getName())
        .address(toAddressDto(user.getAddress()))
        .phoneNumber(user.getPhoneNumber())
        .email(user.getEmail())
        .createdTimestamp(user.getCreatedTimestamp())
        .updatedTimestamp(user.getUpdatedTimestamp());
  }

  private CreateUserRequestAddress toAddressDto(Address address) {
    return new CreateUserRequestAddress()
        .line1(address.line1())
        .line2(address.line2())
        .line3(address.line3())
        .town(address.town())
        .county(address.county())
        .postcode(address.postcode());
  }
}
