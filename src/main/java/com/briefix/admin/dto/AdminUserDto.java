package com.briefix.admin.dto;

import com.briefix.user.model.AuthProvider;
import com.briefix.user.model.UserPlan;
import com.briefix.user.model.UserRole;

import java.time.LocalDateTime;
import java.util.UUID;

public record AdminUserDto(
        UUID id,
        String email,
        String fullName,
        AuthProvider provider,
        UserPlan plan,
        UserRole role,
        boolean isEmailVerified,
        LocalDateTime createdAt
) {}
