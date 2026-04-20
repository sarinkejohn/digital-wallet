package com.sarinkejohn.digitalwalletbackendservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class WalletDto {
    private Long id;
    private Long userId;
    private String username;
    private Double balance;
}