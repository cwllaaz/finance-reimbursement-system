package com.geekworkshop.finance.controller;

import com.geekworkshop.finance.dto.*;
import com.geekworkshop.finance.entity.AssetStatus;
import com.geekworkshop.finance.service.AssetService;
import com.geekworkshop.finance.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/assets")
public class AssetController {
    private final AssetService assetService;
    private final AuthService authService;

    public AssetController(AssetService assetService, AuthService authService) {
        this.assetService = assetService;
        this.authService = authService;
    }

    @GetMapping
    public List<AssetResponse> list(
            @RequestHeader("X-Auth-Token") String token,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) AssetStatus status
    ) {
        return assetService.list(authService.requireUser(token), keyword, status);
    }

    @GetMapping("/{id}")
    public AssetResponse detail(@RequestHeader("X-Auth-Token") String token, @PathVariable Long id) {
        return assetService.detail(authService.requireUser(token), id);
    }

    @GetMapping("/eligible-purchases")
    public List<PurchaseApplicationResponse> eligiblePurchases(
            @RequestHeader("X-Auth-Token") String token
    ) {
        return assetService.eligiblePurchases(authService.requireUser(token));
    }

    @GetMapping("/claimants")
    public List<AssetUserOptionResponse> claimantOptions(
            @RequestHeader("X-Auth-Token") String token
    ) {
        return assetService.claimantOptions(authService.requireUser(token));
    }

    @PostMapping("/acceptance")
    @ResponseStatus(HttpStatus.CREATED)
    public AssetAcceptanceResponse acceptInbound(
            @RequestHeader("X-Auth-Token") String token,
            @Valid @RequestBody AssetAcceptanceRequest request
    ) {
        return assetService.acceptInbound(authService.requireUser(token), request);
    }

    @PostMapping("/{id}/claim")
    public AssetResponse claim(
            @RequestHeader("X-Auth-Token") String token, @PathVariable Long id,
            @Valid @RequestBody AssetClaimRequest request
    ) {
        return assetService.claim(authService.requireUser(token), id, request);
    }
}
