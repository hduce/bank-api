package com.barclays.eagle_bank_api.controller;

import com.barclays.eagle_bank_api.api.UserApi;
import com.barclays.eagle_bank_api.entity.User;
import com.barclays.eagle_bank_api.mapper.AddressMapper;
import com.barclays.eagle_bank_api.model.CreateUserRequest;
import com.barclays.eagle_bank_api.model.UpdateUserRequest;
import com.barclays.eagle_bank_api.model.UserResponse;
import com.barclays.eagle_bank_api.service.AuthService;
import com.barclays.eagle_bank_api.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class UserController implements UserApi {

  private final AuthService authService;
  private final UserService userService;
  private final AddressMapper addressMapper;

  public UserController(
      AuthService authService, UserService userService, AddressMapper addressMapper) {
    this.authService = authService;
    this.userService = userService;
    this.addressMapper = addressMapper;
  }

  @Override
  public ResponseEntity<UserResponse> createUser(CreateUserRequest createUserRequest) {
    var user = authService.register(createUserRequest);
    return new ResponseEntity<>(toDto(user), HttpStatus.CREATED);
  }

  @Override
  @PreAuthorize("#userId == authentication.principal.id")
  public ResponseEntity<Void> deleteUserByID(String userId) {
    throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED, "Not implemented");
  }

  @Override
  @PreAuthorize("#userId == authentication.principal.id")
  public ResponseEntity<UserResponse> fetchUserByID(String userId) {
    var user = userService.getUserById(userId);

    return ResponseEntity.ok(toDto(user));
  }

  @Override
  @PreAuthorize("#userId == authentication.principal.id")
  public ResponseEntity<UserResponse> updateUserByID(
      String userId, UpdateUserRequest updateUserRequest) {
    var updatedUser = userService.updateUser(userId, updateUserRequest);
    return ResponseEntity.ok(toDto(updatedUser));
  }

  private UserResponse toDto(User user) {
    return new UserResponse()
        .id(user.getId())
        .name(user.getName())
        .address(addressMapper.toDto(user.getAddress()))
        .phoneNumber(user.getPhoneNumber())
        .email(user.getEmail())
        .createdTimestamp(user.getCreatedTimestamp())
        .updatedTimestamp(user.getUpdatedTimestamp());
  }
}
