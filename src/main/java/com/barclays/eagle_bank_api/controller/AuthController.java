package com.barclays.eagle_bank_api.controller;

import com.barclays.eagle_bank_api.api.AuthApi;
import com.barclays.eagle_bank_api.model.LoginRequest;
import com.barclays.eagle_bank_api.model.LoginResponse;
import com.barclays.eagle_bank_api.service.AuthService;
import com.barclays.eagle_bank_api.service.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController implements AuthApi {

  private final AuthService authService;
  private final JwtService jwtService;

  public AuthController(AuthService authService, JwtService jwtService) {
    this.authService = authService;
    this.jwtService = jwtService;
  }

  @Override
  public ResponseEntity<LoginResponse> login(LoginRequest loginRequest) {
    final var user = authService.login(loginRequest.getEmail(), loginRequest.getPassword());

    final var token = jwtService.generateToken(user);
    return new ResponseEntity<>(new LoginResponse(token), HttpStatus.OK);
  }
}
