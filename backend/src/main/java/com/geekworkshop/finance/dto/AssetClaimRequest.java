package com.geekworkshop.finance.dto;

import jakarta.validation.constraints.*;

public class AssetClaimRequest {
    @NotBlank @Size(max = 200)
    private String useLocation;
    @Size(max = 500)
    private String remark;

    public String getUseLocation() { return useLocation; }
    public void setUseLocation(String useLocation) { this.useLocation = useLocation; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
