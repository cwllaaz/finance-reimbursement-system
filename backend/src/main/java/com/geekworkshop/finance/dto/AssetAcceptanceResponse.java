package com.geekworkshop.finance.dto;

import java.util.List;
import java.time.LocalDateTime;

public record AssetAcceptanceResponse(
        String acceptanceNumber, String purchaseApplicationNumber,
        Long acceptedById, String acceptedByName, LocalDateTime acceptedAt,
        List<AssetResponse> assets
) {}
