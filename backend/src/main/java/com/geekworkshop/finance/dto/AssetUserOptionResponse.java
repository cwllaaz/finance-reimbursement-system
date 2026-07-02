package com.geekworkshop.finance.dto;

import com.geekworkshop.finance.entity.AppUser;

public record AssetUserOptionResponse(
        Long id, String realName, String username, String departmentName
) {
    public static AssetUserOptionResponse fromEntity(AppUser user) {
        return new AssetUserOptionResponse(
                user.getId(),
                user.getRealName(),
                user.getUsername(),
                user.getDepartment() == null ? null : user.getDepartment().getName()
        );
    }
}
