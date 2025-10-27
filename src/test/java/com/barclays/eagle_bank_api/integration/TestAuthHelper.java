package com.barclays.eagle_bank_api.integration;

import com.barclays.eagle_bank_api.domain.Address;
import com.barclays.eagle_bank_api.entity.User;
import com.barclays.eagle_bank_api.repository.UserRepository;
import com.barclays.eagle_bank_api.security.JwtProvider;
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
    return userRepository.save(
        User.builder()
            .name("Test User")
            .email(email)
            .password(passwordEncoder.encode("Password123!"))
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
