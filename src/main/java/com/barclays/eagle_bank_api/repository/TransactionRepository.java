package com.barclays.eagle_bank_api.repository;

import com.barclays.eagle_bank_api.domain.AccountNumber;
import com.barclays.eagle_bank_api.entity.Transaction;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, String> {

  List<Transaction> findByAccountAccountNumberOrderByCreatedTimestampAsc(
      AccountNumber accountNumber);
}
