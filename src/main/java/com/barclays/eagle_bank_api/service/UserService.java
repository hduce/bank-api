package com.barclays.eagle_bank_api.service;

import com.barclays.eagle_bank_api.entity.User;
import com.barclays.eagle_bank_api.exception.UserEmailAlreadyExistsException;
import com.barclays.eagle_bank_api.exception.UserNotFoundException;
import com.barclays.eagle_bank_api.mapper.AddressMapper;
import com.barclays.eagle_bank_api.model.UpdateUserRequest;
import com.barclays.eagle_bank_api.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

  private final UserRepository userRepository;
  private final AddressMapper addressMapper;

  public UserService(UserRepository userRepository, AddressMapper addressMapper) {
    this.userRepository = userRepository;
    this.addressMapper = addressMapper;
  }

  public User getUserById(String userId) {
    return userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
  }

  @Transactional
  public User updateUser(String userId, UpdateUserRequest updateRequest) {
    var user = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));

    if (updateRequest.getName() != null) {
      user.setName(updateRequest.getName());
    }

    if (updateRequest.getEmail() != null) {
      if (userRepository.existsByEmail(updateRequest.getEmail())) {
        throw new UserEmailAlreadyExistsException(updateRequest.getEmail());
      }
      user.setEmail(updateRequest.getEmail());
    }

    if (updateRequest.getPhoneNumber() != null) {
      user.setPhoneNumber(updateRequest.getPhoneNumber());
    }

    if (updateRequest.getAddress() != null) {
      user.setAddress(addressMapper.toEntity(updateRequest.getAddress()));
    }

    return userRepository.save(user);
  }
}
