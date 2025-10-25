package com.barclays.eagle_bank_api.service;

import com.barclays.eagle_bank_api.entity.Address;
import com.barclays.eagle_bank_api.entity.User;
import com.barclays.eagle_bank_api.exception.UserAlreadyExistsException;
import com.barclays.eagle_bank_api.model.CreateUserRequest;
import com.barclays.eagle_bank_api.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

  private final AuthenticationManager authenticationManager;
  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  public AuthService(
      AuthenticationManager authenticationManager,
      UserRepository userRepository,
      PasswordEncoder passwordEncoder) {
    this.authenticationManager = authenticationManager;
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
  }

  public User login(String email, String password) {
    authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, password));

    return userRepository.findByEmail(email).orElseThrow();
  }

  @Transactional
  public User register(CreateUserRequest createUserRequest) {
    if (userRepository.existsByEmail(createUserRequest.getEmail())) {
      throw new UserAlreadyExistsException(createUserRequest.getEmail());
    }

    var requestAddress = createUserRequest.getAddress();
    var address =
        new Address(
            requestAddress.getLine1(),
            requestAddress.getLine2(),
            requestAddress.getLine3(),
            requestAddress.getTown(),
            requestAddress.getCounty(),
            requestAddress.getPostcode());
    var user =
        User.builder()
            .name(createUserRequest.getName())
            .email(createUserRequest.getEmail())
            .password(passwordEncoder.encode(createUserRequest.getPassword()))
            .phoneNumber(createUserRequest.getPhoneNumber())
            .address(address)
            .build();

    return userRepository.save(user);
  }
}
