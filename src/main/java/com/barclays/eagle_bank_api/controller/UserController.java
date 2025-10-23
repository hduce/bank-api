package com.barclays.eagle_bank_api.controller;

import com.barclays.eagle_bank_api.api.UserApi;
import com.barclays.eagle_bank_api.model.CreateUserRequest;
import com.barclays.eagle_bank_api.model.UpdateUserRequest;
import com.barclays.eagle_bank_api.model.UserResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class UserController implements UserApi {

  @Override
  public ResponseEntity<UserResponse> createUser(CreateUserRequest createUserRequest) {
    throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED, "Not implemented");
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
}
