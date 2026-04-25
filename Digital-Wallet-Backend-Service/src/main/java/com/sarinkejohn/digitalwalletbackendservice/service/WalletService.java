package com.sarinkejohn.digitalwalletbackendservice.service;

import com.sarinkejohn.digitalwalletbackendservice.dto.*;
import com.sarinkejohn.digitalwalletbackendservice.entity.Wallet;
import java.util.List;

public interface WalletService {
    
    Wallet createUserAndWallet(String username, String password, String role);
    
    WalletDto getWalletByUserId(Long userId);
    
    TransactionDto transfer(Long senderId, Long receiverId, Double amount, String reference);
    
    TransactionDto adminTopUp(Long userId, Double amount, Long adminUserId);
    
    TopUpResponseDto requestTopUp(Long userId, Double amount);
    
    TopUpResponseDto approveTopUpRequest(Long requestId, Long adminUserId);
    
    TopUpResponseDto rejectTopUpRequest(Long requestId);
    
    List<TopUpResponseDto> getPendingTopUpRequests();
    
    List<TransactionDto> getUserTransactions(Long userId);
}