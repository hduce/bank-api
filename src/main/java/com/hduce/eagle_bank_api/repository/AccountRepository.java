package com.hduce.eagle_bank_api.repository;

import com.hduce.eagle_bank_api.domain.AccountNumber;
import com.hduce.eagle_bank_api.entity.Account;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AccountRepository extends JpaRepository<Account, String> {

  List<Account> findByUserId(String userId);

  boolean existsByAccountNumber(AccountNumber accountNumber);

  Optional<Account> findByAccountNumber(AccountNumber accountNumber);

  boolean existsByUserId(String userId);
}
