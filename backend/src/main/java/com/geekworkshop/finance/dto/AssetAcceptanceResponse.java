package com.geekworkshop.finance.dto;

import java.util.List;

public record AssetAcceptanceResponse(
        String acceptanceNumber, String purchaseApplicationNumber, List<AssetResponse> assets
) {}
