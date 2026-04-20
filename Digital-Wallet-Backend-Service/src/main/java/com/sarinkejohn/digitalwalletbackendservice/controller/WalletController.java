package com.sarinkejohn.digitalwalletbackendservice.controller;

import com.sarinkejohn.digitalwalletbackendservice.dto.*;
import com.sarinkejohn.digitalwalletbackendservice.security.UserPrincipal;
import com.sarinkejohn.digitalwalletbackendservice.service.WalletService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/wallet")
public class WalletController {

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @GetMapping("/balance")
    public ResponseEntity<WalletDto> getBalance(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(walletService.getWalletByUserId(principal.getUserId()));
    }

    @PostMapping("/transfer")
    public ResponseEntity<TransactionDto> transfer(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody TransferRequest request) {
        return ResponseEntity.ok(walletService.transfer(
                principal.getUserId(), 
                request.getReceiverId(), 
                request.getAmount(),
                request.getReference()));
    }

    @PostMapping("/topup/request")
    public ResponseEntity<TopUpResponseDto> requestTopUp(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody TopUpRequestDto request) {
        return ResponseEntity.ok(walletService.requestTopUp(
                principal.getUserId(), 
                request.getAmount()));
    }

    @GetMapping("/transactions")
    public ResponseEntity<List<TransactionDto>> getTransactions(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(walletService.getUserTransactions(principal.getUserId()));
    }
}