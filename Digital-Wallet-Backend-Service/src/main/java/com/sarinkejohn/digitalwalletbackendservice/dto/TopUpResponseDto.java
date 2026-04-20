package com.sarinkejohn.digitalwalletbackendservice.dto;

import com.sarinkejohn.digitalwalletbackendservice.enums.RequestStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class TopUpResponseDto {
    private Long id;
    private Long userId;
    private Double amount;
    private RequestStatus status;
    private LocalDateTime createdAt;
}