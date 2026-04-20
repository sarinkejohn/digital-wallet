package com.sarinkejohn.digitalwalletbackendservice.dto;

import com.sarinkejohn.digitalwalletbackendservice.enums.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class TransactionDto {
    private Long id;
    private Long senderId;
    private Long receiverId;
    private Double amount;
    private TransactionType type;
    private String reference;
    private LocalDateTime createdAt;
}