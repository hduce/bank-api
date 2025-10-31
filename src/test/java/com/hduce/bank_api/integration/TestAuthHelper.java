package com.hduce.bank_api.integration;

import com.hduce.bank_api.domain.Address;
import com.hduce.bank_api.entity.User;
import com.hduce.bank_api.repository.UserRepository;
import com.hduce.bank_api.security.JwtProvider;
import org.springframework.http.HttpHeaders;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class TestAuthHelper {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtProvider jwtProvider;

  public TestAuthHelper(
      UserRepository userRepository, PasswordEncoder passwordEncoder, JwtProvider jwtProvider) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.jwtProvider = jwtProvider;
  }

  public User createAndSaveUser(String email) {
    return createAndSaveUser(email, "Password123!");
  }

  public User createAndSaveUser(String email, String password) {
    return userRepository.save(
        User.builder()
            .name("Test User")
            .email(email)
            .password(passwordEncoder.encode(password))
            .phoneNumber("07988220214")
            .address(new Address("line1", "line2", "line3", "town", "county", "postcode"))
            .build());
  }

  public HttpHeaders createAuthHeaders(User user) {
    HttpHeaders headers = new HttpHeaders();
    String token = jwtProvider.generateToken(user);
    headers.setBearerAuth(token);
    return headers;
  }
}
