package com.briefix.admin.dto;

import com.briefix.user.model.UserPlan;
import com.briefix.user.model.UserRole;
import jakarta.validation.constraints.NotNull;

public record AdminUpdateUserRequest(
        @NotNull UserRole role,
        @NotNull UserPlan plan
) {}
