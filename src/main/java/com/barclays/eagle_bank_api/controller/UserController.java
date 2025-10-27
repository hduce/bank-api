package com.barclays.eagle_bank_api.controller;

import com.barclays.eagle_bank_api.api.UserApi;
import com.barclays.eagle_bank_api.entity.User;
import com.barclays.eagle_bank_api.mapper.AddressDtoMapper;
import com.barclays.eagle_bank_api.model.CreateUserRequest;
import com.barclays.eagle_bank_api.model.UpdateUserRequest;
import com.barclays.eagle_bank_api.model.UserResponse;
import com.barclays.eagle_bank_api.service.AuthService;
import com.barclays.eagle_bank_api.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class UserController implements UserApi {

  private final AuthService authService;
  private final UserService userService;
  private final AddressDtoMapper addressMapper;

  public UserController(
      AuthService authService, UserService userService, AddressDtoMapper addressMapper) {
    this.authService = authService;
    this.userService = userService;
    this.addressMapper = addressMapper;
  }

  @Override
  public ResponseEntity<UserResponse> createUser(CreateUserRequest createUserRequest) {
    log.info("Creating new user with email: {}", createUserRequest.getEmail());
    var user = authService.register(createUserRequest);
    log.info("Successfully created user with ID: {}", user.getId());
    return new ResponseEntity<>(toDto(user), HttpStatus.CREATED);
  }

  @Override
  @PreAuthorize("#userId == authentication.principal.id")
  public ResponseEntity<Void> deleteUserByID(String userId) {
    log.info("Deleting user with ID: {}", userId);
    userService.deleteUser(userId);
    log.info("Successfully deleted user with ID: {}", userId);
    return ResponseEntity.noContent().build();
  }

  @Override
  @PreAuthorize("#userId == authentication.principal.id")
  public ResponseEntity<UserResponse> fetchUserByID(String userId) {
    log.debug("Fetching user with ID: {}", userId);
    var user = userService.getUserById(userId);
    log.debug("Successfully fetched user with ID: {}", userId);
    return ResponseEntity.ok(toDto(user));
  }

  @Override
  @PreAuthorize("#userId == authentication.principal.id")
  public ResponseEntity<UserResponse> updateUserByID(
      String userId, UpdateUserRequest updateUserRequest) {
    log.info("Updating user with ID: {}", userId);
    var updatedUser = userService.updateUser(userId, updateUserRequest);
    log.info("Successfully updated user with ID: {}", userId);
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
