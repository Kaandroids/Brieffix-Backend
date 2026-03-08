package com.briefix.auth.dto;

/**
 * Inbound DTO carrying the Google ID token submitted by the client during
 * Google OAuth2 sign-in.
 *
 * <p>This record is deserialised from the JSON body of a
 * {@code POST /api/v1/auth/google} request. The {@code idToken} field contains
 * the raw Google ID token string obtained by the frontend via the Google Sign-In
 * SDK. The token is verified server-side by
 * {@link com.briefix.auth.service.AuthServiceImpl#verifyGoogleToken(String)}
 * using the Google API Client library.</p>
 *
 * <p>This record is immutable and safe for concurrent use across threads.</p>
 *
 * @param idToken the raw Google ID token string issued to the client by Google's
 *                authentication endpoint; must not be {@code null} or blank
 */
public record GoogleAuthRequest(String idToken) {}
