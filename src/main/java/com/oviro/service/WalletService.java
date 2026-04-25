package com.oviro.service;

import com.oviro.dto.request.SetPinRequest;
import com.oviro.dto.request.WalletRechargeRequest;
import com.oviro.dto.request.WalletTransferRequest;
import com.oviro.dto.response.TransactionResponse;
import com.oviro.dto.response.WalletResponse;
import com.oviro.enums.TransactionStatus;
import com.oviro.enums.TransactionType;
import com.oviro.exception.BusinessException;
import com.oviro.exception.InsufficientBalanceException;
import com.oviro.exception.ResourceNotFoundException;
import com.oviro.model.Transaction;
import com.oviro.model.User;
import com.oviro.model.Wallet;
import com.oviro.repository.TransactionRepository;
import com.oviro.repository.UserRepository;
import com.oviro.repository.WalletRepository;
import com.oviro.util.ReferenceGenerator;
import com.oviro.util.SecurityContextHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
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
    private final UserRepository userRepository;
    private final ReferenceGenerator referenceGenerator;
    private final SecurityContextHelper securityContextHelper;
    private final NotificationService notificationService;
    private final PasswordEncoder passwordEncoder;

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
        log.info("Recharge wallet {} : +{} UT (ref: {})", userId, request.getAmount(), tx.getReference());

        notificationService.notifyWalletRecharged(userId, request.getAmount(), wallet.getBalance());

        return mapTransaction(tx);
    }

    @Transactional
    public void transfer(WalletTransferRequest request) {
        UUID senderId = securityContextHelper.getCurrentUserId();
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", senderId.toString()));

        // Vérifier PIN
        if (sender.getTransferPinHash() == null) {
            throw new BusinessException("Veuillez d'abord définir votre PIN de transfert", "PIN_NOT_SET");
        }
        if (!passwordEncoder.matches(request.getPin(), sender.getTransferPinHash())) {
            throw new BusinessException("PIN incorrect", "INVALID_PIN");
        }

        // Trouver destinataire
        User recipient = userRepository.findByPhoneNumber(request.getRecipientPhone())
                .orElseThrow(() -> new BusinessException("Aucun compte trouvé pour ce numéro", "RECIPIENT_NOT_FOUND"));

        if (recipient.getId().equals(senderId)) {
            throw new BusinessException("Vous ne pouvez pas vous transférer des UT à vous-même", "SELF_TRANSFER");
        }

        String transferId = UUID.randomUUID().toString();

        // Débiter l'expéditeur
        Wallet senderWallet = walletRepository.findByUserIdWithLock(senderId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet", senderId.toString()));
        if (senderWallet.getBalance().compareTo(request.getAmount()) < 0) {
            throw new InsufficientBalanceException();
        }
        BigDecimal senderBefore = senderWallet.getBalance();
        senderWallet.debit(request.getAmount());
        walletRepository.save(senderWallet);

        Transaction debitTx = Transaction.builder()
                .reference(referenceGenerator.generateTransactionReference())
                .wallet(senderWallet)
                .type(TransactionType.TRANSFER_DEBIT)
                .status(TransactionStatus.SUCCESS)
                .amount(request.getAmount())
                .balanceBefore(senderBefore)
                .balanceAfter(senderWallet.getBalance())
                .description("Transfert vers " + recipient.getPhoneNumber()
                        + (request.getNote() != null ? " – " + request.getNote() : ""))
                .metadata(transferId)
                .completedAt(LocalDateTime.now())
                .build();
        transactionRepository.save(debitTx);

        // Créditer le destinataire
        Wallet recipientWallet = walletRepository.findByUserIdWithLock(recipient.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Wallet destinataire", recipient.getId().toString()));
        BigDecimal recipientBefore = recipientWallet.getBalance();
        recipientWallet.credit(request.getAmount());
        walletRepository.save(recipientWallet);

        Transaction creditTx = Transaction.builder()
                .reference(referenceGenerator.generateTransactionReference())
                .wallet(recipientWallet)
                .type(TransactionType.TRANSFER_CREDIT)
                .status(TransactionStatus.SUCCESS)
                .amount(request.getAmount())
                .balanceBefore(recipientBefore)
                .balanceAfter(recipientWallet.getBalance())
                .description("Transfert reçu de " + sender.getPhoneNumber())
                .metadata(transferId)
                .completedAt(LocalDateTime.now())
                .build();
        transactionRepository.save(creditTx);

        log.info("Transfert {} UT de {} vers {} (ref={})", request.getAmount(), senderId, recipient.getId(), transferId);
        notificationService.notifyWalletTransfer(senderId, recipient.getId(), request.getAmount(), sender.getFullName());
    }

    @Transactional
    public void setTransferPin(SetPinRequest request) {
        UUID userId = securityContextHelper.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", userId.toString()));
        user.setTransferPinHash(passwordEncoder.encode(request.getPin()));
        userRepository.save(user);
        log.debug("PIN de transfert défini pour userId={}", userId);
    }

    @Transactional(readOnly = true)
    public boolean verifyPin(SetPinRequest request) {
        UUID userId = securityContextHelper.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", userId.toString()));
        if (user.getTransferPinHash() == null) return false;
        return passwordEncoder.matches(request.getPin(), user.getTransferPinHash());
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
