package com.barclays.eagle_bank_api.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "accounts")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Account {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(name = "id", nullable = false)
  private UUID id;

  @Column(name = "account_number", nullable = false, length = 8, unique = true)
  @Convert(converter = AccountNumberConverter.class)
  private AccountNumber accountNumber;

  @Column(name = "sort_code", nullable = false)
  @Builder.Default
  private String sortCode = SortCode.DEFAULT;

  @Column(name = "name", nullable = false, length = 100)
  private String name;

  @Enumerated(EnumType.STRING)
  @Column(name = "account_type", nullable = false)
  private AccountType accountType;

  @Embedded
  @AttributeOverrides({
    @AttributeOverride(name = "value", column = @Column(name = "balance", nullable = false)),
    @AttributeOverride(name = "currency", column = @Column(name = "currency", nullable = false))
  })
  @Builder.Default
  private Amount balance = Amount.zero();

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @CreationTimestamp
  @Column(name = "created_timestamp", nullable = false, updatable = false)
  private OffsetDateTime createdTimestamp;

  @UpdateTimestamp
  @Column(name = "updated_timestamp", nullable = false)
  private OffsetDateTime updatedTimestamp;
}
