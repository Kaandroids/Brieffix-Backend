package com.briefix.admin.service;

import com.briefix.admin.dto.AdminUpdateUserRequest;
import com.briefix.admin.dto.AdminUserDto;
import org.springframework.data.domain.Page;

import java.util.UUID;

public interface AdminService {
    Page<AdminUserDto> getUsers(String emailFilter, int page, int size);
    AdminUserDto updateUser(UUID id, AdminUpdateUserRequest request);
}
