package com.barclays.eagle_bank_api.entity;

import com.barclays.eagle_bank_api.domain.Amount;
import com.barclays.eagle_bank_api.domain.TransactionType;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "transactions")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {

  @Id
  @Column(name = "id", nullable = false)
  private String id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "account_number", referencedColumnName = "account_number", nullable = false)
  private Account account;

  @Enumerated(EnumType.STRING)
  @Column(name = "type", nullable = false)
  private TransactionType type;

  @Embedded
  @AttributeOverrides({
    @AttributeOverride(name = "value", column = @Column(name = "amount", nullable = false)),
    @AttributeOverride(name = "currency", column = @Column(name = "currency", nullable = false))
  })
  private Amount amount;

  @Column(name = "reference")
  private String reference;

  @CreationTimestamp
  @Column(name = "created_timestamp", nullable = false, updatable = false)
  private OffsetDateTime createdTimestamp;

  @PrePersist
  private void generateId() {
    if (this.id == null) {
      this.id = "tan-" + UUID.randomUUID().toString().replace("-", "");
    }
  }
}
