package com.briefix.auth.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * Application service responsible for composing and dispatching transactional
 * emails on behalf of the Briefix backend.
 *
 * <p>Currently this service exposes a single operation:
 * {@link #sendVerificationEmail(String, String, String)}, which sends an HTML
 * email containing a one-time verification link to newly registered users. The
 * service is designed as a thin wrapper around Spring's {@link JavaMailSender}
 * and constructs HTML email bodies inline using Java text blocks.</p>
 *
 * <p>The sender address and frontend base URL are resolved from application
 * properties at construction time ({@code app.mail.from} and
 * {@code app.frontend-url} respectively) and are immutable for the lifetime of
 * the bean.</p>
 *
 * <p>Thread-safety: This class is a stateless Spring singleton with only
 * immutable fields after construction. It is safe for concurrent use across
 * multiple request threads.</p>
 */
@Service
public class EmailService {

    /**
     * The Spring Mail sender used to create and dispatch MIME messages via the
     * configured SMTP server.
     */
    private final JavaMailSender mailSender;

    /**
     * The "From" address used in outgoing emails (e.g., {@code no-reply@brief-fix.de}).
     * Injected from the {@code app.mail.from} application property.
     */
    private final String fromAddress;

    /**
     * The base URL of the Briefix frontend application (e.g., {@code https://brief-fix.de}).
     * Used to construct the email verification deep-link. Injected from the
     * {@code app.frontend-url} application property.
     */
    private final String frontendUrl;

    /**
     * Constructs an {@code EmailService} with the required mail sender and configuration values.
     *
     * @param mailSender   the Spring Mail sender; must not be {@code null}
     * @param fromAddress  the sender email address; injected from {@code app.mail.from}
     * @param frontendUrl  the frontend base URL; injected from {@code app.frontend-url}
     */
    public EmailService(JavaMailSender mailSender,
                        @Value("${app.mail.from}") String fromAddress,
                        @Value("${app.frontend-url}") String frontendUrl) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
        this.frontendUrl = frontendUrl;
    }

    /**
     * Composes and sends an HTML email verification message to the specified recipient.
     *
     * <p>The email contains a styled HTML button that links to
     * {@code {frontendUrl}/verify-email?token={token}}, which the user must click to
     * confirm ownership of their email address. The link expires 24 hours after the
     * account is created or after the last resend request.</p>
     *
     * <p>The message is sent as a multipart MIME message with {@code Content-Type: text/html}
     * and UTF-8 encoding. If sending fails for any reason (e.g., SMTP error, invalid
     * address), the underlying exception is wrapped in a {@link RuntimeException} and
     * re-thrown so that callers can decide whether to propagate or swallow the failure.</p>
     *
     * @param toEmail  the recipient's email address; must be a valid RFC-5321 address
     * @param fullName the recipient's display name, used in the email greeting
     * @param token    the UUID verification token to embed in the verification link;
     *                 must not be {@code null} or blank
     * @throws RuntimeException wrapping the underlying mail exception if the SMTP
     *                          dispatch fails
     */
    public void sendVerificationEmail(String toEmail, String fullName, String token) {
        String verifyUrl = frontendUrl + "/verify-email?token=" + token;
        String html = """
                <div style="font-family:Arial,sans-serif;max-width:600px;margin:0 auto">
                  <h2>Welcome to Briefix, %s!</h2>
                  <p>Please verify your email address by clicking the button below.
                     The link expires in 24 hours.</p>
                  <a href="%s"
                     style="display:inline-block;padding:12px 24px;background:#1a56db;
                            color:#fff;text-decoration:none;border-radius:6px;font-weight:bold">
                    Verify Email
                  </a>
                  <p style="margin-top:24px;color:#6b7280;font-size:13px">
                    If you did not create a Briefix account, you can ignore this email.
                  </p>
                </div>
                """.formatted(fullName, verifyUrl);

        try {
            var message = mailSender.createMimeMessage();
            var helper = new MimeMessageHelper(message, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(toEmail);
            helper.setSubject("Verify your Briefix email address");
            helper.setText(html, true);
            mailSender.send(message);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send verification email to " + toEmail, e);
        }
    }
}
