package com.oviro.repository;

import com.oviro.enums.TransactionType;
import com.oviro.model.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    Optional<Transaction> findByReference(String reference);

    Page<Transaction> findByWalletId(UUID walletId, Pageable pageable);

    Page<Transaction> findByWalletIdAndType(UUID walletId, TransactionType type, Pageable pageable);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.wallet.id = :walletId AND t.type = :type AND t.status = 'SUCCESS'")
    BigDecimal sumByWalletIdAndType(@Param("walletId") UUID walletId, @Param("type") TransactionType type);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.wallet.id = :walletId AND t.type = 'RECHARGE' AND t.status = 'SUCCESS' AND t.createdAt >= :since")
    BigDecimal sumDailyRecharges(@Param("walletId") UUID walletId, @Param("since") LocalDateTime since);

    boolean existsByReference(String reference);
}
