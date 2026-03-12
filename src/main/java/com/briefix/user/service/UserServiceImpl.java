package com.briefix.user.service;

import com.briefix.user.dto.UpdateBillingRequest;
import com.briefix.user.dto.UserDto;
import com.briefix.user.exception.InvalidCurrentPasswordException;
import com.briefix.user.exception.UserNotFoundException;
import com.briefix.user.mapper.UserMapper;
import com.briefix.user.model.AuthProvider;
import com.briefix.user.model.User;
import com.briefix.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Default implementation of the {@link UserService} application service interface.
 *
 * <p>This class orchestrates user query operations by delegating persistence lookups
 * to {@link UserRepository} and converting the resulting domain records to safe
 * outbound representations via {@link UserMapper}. It acts as the sole business
 * logic coordinator for the user domain's read operations.</p>
 *
 * <p>Write operations (registration, password updates, OAuth2 account linking) are
 * handled by dedicated authentication components outside this service boundary.</p>
 *
 * <p>This class is registered as a Spring {@code @Service} singleton and is
 * injected wherever {@link UserService} is required, enabling mocking in tests.</p>
 *
 * <p>Thread-safety: This class is stateless and relies exclusively on thread-safe,
 * singleton-scoped dependencies. It is safe for concurrent use.</p>
 */
@Service
public class UserServiceImpl implements UserService {

    /**
     * The domain-facing repository port used to load {@link com.briefix.user.model.User}
     * aggregates from the underlying data store.
     */
    private final UserRepository userRepository;

    private final UserMapper userMapper;

    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository, UserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Performs a primary-key lookup via {@link UserRepository#findById(UUID)} and
     * maps the result to a {@link UserDto}. Throws {@link UserNotFoundException} if
     * the lookup returns empty, allowing the global exception handler to respond
     * with an HTTP 404.</p>
     *
     * @param id the UUID of the user to retrieve; must not be {@code null}
     * @return a {@link UserDto} with sensitive fields excluded
     * @throws UserNotFoundException if no user record exists with the given UUID
     */
    @Override
    public UserDto findById(UUID id) {
        return userRepository.findById(id)
                .map(userMapper::toDto)
                .orElseThrow(() -> new UserNotFoundException(id));
    }

    /**
     * {@inheritDoc}
     *
     * <p>Performs an email-based lookup via {@link UserRepository#findByEmail(String)} and
     * maps the result to a {@link UserDto}. Throws {@link UserNotFoundException} if
     * the lookup returns empty, allowing the global exception handler to respond
     * with an HTTP 404.</p>
     *
     * @param email the email address to search for; must not be {@code null}
     * @return a {@link UserDto} with sensitive fields excluded
     * @throws UserNotFoundException if no user record exists with the given email address
     */
    @Override
    public UserDto findByEmail(String email) {
        return userRepository.findByEmail(email)
                .map(userMapper::toDto)
                .orElseThrow(() -> new UserNotFoundException(email));
    }

    @Override
    public UserDto updateProfile(String email, String fullName, String phone) {
        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email));
        var updated = copyWith(user, fullName, phone, user.passwordHash(),
                user.billingName(), user.billingStreet(), user.billingStreetNo(),
                user.billingZip(), user.billingCity(), user.billingCountry());
        return userMapper.toDto(userRepository.save(updated));
    }

    @Override
    public void updatePassword(String email, String currentPassword, String newPassword) {
        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email));
        if (user.provider() != AuthProvider.LOCAL) {
            throw new IllegalStateException("Password change is not supported for OAuth2 accounts");
        }
        if (!passwordEncoder.matches(currentPassword, user.passwordHash())) {
            throw new InvalidCurrentPasswordException();
        }
        var updated = copyWith(user, user.fullName(), user.phone(), passwordEncoder.encode(newPassword),
                user.billingName(), user.billingStreet(), user.billingStreetNo(),
                user.billingZip(), user.billingCity(), user.billingCountry());
        userRepository.save(updated);
    }

    @Override
    public UserDto updateBilling(String email, UpdateBillingRequest request) {
        var user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email));
        var updated = copyWith(user, user.fullName(), user.phone(), user.passwordHash(),
                request.billingName(), request.billingStreet(), request.billingStreetNo(),
                request.billingZip(), request.billingCity(), request.billingCountry());
        return userMapper.toDto(userRepository.save(updated));
    }

    private User copyWith(User u, String fullName, String phone, String passwordHash,
                          String billingName, String billingStreet, String billingStreetNo,
                          String billingZip, String billingCity, String billingCountry) {
        return new User(u.id(), u.email(), passwordHash, u.provider(), u.providerId(),
                u.isEmailVerified(), fullName, phone, u.plan(), u.role(), u.createdAt(),
                u.verificationToken(), u.verificationTokenExpiry(),
                u.passwordResetToken(), u.passwordResetTokenExpiry(),
                billingName, billingStreet, billingStreetNo, billingZip, billingCity, billingCountry);
    }
}
