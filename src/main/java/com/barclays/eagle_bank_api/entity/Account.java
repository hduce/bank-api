package com.barclays.eagle_bank_api.entity;

import com.barclays.eagle_bank_api.domain.AccountNumber;
import com.barclays.eagle_bank_api.domain.AccountType;
import com.barclays.eagle_bank_api.domain.Amount;
import com.barclays.eagle_bank_api.domain.SortCode;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
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
@Table(
    name = "accounts",
    indexes = {@Index(name = "idx_accounts_user_id", columnList = "user_id")})
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

  @Version
  @Column(name = "version", nullable = false)
  private Long version;
}
