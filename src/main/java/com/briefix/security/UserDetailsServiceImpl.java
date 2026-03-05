package com.briefix.security;

import com.briefix.user.repository.UserJpaRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Spring Security {@link UserDetailsService} implementation that loads
 * user credentials from the PostgreSQL database for authentication.
 *
 * <p>Within the Briefix security model, the "username" concept is mapped to the
 * user's email address. This service is consumed by Spring Security's
 * {@link org.springframework.security.authentication.dao.DaoAuthenticationProvider}
 * during the login flow, and directly by {@link JwtAuthFilter} when reconstructing
 * the authentication principal from a validated JWT on subsequent requests.</p>
 *
 * <p>A single {@code ROLE_USER} authority is granted to all accounts loaded by this
 * service. Role-based access control beyond this single role is not implemented
 * in the current version.</p>
 *
 * <p><b>Thread safety:</b> This class is a stateless Spring singleton. All database
 * access is delegated to the thread-safe {@link UserJpaRepository}; concurrent
 * invocations of {@link #loadUserByUsername} are safe.</p>
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    /**
     * JPA repository used to query the {@code users} table by email address.
     * Injected by the Spring container; must not be {@code null}.
     */
    private final UserJpaRepository userJpaRepository;

    /**
     * Constructs a new {@code UserDetailsServiceImpl} with the provided user repository.
     *
     * @param userJpaRepository the JPA repository for user entity lookups;
     *                          must not be {@code null}
     */
    public UserDetailsServiceImpl(UserJpaRepository userJpaRepository) {
        this.userJpaRepository = userJpaRepository;
    }

    /**
     * Loads a {@link UserDetails} instance by the user's email address.
     *
     * <p>The returned {@link User} object is constructed with:
     * <ul>
     *   <li>The persisted email as the username.</li>
     *   <li>The persisted BCrypt password hash, or an empty string for accounts
     *       authenticated via an external OAuth2 provider (which have no local password).</li>
     *   <li>A single {@code ROLE_USER} granted authority.</li>
     * </ul>
     * </p>
     *
     * @param email the email address of the account to load; used as the Spring Security
     *              "username" throughout the authentication pipeline
     * @return a populated, non-{@code null} {@link UserDetails} object for the matching account
     * @throws UsernameNotFoundException if no active account is associated with the given email address
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userJpaRepository.findByEmail(email)
                .map(entity -> new User(
                        entity.getEmail(),
                        entity.getPasswordHash() != null ? entity.getPasswordHash() : "",
                        List.of(new SimpleGrantedAuthority("ROLE_USER"))
                ))
                .orElseThrow(() -> new UsernameNotFoundException("No account found for email: " + email));
    }
}
