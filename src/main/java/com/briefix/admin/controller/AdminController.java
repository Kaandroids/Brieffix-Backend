package com.briefix.admin.controller;

import com.briefix.admin.dto.AdminUpdateUserRequest;
import com.briefix.admin.dto.AdminUserDto;
import com.briefix.admin.service.AdminService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/users")
    public ResponseEntity<Page<AdminUserDto>> getUsers(
            @RequestParam(required = false) String email,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(adminService.getUsers(email, page, Math.min(size, 100)));
    }

    @PatchMapping("/users/{id}")
    public ResponseEntity<AdminUserDto> updateUser(
            @PathVariable UUID id,
            @Valid @RequestBody AdminUpdateUserRequest request
    ) {
        return ResponseEntity.ok(adminService.updateUser(id, request));
    }
}
