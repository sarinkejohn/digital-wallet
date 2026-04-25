package com.sarinkejohn.digitalwalletbackendservice.dto;

import com.sarinkejohn.digitalwalletbackendservice.enums.TransactionType;

import java.time.LocalDateTime;

public record TransactionDto(Long id, Long senderId, Long receiverId, Double amount,
                           TransactionType type, String reference, LocalDateTime createdAt) {
}