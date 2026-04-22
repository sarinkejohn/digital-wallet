package com.sarinkejohn.digitalwalletbackendservice.service;

import com.sarinkejohn.digitalwalletbackendservice.dto.*;
import com.sarinkejohn.digitalwalletbackendservice.entity.Transactions;
import com.sarinkejohn.digitalwalletbackendservice.entity.TopUpRequest;
import com.sarinkejohn.digitalwalletbackendservice.entity.User;
import com.sarinkejohn.digitalwalletbackendservice.entity.Wallet;
import com.sarinkejohn.digitalwalletbackendservice.enums.RequestStatus;
import com.sarinkejohn.digitalwalletbackendservice.enums.TransactionType;
import com.sarinkejohn.digitalwalletbackendservice.exception.*;
import com.sarinkejohn.digitalwalletbackendservice.repository.TransactionsRepository;
import com.sarinkejohn.digitalwalletbackendservice.repository.TopUpRequestRepository;
import com.sarinkejohn.digitalwalletbackendservice.repository.UserRepository;
import com.sarinkejohn.digitalwalletbackendservice.repository.WalletRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class WalletService {

    private static final Logger logger = LoggerFactory.getLogger(WalletService.class);

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final TransactionsRepository transactionsRepository;
    private final TopUpRequestRepository topUpRequestRepository;

    @Value("${wallet.transfer.min-amount:1.0}")
    private double minTransferAmount;

    @Value("${wallet.transfer.max-amount:100000.0}")
    private double maxTransferAmount;

    @Value("${wallet.topup.max-amount:1000000.0}")
    private double maxTopUpAmount;

    public WalletService(UserRepository userRepository, WalletRepository walletRepository,
                        TransactionsRepository transactionsRepository, TopUpRequestRepository topUpRequestRepository) {
        this.userRepository = userRepository;
        this.walletRepository = walletRepository;
        this.transactionsRepository = transactionsRepository;
        this.topUpRequestRepository = topUpRequestRepository;
    }

    @Transactional
    public Wallet createUserAndWallet(String username, String password, String role) {
        if (userRepository.existsByUsername(username)) {
            throw new InvalidAmountException("Username already exists");
        }
        User user = new User();
        user.setUsername(username);
        user.setPassword(password);
        user.setRole(role);
        user = userRepository.save(user);

        Wallet wallet = new Wallet();
        wallet.setUser(user);
        wallet.setBalance(0.0);
        walletRepository.save(wallet);

        logger.info("Created user {} with wallet", username);
        return wallet;
    }

    public WalletDto getWalletByUserId(Long userId) {
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new UserNotFoundException("Wallet not found for user: " + userId));
        return new WalletDto(wallet.getId(), wallet.getUser().getId(),
                wallet.getUser().getUsername(), wallet.getBalance());
    }

    @Transactional
    public TransactionDto transfer(Long senderId, Long receiverId, Double amount, String reference) {
        validateTransferAmount(amount);

        if (transactionsRepository.findByReference(reference).isPresent() && reference != null) {
            throw new DuplicateReferenceException("Transaction with this reference already exists");
        }

        Wallet senderWallet = walletRepository.findByUserId(senderId)
                .orElseThrow(() -> new UserNotFoundException("Sender wallet not found"));

        if (senderWallet.getBalance() < amount) {
            throw new InsufficientFundsException("Insufficient funds for transfer");
        }

        Wallet receiverWallet = walletRepository.findByUserId(receiverId)
                .orElseThrow(() -> new UserNotFoundException("Receiver wallet not found"));

        String ref = reference != null ? reference : UUID.randomUUID().toString();

        senderWallet.setBalance(senderWallet.getBalance() - amount);
        receiverWallet.setBalance(receiverWallet.getBalance() + amount);
        walletRepository.save(senderWallet);
        walletRepository.save(receiverWallet);

        Transactions transaction = new Transactions();
        transaction.setSenderId(senderId);
        transaction.setReceiverId(receiverId);
        transaction.setAmount(amount);
        transaction.setType(TransactionType.TRANSFER);
        transaction.setReference(ref);
        transactionsRepository.save(transaction);

        logger.info("Transfer completed: {} from user {} to user {}", amount, senderId, receiverId);
        return new TransactionDto(transaction.getId(), transaction.getSenderId(),
                transaction.getReceiverId(), transaction.getAmount(), transaction.getType(),
                transaction.getReference(), transaction.getCreatedAt());
    }

    @Transactional
    public TransactionDto adminTopUp(Long userId, Double amount, Long adminUserId) {
        validateTopUpAmount(amount);

        String correlationId = "TX" + UUID.randomUUID().toString().substring(0, 13).toUpperCase();
        
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new UserNotFoundException("Wallet not found for user: " + userId));

        wallet.setBalance(wallet.getBalance() + amount);
        walletRepository.save(wallet);

        Transactions transaction = new Transactions();
        transaction.setSenderId(adminUserId);
        transaction.setReceiverId(userId);
        transaction.setAmount(amount);
        transaction.setType(TransactionType.TOP_UP);
        transaction.setReference(correlationId);
        transactionsRepository.save(transaction);

        logger.info("Admin top-up completed: {} by admin {} to user {}", amount, adminUserId, userId);
        return new TransactionDto(transaction.getId(), transaction.getSenderId(),
                transaction.getReceiverId(), transaction.getAmount(), transaction.getType(),
                transaction.getReference(), transaction.getCreatedAt());
    }

    @Transactional
    public TopUpResponseDto requestTopUp(Long userId, Double amount) {
        validateTopUpAmount(amount);

        TopUpRequest request = new TopUpRequest();
        request.setUserId(userId);
        request.setAmount(amount);
        request.setStatus(RequestStatus.PENDING);
        request = topUpRequestRepository.save(request);

        logger.info("Top-up request created: {} for user {}", amount, userId);
        return new TopUpResponseDto(request.getId(), request.getUserId(),
                request.getAmount(), request.getStatus(), request.getCreatedAt());
    }

    @Transactional
    public TopUpResponseDto approveTopUpRequest(Long requestId, Long adminUserId) {
        TopUpRequest request = topUpRequestRepository.findById(requestId)
                .orElseThrow(() -> new UserNotFoundException("Top-up request not found"));

        if (request.getStatus() != RequestStatus.PENDING) {
            throw new InvalidAmountException("Request is not in pending status");
        }

        adminTopUp(request.getUserId(), request.getAmount(), adminUserId);
        request.setStatus(RequestStatus.APPROVED);
        request = topUpRequestRepository.save(request);

        logger.info("Top-up request {} approved by admin {}", requestId, adminUserId);
        return new TopUpResponseDto(request.getId(), request.getUserId(),
                request.getAmount(), request.getStatus(), request.getCreatedAt());
    }

    @Transactional
    public TopUpResponseDto rejectTopUpRequest(Long requestId) {
        TopUpRequest request = topUpRequestRepository.findById(requestId)
                .orElseThrow(() -> new UserNotFoundException("Top-up request not found"));

        if (request.getStatus() != RequestStatus.PENDING) {
            throw new InvalidAmountException("Request is not in pending status");
        }

        request.setStatus(RequestStatus.REJECTED);
        request = topUpRequestRepository.save(request);

        logger.info("Top-up request rejected: {}", requestId);
        return new TopUpResponseDto(request.getId(), request.getUserId(),
                request.getAmount(), request.getStatus(), request.getCreatedAt());
    }

    public List<TopUpResponseDto> getPendingTopUpRequests() {
        return topUpRequestRepository.findByStatusOrderByCreatedAtDesc(RequestStatus.PENDING)
                .stream()
                .map(r -> new TopUpResponseDto(r.getId(), r.getUserId(),
                        r.getAmount(), r.getStatus(), r.getCreatedAt()))
                .collect(Collectors.toList());
    }

    public List<TransactionDto> getUserTransactions(Long userId) {
        return transactionsRepository.findBySenderIdOrReceiverIdOrderByCreatedAtDesc(userId, userId)
                .stream()
                .map(t -> new TransactionDto(t.getId(), t.getSenderId(),
                        t.getReceiverId(), t.getAmount(), t.getType(),
                        t.getReference(), t.getCreatedAt()))
                .collect(Collectors.toList());
    }

    private void validateTransferAmount(Double amount) {
        if (amount < minTransferAmount) {
            throw new InvalidAmountException("Transfer amount must be at least " + minTransferAmount);
        }
        if (amount > maxTransferAmount) {
            throw new InvalidAmountException("Transfer amount must not exceed " + maxTransferAmount);
        }
    }

    private void validateTopUpAmount(Double amount) {
        if (amount <= 0) {
            throw new InvalidAmountException("Top-up amount must be positive");
        }
        if (amount > maxTopUpAmount) {
            throw new InvalidAmountException("Top-up amount must not exceed " + maxTopUpAmount);
        }
    }
}