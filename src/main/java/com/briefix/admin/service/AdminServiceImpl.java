package com.briefix.admin.service;

import com.briefix.admin.dto.AdminUpdateUserRequest;
import com.briefix.admin.dto.AdminUserDto;
import com.briefix.user.exception.UserNotFoundException;
import com.briefix.user.repository.UserJpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AdminServiceImpl implements AdminService {

    private final UserJpaRepository userJpaRepository;

    public AdminServiceImpl(UserJpaRepository userJpaRepository) {
        this.userJpaRepository = userJpaRepository;
    }

    @Override
    public Page<AdminUserDto> getUsers(String emailFilter, int page, int size) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        var entities = (emailFilter != null && !emailFilter.isBlank())
                ? userJpaRepository.findByEmailContainingIgnoreCase(emailFilter.trim(), pageable)
                : userJpaRepository.findAll(pageable);

        return entities.map(e -> new AdminUserDto(
                e.getId(),
                e.getEmail(),
                e.getFullName(),
                e.getProvider(),
                e.getPlan(),
                e.getRole(),
                e.isEmailVerified(),
                e.getCreatedAt()
        ));
    }

    @Override
    public AdminUserDto updateUser(UUID id, AdminUpdateUserRequest request) {
        var entity = userJpaRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
        entity.setRole(request.role());
        entity.setPlan(request.plan());
        var saved = userJpaRepository.save(entity);
        return new AdminUserDto(
                saved.getId(),
                saved.getEmail(),
                saved.getFullName(),
                saved.getProvider(),
                saved.getPlan(),
                saved.getRole(),
                saved.isEmailVerified(),
                saved.getCreatedAt()
        );
    }
}
