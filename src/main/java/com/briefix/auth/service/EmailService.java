package com.briefix.auth.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final String fromAddress;
    private final String frontendUrl;

    public EmailService(JavaMailSender mailSender,
                        @Value("${app.mail.from}") String fromAddress,
                        @Value("${app.frontend-url}") String frontendUrl) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
        this.frontendUrl = frontendUrl;
    }

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
