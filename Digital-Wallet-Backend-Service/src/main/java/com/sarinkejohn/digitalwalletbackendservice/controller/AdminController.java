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
@RequestMapping("/api/admin")
public class AdminController {

    private final WalletService walletService;

    public AdminController(WalletService walletService) {
        this.walletService = walletService;
    }

    @PostMapping("/topup")
    public ResponseEntity<TransactionDto> adminTopUp(
            @Valid @RequestBody AdminTopUpRequest request,
            @AuthenticationPrincipal UserPrincipal admin) {
        return ResponseEntity.ok(walletService.adminTopUp(
                request.getUserId(), 
                request.getAmount(),
                admin.getUserId()));
    }

    @GetMapping("/topup/requests")
    public ResponseEntity<List<TopUpResponseDto>> getPendingTopUpRequests() {
        return ResponseEntity.ok(walletService.getPendingTopUpRequests());
    }

    @PostMapping("/topup/requests/{requestId}/approve")
    public ResponseEntity<TopUpResponseDto> approveTopUpRequest(
            @PathVariable Long requestId,
            @AuthenticationPrincipal UserPrincipal admin) {
        return ResponseEntity.ok(walletService.approveTopUpRequest(requestId, admin.getUserId()));
    }

    @PostMapping("/topup/requests/{requestId}/reject")
    public ResponseEntity<TopUpResponseDto> rejectTopUpRequest(@PathVariable Long requestId) {
        return ResponseEntity.ok(walletService.rejectTopUpRequest(requestId));
    }

    @GetMapping("/wallet/{userId}")
    public ResponseEntity<WalletDto> getUserWallet(@PathVariable Long userId) {
        return ResponseEntity.ok(walletService.getWalletByUserId(userId));
    }
}