package com.barclays.eagle_bank_api.service;

import com.barclays.eagle_bank_api.entity.User;
import com.barclays.eagle_bank_api.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

  private final AuthenticationManager authenticationManager;
  private final UserRepository userRepository;

  public AuthService(AuthenticationManager authenticationManager, UserRepository userRepository) {
    this.authenticationManager = authenticationManager;
    this.userRepository = userRepository;
  }

  public User login(String email, String password) {
    authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, password));

    return userRepository.findByEmail(email).orElseThrow();
  }
}
