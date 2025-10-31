package com.hduce.eagle_bank_api.controller;

import com.hduce.eagle_bank_api.api.AuthApi;
import com.hduce.eagle_bank_api.model.LoginRequest;
import com.hduce.eagle_bank_api.model.LoginResponse;
import com.hduce.eagle_bank_api.security.JwtProvider;
import com.hduce.eagle_bank_api.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController implements AuthApi {

  private final AuthService authService;
  private final JwtProvider jwtProvider;

  public AuthController(AuthService authService, JwtProvider jwtProvider) {
    this.authService = authService;
    this.jwtProvider = jwtProvider;
  }

  @Override
  public ResponseEntity<LoginResponse> login(LoginRequest loginRequest) {
    final var user = authService.login(loginRequest.getEmail(), loginRequest.getPassword());

    final var token = jwtProvider.generateToken(user);
    final var response = new LoginResponse(token, user.getId());
    return new ResponseEntity<>(response, HttpStatus.OK);
  }
}
