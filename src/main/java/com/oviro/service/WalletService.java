package com.oviro.service;

import com.oviro.dto.request.WalletRechargeRequest;
import com.oviro.dto.response.TransactionResponse;
import com.oviro.dto.response.WalletResponse;
import com.oviro.enums.TransactionStatus;
import com.oviro.enums.TransactionType;
import com.oviro.exception.BusinessException;
import com.oviro.exception.InsufficientBalanceException;
import com.oviro.exception.ResourceNotFoundException;
import com.oviro.model.Transaction;
import com.oviro.model.Wallet;
import com.oviro.repository.TransactionRepository;
import com.oviro.repository.WalletRepository;
import com.oviro.util.ReferenceGenerator;
import com.oviro.util.SecurityContextHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class WalletService {

    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final ReferenceGenerator referenceGenerator;
    private final SecurityContextHelper securityContextHelper;
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    public WalletResponse getMyWallet() {
        UUID userId = securityContextHelper.getCurrentUserId();
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet", userId.toString()));
        return mapWallet(wallet);
    }

    @Transactional
    public TransactionResponse recharge(WalletRechargeRequest request) {
        UUID userId = securityContextHelper.getCurrentUserId();
        Wallet wallet = walletRepository.findByUserIdWithLock(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet", userId.toString()));

        if (!wallet.isActive()) {
            throw new BusinessException("Wallet inactif", "WALLET_INACTIVE");
        }

        BigDecimal balanceBefore = wallet.getBalance();
        wallet.credit(request.getAmount());
        walletRepository.save(wallet);

        Transaction tx = Transaction.builder()
                .reference(referenceGenerator.generateTransactionReference())
                .wallet(wallet)
                .type(TransactionType.RECHARGE)
                .status(TransactionStatus.SUCCESS)
                .amount(request.getAmount())
                .balanceBefore(balanceBefore)
                .balanceAfter(wallet.getBalance())
                .description("Recharge Mobile Money via " + request.getProvider())
                .externalReference(request.getExternalReference())
                .paymentProvider(request.getProvider())
                .completedAt(LocalDateTime.now())
                .build();

        tx = transactionRepository.save(tx);
        log.info("Recharge wallet {} : +{} XAF (ref: {})", userId, request.getAmount(), tx.getReference());

        notificationService.notifyWalletRecharged(userId, request.getAmount(), wallet.getBalance());

        return mapTransaction(tx);
    }

    @Transactional
    public Transaction debitForRide(UUID userId, BigDecimal amount, UUID rideId) {
        Wallet wallet = walletRepository.findByUserIdWithLock(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet", userId.toString()));

        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException();
        }

        BigDecimal balanceBefore = wallet.getBalance();
        wallet.debit(amount);
        walletRepository.save(wallet);

        Transaction tx = Transaction.builder()
                .reference(referenceGenerator.generateTransactionReference())
                .wallet(wallet)
                .type(TransactionType.RIDE_PAYMENT)
                .status(TransactionStatus.SUCCESS)
                .amount(amount)
                .balanceBefore(balanceBefore)
                .balanceAfter(wallet.getBalance())
                .description("Paiement course")
                .completedAt(LocalDateTime.now())
                .build();

        return transactionRepository.save(tx);
    }

    @Transactional
    public Transaction creditDriver(UUID driverUserId, BigDecimal amount, String description) {
        Wallet wallet = walletRepository.findByUserIdWithLock(driverUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet chauffeur", driverUserId.toString()));

        BigDecimal balanceBefore = wallet.getBalance();
        wallet.credit(amount);
        walletRepository.save(wallet);

        Transaction tx = Transaction.builder()
                .reference(referenceGenerator.generateTransactionReference())
                .wallet(wallet)
                .type(TransactionType.RIDE_PAYMENT)
                .status(TransactionStatus.SUCCESS)
                .amount(amount)
                .balanceBefore(balanceBefore)
                .balanceAfter(wallet.getBalance())
                .description(description)
                .completedAt(LocalDateTime.now())
                .build();

        return transactionRepository.save(tx);
    }

    @Transactional(readOnly = true)
    public Page<TransactionResponse> getMyTransactions(Pageable pageable) {
        UUID userId = securityContextHelper.getCurrentUserId();
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet", userId.toString()));
        return transactionRepository.findByWalletId(wallet.getId(), pageable).map(this::mapTransaction);
    }

    @Transactional(readOnly = true)
    public WalletResponse getWalletByUserId(UUID userId) {
        return walletRepository.findByUserId(userId)
                .map(this::mapWallet)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet", userId.toString()));
    }

    private WalletResponse mapWallet(Wallet w) {
        return WalletResponse.builder()
                .id(w.getId())
                .userId(w.getUser().getId())
                .balance(w.getBalance())
                .currency(w.getCurrency())
                .active(w.isActive())
                .build();
    }

    private TransactionResponse mapTransaction(Transaction t) {
        return TransactionResponse.builder()
                .id(t.getId())
                .reference(t.getReference())
                .type(t.getType())
                .status(t.getStatus())
                .amount(t.getAmount())
                .balanceBefore(t.getBalanceBefore())
                .balanceAfter(t.getBalanceAfter())
                .description(t.getDescription())
                .createdAt(t.getCreatedAt())
                .build();
    }
}
