package com.briefix.user.service;

import com.briefix.user.dto.UserDto;
import com.briefix.user.exception.UserNotFoundException;
import com.briefix.user.mapper.UserMapper;
import com.briefix.user.repository.UserRepository;
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

    /**
     * Mapper used to convert {@link com.briefix.user.model.User} domain records
     * to {@link UserDto} instances before returning them to callers.
     */
    private final UserMapper userMapper;

    /**
     * Constructs a {@code UserServiceImpl} with the required repository and mapper.
     *
     * <p>Constructor injection is used to enforce that both dependencies are mandatory
     * and to facilitate straightforward unit testing with mock implementations.</p>
     *
     * @param userRepository the repository port for user persistence operations; must not be {@code null}
     * @param userMapper     the mapper for converting domain models to DTOs; must not be {@code null}
     */
    public UserServiceImpl(UserRepository userRepository, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
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
}
