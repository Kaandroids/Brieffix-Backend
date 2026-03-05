package com.briefix.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Immutable request payload for the token refresh endpoint
 * ({@code POST /api/v1/auth/refresh}).
 *
 * <p>Clients submit this payload when their access token has expired and they wish to
 * obtain a new one without prompting the user to log in again. The refresh token is
 * validated by {@link com.briefix.auth.service.AuthServiceImpl#refresh(String)} to
 * confirm it is structurally sound, correctly signed, and not yet expired.
 * If valid, a new access token is issued while the original refresh token is returned
 * unchanged.</p>
 *
 * <p>The field is validated at the controller boundary via Bean Validation ({@code @Valid}).
 * A blank value produces an HTTP 400 response. An invalid or expired token produces
 * an HTTP 401 response via {@link com.briefix.common.GlobalExceptionHandler}.</p>
 *
 * <p><b>Thread safety:</b> Java records are immutable by design; instances of this
 * class are safe to share across threads.</p>
 *
 * @param refreshToken The refresh token previously issued at login or registration.
 *                     Must not be blank. Validated for signature integrity and expiry
 *                     by the service layer before a new access token is issued.
 */
public record RefreshRequest(

        @NotBlank(message = "Refresh token is required")
        String refreshToken
) {}
