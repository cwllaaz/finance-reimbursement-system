package com.geekworkshop.finance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AssetClaimApplicationRequest {
    @NotBlank @Size(max = 200)
    private String useLocation;
    @NotBlank @Size(max = 500)
    private String reason;
    public String getUseLocation() { return useLocation; }
    public void setUseLocation(String useLocation) { this.useLocation = useLocation; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
