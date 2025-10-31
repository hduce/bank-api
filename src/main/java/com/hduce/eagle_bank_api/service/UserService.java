package com.hduce.eagle_bank_api.service;

import com.hduce.eagle_bank_api.entity.User;
import com.hduce.eagle_bank_api.exception.CannotDeleteUserWithAccountsException;
import com.hduce.eagle_bank_api.exception.UserEmailAlreadyExistsException;
import com.hduce.eagle_bank_api.exception.UserNotFoundException;
import com.hduce.eagle_bank_api.mapper.AddressDtoMapper;
import com.hduce.eagle_bank_api.model.UpdateUserRequest;
import com.hduce.eagle_bank_api.repository.AccountRepository;
import com.hduce.eagle_bank_api.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

  private final UserRepository userRepository;
  private final AddressDtoMapper addressMapper;
  private final AccountRepository accountRepository;

  public UserService(
      UserRepository userRepository,
      AccountRepository accountRepository,
      AddressDtoMapper addressMapper) {
    this.userRepository = userRepository;
    this.accountRepository = accountRepository;
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

    if (emailIsUpdated(updateRequest, user)) {
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

  @Transactional
  public void deleteUser(String userId) {
    var user = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));

    if (accountRepository.existsByUserId(userId)) {
      throw new CannotDeleteUserWithAccountsException(userId);
    }

    userRepository.delete(user);
  }

  private static boolean emailIsUpdated(UpdateUserRequest updateRequest, User user) {
    return updateRequest.getEmail() != null && !updateRequest.getEmail().equals(user.getEmail());
  }
}
