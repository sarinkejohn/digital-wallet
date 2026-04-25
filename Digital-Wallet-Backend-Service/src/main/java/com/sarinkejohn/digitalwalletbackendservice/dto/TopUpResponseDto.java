package com.sarinkejohn.digitalwalletbackendservice.dto;

import com.sarinkejohn.digitalwalletbackendservice.enums.RequestStatus;

import java.time.LocalDateTime;

public record TopUpResponseDto(Long id, Long userId, Double amount, RequestStatus status,
                              LocalDateTime createdAt) {
}